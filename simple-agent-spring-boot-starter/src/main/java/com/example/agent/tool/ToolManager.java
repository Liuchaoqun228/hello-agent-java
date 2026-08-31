package com.example.agent.tool;

import com.example.agent.message.Message;
import com.example.agent.message.MessageRoleEnum;
import com.example.agent.tool.dto.ToolCall;
import com.example.agent.tool.dto.ToolDefinition;
import com.example.agent.tool.dto.ToolParameter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ToolManager {

    private static final Logger log = LoggerFactory.getLogger(ToolManager.class);

    private final Map<String, RegisteredTool> tools = new LinkedHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();



    public synchronized List<ToolDefinition> getToolDefinitions() {
        List<ToolDefinition> definitions = new ArrayList<>();
        for (RegisteredTool tool : tools.values()) {
            definitions.add(tool.definition);
        }
        return Collections.unmodifiableList(definitions);
    }

    public String execute(String name, Map<String, Object> arguments) {
        Assert.hasText(name, "tool name must not be empty");

        RegisteredTool tool;
        synchronized (this) {
            tool = tools.get(name);
        }
        Assert.notNull(tool, "tool not registered: " + name);
        return execute(tool, arguments == null ? Collections.emptyMap() : arguments);
    }

    public List<Message> execute(List<ToolCall> toolCalls) {
        Assert.notEmpty(toolCalls, "toolCalls must not be empty");

        // 依次执行模型请求的工具，并转换为模型可识别的工具结果消息。
        List<Message> messages = new ArrayList<>();
        for (ToolCall toolCall : toolCalls) {
            Assert.notNull(toolCall, "toolCall must not be null");
            log.info("工具调用：id={}, name={}, arguments={}", toolCall.getId(), toolCall.getName(), toolCall.getArguments());
            String result = execute(toolCall.getName(), toolCall.getArguments());
            Assert.hasText(result, "tool exec result must not be empty: " + toolCall.getName());
            log.info("工具结果：id={}, name={}, result={}", toolCall.getId(), toolCall.getName(), result);

            Message message = new Message(result, MessageRoleEnum.TOOL_EXEC_RESULT);
            message.setToolCallId(toolCall.getId());
            messages.add(message);
        }
        return messages;
    }

    private String execute(RegisteredTool tool, Map<String, Object> arguments) {
        String name = tool.definition.getName();
        List<ToolParameter> parameters = tool.definition.getParameters();

        // 严格校验必填参数和额外参数，尽早暴露模型生成的不合法调用。
        Set<String> expectedNames = new LinkedHashSet<>();
        for (ToolParameter parameter : parameters) {
            expectedNames.add(parameter.getName());
            Assert.isTrue(arguments.containsKey(parameter.getName()),
                    "missing tool argument for " + name + ": " + parameter.getName());
            Assert.notNull(arguments.get(parameter.getName()),
                    "tool argument must not be null for " + name + ": " + parameter.getName());
        }
        for (String argumentName : arguments.keySet()) {
            Assert.isTrue(expectedNames.contains(argumentName),
                    "unexpected tool argument for " + name + ": " + argumentName);
        }

        // 按方法签名转换模型参数，使业务方法不再感知 Map 和 JSON 类型。
        Object[] invocationArguments = new Object[parameters.size()];
        for (int index = 0; index < parameters.size(); index++) {
            ToolParameter parameter = parameters.get(index);
            try {
                invocationArguments[index] = objectMapper.convertValue(arguments.get(parameter.getName()), tool.parameterTypes[index]);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "cannot convert tool argument for " + name + ": " + parameter.getName(), exception);
            }
        }

        // 统一包装反射异常，并将简单返回值转换为模型可消费的文本。
        try {
            Object result = tool.method.invoke(tool.target, invocationArguments);
            return result == null ? "null" : String.valueOf(result);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("cannot access tool method: " + name, exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            throw new IllegalStateException("tool execution failed: " + name, cause);
        }
    }

    private boolean isSupportedSimpleType(Class<?> type) {
        return type == String.class || type == Character.class || type == Character.TYPE
                || type == Boolean.class || type == Boolean.TYPE || isIntegerType(type)
                || isNumberType(type) || type.isEnum();
    }

    private boolean isIntegerType(Class<?> type) {
        return type == Byte.class || type == Byte.TYPE || type == Short.class || type == Short.TYPE
                || type == Integer.class || type == Integer.TYPE || type == Long.class || type == Long.TYPE;
    }

    private boolean isNumberType(Class<?> type) {
        return type == Float.class || type == Float.TYPE || type == Double.class || type == Double.TYPE;
    }

    private static final class RegisteredTool {

        private final ToolDefinition definition;
        private final Object target;
        private final Method method;
        private final Class<?>[] parameterTypes;

        private RegisteredTool(ToolDefinition definition, Object target, Method method, Class<?>[] parameterTypes) {
            this.definition = definition;
            this.target = target;
            this.method = method;
            this.parameterTypes = parameterTypes;
        }
    }
}
