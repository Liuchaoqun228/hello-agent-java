package com.example.agent;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.springframework.util.Assert;

public class SimpleAgentClient {

    private final OpenAIClient openAIClient;
    private final String model;

    public SimpleAgentClient(OpenAIClient openAIClient, String model) {
        this.openAIClient = openAIClient;
        this.model = model;
    }

    public ChatCompletion chat(String prompt) {
        Assert.hasText(prompt, "prompt must not be empty");

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(model)
                .addUserMessage(prompt)
                .build();
        return openAIClient.chat().completions().create(params);
    }
}
