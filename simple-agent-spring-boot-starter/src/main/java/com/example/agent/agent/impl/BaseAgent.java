package com.example.agent.agent.impl;

import com.example.agent.agent.AbstractAgent;
import com.example.agent.agent.ChatOptions;
import com.example.agent.message.Message;
import com.example.agent.message.MessageHistoryManager;
import com.example.agent.message.MessageRoleEnum;
import com.example.agent.tool.Tool;
import com.example.agent.tool.ToolCall;
import com.example.agent.tool.ToolRegistry;
import com.openai.client.OpenAIClient;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

public class BaseAgent extends AbstractAgent {

    private static final int MAX_TOOL_CALL_ROUNDS = 3;

    private final MessageHistoryManager messageHistoryManager = new MessageHistoryManager();
    private final ToolRegistry toolRegistry;

    public BaseAgent(OpenAIClient openAIClient, String model) {
        this(openAIClient, model, new ToolRegistry());
    }

    public BaseAgent(OpenAIClient openAIClient, String model, ToolRegistry toolRegistry) {
        super(openAIClient, model);
        Assert.notNull(toolRegistry, "toolRegistry must not be null");
        this.toolRegistry = toolRegistry;
    }

    @Override
    public String chat(String input) {
        return chat(input, null);
    }

    @Override
    public String chat(String input, ChatOptions config) {
        Assert.hasText(input, "input must not be empty");

        // 基于已提交的对话历史构建本次请求。
        List<Message> messages = messageHistoryManager.getHistory();
        int newMessagesStart = messages.size();

        // 仅在新对话开始时添加系统提示词。
        if (messages.isEmpty() && config != null && StringUtils.hasText(config.getSystemPrompt())) {
            messages.add(new Message(config.getSystemPrompt(), MessageRoleEnum.SYSTEM));
        }

        messages.add(new Message(input, MessageRoleEnum.USER));

        // 请求模型生成最终回答或发起工具调用。
        List<Tool> tools = toolRegistry.getTools();
        Message assistantMessage = call(messages, tools);
        messages.add(assistantMessage);
        List<ToolCall> toolCalls = assistantMessage.getToolCallList();

        // 执行模型请求的全部工具，并在轮次限制内让模型继续处理。
        for (int round = 0; !CollectionUtils.isEmpty(toolCalls) && round < MAX_TOOL_CALL_ROUNDS; round++) {
            for (ToolCall toolCall : toolCalls) {
                String result = toolRegistry.execute(toolCall.getName(), toolCall.getArguments());
                Assert.hasText(result, "tool exec result must not be empty: " + toolCall.getName());

                Message toolExecResult = new Message(result, MessageRoleEnum.TOOL_EXEC_RESULT);
                toolExecResult.setToolCallId(toolCall.getId());
                messages.add(toolExecResult);
            }
            //  工具执行完 在此调用大模型
            assistantMessage = call(messages, tools);
            messages.add(assistantMessage);
            // 看看是否需要调用别的工具
            toolCalls = assistantMessage.getToolCallList();
        }

        // 仅将成功完成的本轮消息提交到对话历史。
        Assert.state(CollectionUtils.isEmpty(toolCalls), "tool call rounds exceed limit: " + MAX_TOOL_CALL_ROUNDS);
        Assert.hasText(assistantMessage.getContent(), "assistant answer must not be empty");
        messageHistoryManager.addAll(messages.subList(newMessagesStart, messages.size()));
        return assistantMessage.getContent();
    }
}
