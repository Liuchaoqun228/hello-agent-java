package com.example.agent.agent;

import com.openai.client.OpenAIClient;

public class BaseAgent extends AbstractAgent {

    public BaseAgent(OpenAIClient openAIClient, String model) {
        super(openAIClient, model);
    }
}
