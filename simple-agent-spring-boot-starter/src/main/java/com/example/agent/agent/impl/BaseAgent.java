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

import java.util.ArrayList;
import java.util.List;

public class BaseAgent extends AbstractAgent {

    private static final int MAX_TOOL_CALL_ROUNDS = 10;

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

        List<Message> history = messageHistoryManager.getHistory();
        List<Message> turnMessages = new ArrayList<>();
        List<Message> workingMessages = new ArrayList<>(history);

        if (history.isEmpty() && config != null && StringUtils.hasText(config.getSystemPrompt())) {
            turnMessages.add(new Message(config.getSystemPrompt(), MessageRoleEnum.SYSTEM));
        }

        Message userMessage = new Message(input, MessageRoleEnum.USER);
        turnMessages.add(userMessage);
        workingMessages.addAll(turnMessages);

        List<Tool> tools = toolRegistry.getTools();
        int toolCallRounds = 0;

        while (true) {
            Message assistantMessage = call(workingMessages, tools);
            turnMessages.add(assistantMessage);
            workingMessages.add(assistantMessage);

            List<ToolCall> toolCalls = assistantMessage.getAssistantNeedExecToolCallList();
            if (CollectionUtils.isEmpty(toolCalls)) {
                Assert.hasText(assistantMessage.getContent(), "assistant answer must not be empty");
                messageHistoryManager.addAll(turnMessages);
                return assistantMessage.getContent();
            }

            Assert.state(toolCallRounds < MAX_TOOL_CALL_ROUNDS,
                    "tool call rounds exceed limit: " + MAX_TOOL_CALL_ROUNDS);
            toolCallRounds++;

            for (ToolCall toolCall : toolCalls) {
                String result = toolRegistry.execute(toolCall.getName(), toolCall.getArguments());
                Assert.hasText(result, "tool exec result must not be empty: " + toolCall.getName());

                Message toolExecResult = new Message(result, MessageRoleEnum.TOOL_EXEC_RESULT);
                toolExecResult.setToolCallId(toolCall.getId());
                turnMessages.add(toolExecResult);
                workingMessages.add(toolExecResult);
            }
        }
    }
}
