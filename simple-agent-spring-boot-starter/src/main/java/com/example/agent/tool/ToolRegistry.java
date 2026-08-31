package com.example.agent.tool;

import com.example.agent.message.Message;
import com.example.agent.message.MessageRoleEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);

    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public ToolRegistry() {
    }

    public ToolRegistry(List<Tool> tools) {
        if (tools != null) {
            for (Tool tool : tools) {
                register(tool);
            }
        }
    }

    public synchronized void register(Tool tool) {
        Assert.notNull(tool, "tool must not be null");
        Assert.isTrue(!tools.containsKey(tool.getName()),
                "tool name already registered: " + tool.getName());
        tools.put(tool.getName(), tool);
    }

    public synchronized List<Tool> getTools() {
        return Collections.unmodifiableList(new ArrayList<>(tools.values()));
    }

    public String execute(String name, Map<String, Object> arguments) {
        Assert.hasText(name, "tool name must not be empty");

        Tool tool;
        synchronized (this) {
            tool = tools.get(name);
        }
        Assert.notNull(tool, "tool not registered: " + name);
        return tool.execute(arguments == null ? Collections.emptyMap() : arguments);
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
}
