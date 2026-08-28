package com.example.agent.agent.impl;

import com.example.agent.agent.AbstractAgent;
import com.example.agent.agent.ChatConfig;
import com.example.agent.message.Message;
import com.example.agent.message.MessageHistoryManager;
import com.example.agent.message.MessageRoleEnum;
import com.openai.client.OpenAIClient;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class BaseAgent extends AbstractAgent {

    private final MessageHistoryManager messageHistoryManager = new MessageHistoryManager();

    public BaseAgent(OpenAIClient openAIClient, String model) {
        super(openAIClient, model);
    }

    @Override
    public String chat(String input) {
        return chat(input, null);
    }

    @Override
    public String chat(String input, ChatConfig config) {
        Assert.hasText(input, "input must not be empty");

        Message systemMessage = null;
        if (messageHistoryManager.isEmpty() && config != null && StringUtils.hasText(config.getSystemPrompt())) {
            systemMessage = new Message(config.getSystemPrompt(), MessageRoleEnum.SYSTEM);
            messageHistoryManager.add(systemMessage);
        }

        Message userMessage = new Message(input, MessageRoleEnum.USER);
        messageHistoryManager.add(userMessage);

        try {
            Message assistantMessage = call(messageHistoryManager.getHistory());
            messageHistoryManager.add(assistantMessage);
            return assistantMessage.getContent();
        } catch (RuntimeException exception) {
            messageHistoryManager.remove(userMessage);
            if (systemMessage != null) {
                messageHistoryManager.remove(systemMessage);
            }
            throw exception;
        }
    }
}
