package com.example.agent.agent;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.springframework.util.Assert;

public abstract class AbstractAgent implements Agent {

    private final OpenAIClient openAIClient;
    private final String model;

    protected AbstractAgent(OpenAIClient openAIClient, String model) {
        Assert.notNull(openAIClient, "openAIClient must not be null");
        Assert.hasText(model, "model must not be empty");
        this.openAIClient = openAIClient;
        this.model = model;
    }

    @Override
    public final ChatCompletion run(String input) {
        Assert.hasText(input, "input must not be empty");
        ChatCompletionCreateParams request = buildRequest(input);
        Assert.notNull(request, "request must not be null");
        return openAIClient.chat().completions().create(request);
    }

    protected ChatCompletionCreateParams buildRequest(String input) {
        return ChatCompletionCreateParams.builder()
                .model(model)
                .addUserMessage(input)
                .build();
    }

}
