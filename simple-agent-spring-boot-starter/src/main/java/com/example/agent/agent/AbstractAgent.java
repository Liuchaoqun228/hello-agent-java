package com.example.agent.agent;

import com.example.agent.message.Message;
import com.example.agent.message.MessageRoleEnum;
import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;

public abstract class AbstractAgent implements Agent {

    private final OpenAIClient openAIClient;
    private final String model;

    protected AbstractAgent(OpenAIClient openAIClient, String model) {
        Assert.notNull(openAIClient, "openAIClient must not be null");
        Assert.hasText(model, "model must not be empty");
        this.openAIClient = openAIClient;
        this.model = model;
    }

    protected final Message call(Message message) {
        Assert.notNull(message, "message must not be null");
        return call(Collections.singletonList(message));
    }

    protected final Message call(List<Message> messages) {
        Assert.notEmpty(messages, "messages must not be empty");

        ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder()
                .model(model);

        for (Message message : messages) {
            addMessage(builder, message);
        }

        ChatCompletion completion = openAIClient.chat().completions().create(builder.build());
        Assert.state(!completion.choices().isEmpty(), "completion choices must not be empty");

        String content = completion.choices().get(0).message().content()
                .orElseThrow(() -> new IllegalStateException("completion does not contain a text answer"));
        Assert.hasText(content, "completion answer must not be empty");
        return new Message(content, MessageRoleEnum.ASSISTANT);
    }

    private void addMessage(ChatCompletionCreateParams.Builder builder, Message message) {
        Assert.notNull(message, "message must not be null");
        Assert.notNull(message.getMessageRole(), "message role must not be null");
        Assert.hasText(message.getContent(), "message content must not be empty");

        if (message.getMessageRole() == MessageRoleEnum.SYSTEM) {
            builder.addSystemMessage(message.getContent());
        } else if (message.getMessageRole() == MessageRoleEnum.USER) {
            builder.addUserMessage(message.getContent());
        } else if (message.getMessageRole() == MessageRoleEnum.ASSISTANT) {
            builder.addAssistantMessage(message.getContent());
        } else {
            throw new IllegalArgumentException("unsupported message role: " + message.getMessageRole());
        }
    }

}
