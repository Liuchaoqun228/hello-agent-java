package com.example.agent.agent.impl;

import com.example.agent.agent.AbstractAgent;
import com.openai.client.OpenAIClient;

public class BaseAgent extends AbstractAgent {

    public BaseAgent(OpenAIClient openAIClient, String model) {
        super(openAIClient, model);
    }
}
