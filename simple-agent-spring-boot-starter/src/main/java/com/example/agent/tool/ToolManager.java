package com.example.agent.tool;

import com.example.agent.message.Message;
import com.example.agent.message.MessageRoleEnum;
import com.example.agent.tool.anno.ToolDescription;
import com.example.agent.tool.dto.ToolCall;
import com.example.agent.tool.dto.ToolDefinition;
import com.example.agent.tool.dto.ToolParameter;
import com.example.agent.util.ToolTypeUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ToolManager {

    private static final Logger log = LoggerFactory.getLogger(ToolManager.class);

    private final Map<String, ToolDefinition> tools = new LinkedHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public synchronized void register(Object toolBean) {
        Class<?> toolClass = toolBean.getClass();

        // 一个带描述的公开方法对应一个可供模型调用的工具。
        for (Method method : toolClass.getMethods()) {
            ToolDescription description = method.getAnnotation(ToolDescription.class);
            if (description == null) {
                continue;
            }

            List<ToolParameter> parameters = new ArrayList<>();
            Parameter[] methodParameters = method.getParameters();
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (int index = 0; index < methodParameters.length; index++) {
                Class<?> parameterType = parameterTypes[index];
                parameters.add(new ToolParameter(methodParameters[index].getName(), ToolTypeUtil.toJsonType(parameterType)));
            }

            String name = method.getName();
            tools.put(name, new ToolDefinition(name, description.value(), parameters, toolBean, method));
        }
    }


    public synchronized List<ToolDefinition> getToolDefinitions() {
        List<ToolDefinition> definitions = new ArrayList<>();
        for (ToolDefinition tool : tools.values()) {
            definitions.add(tool);
        }
        return Collections.unmodifiableList(definitions);
    }

    public List<Message> execute(List<ToolCall> toolCalls) {
        // 依次执行模型请求的工具，并转换为模型可识别的工具结果消息。
        List<Message> messages = new ArrayList<>();
        for (ToolCall toolCall : toolCalls) {
            log.info("工具调用：id={}, name={}, arguments={}", toolCall.getId(), toolCall.getName(), toolCall.getArguments());
            ToolDefinition tool;
            synchronized (this) {
                tool = tools.get(toolCall.getName());
            }
            String result = invoke(tool, toolCall.getArguments() == null ? Collections.emptyMap() : toolCall.getArguments());
            log.info("工具结果：id={}, name={}, result={}", toolCall.getId(), toolCall.getName(), result);

            Message message = new Message(result, MessageRoleEnum.TOOL_EXEC_RESULT);
            message.setToolCallId(toolCall.getId());
            messages.add(message);
        }
        return messages;
    }

    private String invoke(ToolDefinition tool, Map<String, Object> arguments) {
        String name = tool.getName();
        List<ToolParameter> parameters = tool.getParameters();

        // 按方法签名转换模型参数，使业务方法不再感知 Map 和 JSON 类型。
        Object[] invocationArguments = new Object[parameters.size()];
        for (int index = 0; index < parameters.size(); index++) {
            ToolParameter parameter = parameters.get(index);
            try {
                invocationArguments[index] = objectMapper.convertValue(arguments.get(parameter.getName()), tool.getMethod().getParameterTypes()[index]);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "cannot convert tool argument for " + name + ": " + parameter.getName(), exception);
            }
        }

        // 统一包装反射异常，并将简单返回值转换为模型可消费的文本。
        try {
            Object result = tool.getMethod().invoke(tool.getTarget(), invocationArguments);
            return result == null ? "null" : String.valueOf(result);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("cannot access tool method: " + name, exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            throw new IllegalStateException("tool execution failed: " + name, cause);
        }
    }

}
