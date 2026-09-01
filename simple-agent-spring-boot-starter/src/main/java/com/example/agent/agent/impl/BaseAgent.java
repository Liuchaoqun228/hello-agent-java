package com.example.agent.agent.impl;

import com.example.agent.agent.AbstractAgent;
import com.example.agent.agent.ChatOptions;
import com.example.agent.message.Message;
import com.example.agent.message.MessageHistoryManager;
import com.example.agent.message.MessageRoleEnum;
import com.example.agent.memory.MemoryTool;
import com.example.agent.tool.dto.ToolCall;
import com.example.agent.tool.dto.ToolDefinition;
import com.example.agent.tool.ToolManager;
import com.openai.client.OpenAIClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

public class BaseAgent extends AbstractAgent {

    private static final Logger log = LoggerFactory.getLogger(BaseAgent.class);
    private static final int MAX_TOOL_CALL_ROUNDS = 3;

    private final MessageHistoryManager messageHistoryManager = new MessageHistoryManager();
    private ToolManager toolManager;

    public BaseAgent(OpenAIClient openAIClient, String model) {
        super(openAIClient, model);
    }

    @Override
    public void setToolManager(ToolManager toolManager) {
        this.toolManager = toolManager;
    }

    @Override
    public String chat(String input) {
        return chat(input, null);
    }

    @Override
    public String chat(String input, ChatOptions config) {
        Assert.hasText(input, "input must not be empty");
        log.info("用户消息：{}", input);

        // 基于已提交的对话历史构建本次请求。
        List<Message> messages = messageHistoryManager.getHistory();
        int newMessagesStart = messages.size();

        // 仅在新对话开始时添加系统提示词。
        if (messages.isEmpty() && config != null && StringUtils.hasText(config.getSystemPrompt())) {
            messages.add(new Message(config.getSystemPrompt(), MessageRoleEnum.SYSTEM));
        }

        messages.add(new Message(input, MessageRoleEnum.USER));

        Message assistantMessage;

        // 未配置工具时直接请求模型，不进入工具调用流程。
        if (toolManager == null || CollectionUtils.isEmpty(toolManager.getToolDefinitions())) {
            assistantMessage = call(messages);
            messages.add(assistantMessage);
            Assert.state(CollectionUtils.isEmpty(assistantMessage.getToolCallList()), "model returned tool calls but no tool manager is configured");
        } else {
            // 请求模型生成最终回答或发起工具调用。
            List<ToolDefinition> tools = toolManager.getToolDefinitions();
            assistantMessage = call(messages, tools);
            messages.add(assistantMessage);
            List<ToolCall> toolCalls = assistantMessage.getToolCallList();

            // 执行模型请求的全部工具，并在轮次限制内让模型继续处理。
            for (int round = 0; !CollectionUtils.isEmpty(toolCalls) && round < MAX_TOOL_CALL_ROUNDS; round++) {
                // 执行工具
                List<Message> toolsResultMessaList = toolManager.execute(toolCalls);
                messages.addAll(toolsResultMessaList);
                //  工具执行完 在此调用大模型
                assistantMessage = call(messages, tools);
                messages.add(assistantMessage);
                // 看看是否需要调用别的工具
                toolCalls = assistantMessage.getToolCallList();
            }

            Assert.state(CollectionUtils.isEmpty(toolCalls), "tool call rounds exceed limit: " + MAX_TOOL_CALL_ROUNDS);
        }
        // 仅将成功完成的本轮消息提交到对话历史。
        Assert.hasText(assistantMessage.getContent(), "assistant answer must not be empty");
        messageHistoryManager.addAll(messages.subList(newMessagesStart, messages.size()));
        log.info("Agent 消息：{}", assistantMessage.getContent());
        return assistantMessage.getContent();
    }

}
