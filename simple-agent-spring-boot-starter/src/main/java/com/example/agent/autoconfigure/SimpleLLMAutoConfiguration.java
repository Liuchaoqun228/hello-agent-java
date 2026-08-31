package com.example.agent.autoconfigure;

import com.example.agent.agent.Agent;
import com.example.agent.agent.impl.BaseAgent;
import com.example.agent.tool.ToolManager;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(OpenAIClient.class)
@ConditionalOnProperty(
        prefix = LLMProperties.PREFIX,
        name = {"api-key", "model"}
)
@EnableConfigurationProperties(LLMProperties.class)
public class SimpleLLMAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OpenAIClient openAIClient(LLMProperties properties) {
        return OpenAIOkHttpClient.builder()
                .apiKey(properties.getApiKey())
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(Agent.class)
    public BaseAgent baseAgent(OpenAIClient openAIClient,
                               LLMProperties properties) {
        return new BaseAgent(openAIClient, properties.getModel());
    }
}
