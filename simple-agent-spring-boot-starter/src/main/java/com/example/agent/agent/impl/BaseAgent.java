package com.example.agent.agent.impl;

import com.example.agent.agent.AbstractAgent;
import com.example.agent.agent.ChatConfig;
import com.example.agent.message.Message;
import com.example.agent.message.MessageHistoryManager;
import com.example.agent.message.MessageRoleEnum;
import com.openai.client.OpenAIClient;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

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

        List<Message> messages = new ArrayList<>();
        if (config != null && StringUtils.hasText(config.getSystemPrompt())) {
            messages.add(new Message(config.getSystemPrompt(), MessageRoleEnum.SYSTEM));
        }
        messages.addAll(messageHistoryManager.getHistory());

        Message userMessage = new Message(input, MessageRoleEnum.USER);
        messages.add(userMessage);

        Message assistantMessage = call(messages);
        messageHistoryManager.add(userMessage);
        messageHistoryManager.add(assistantMessage);
        return assistantMessage.getContent();
    }
}
