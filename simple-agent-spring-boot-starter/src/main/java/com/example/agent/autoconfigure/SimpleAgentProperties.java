package com.example.agent.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = SimpleAgentProperties.PREFIX)
public class SimpleAgentProperties {

    public static final String PREFIX = "simple.agent.openai";

    private String apiKey;
    private String baseUrl = "https://api.openai.com/v1";
    private String model;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
