package com.example.agent.autoconfigure;

import com.example.agent.SimpleAgentClient;
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
        prefix = SimpleAgentProperties.PREFIX,
        name = {"api-key", "model"}
)
@EnableConfigurationProperties(SimpleAgentProperties.class)
public class SimpleAgentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OpenAIClient openAIClient(SimpleAgentProperties properties) {
        return OpenAIOkHttpClient.builder()
                .apiKey(properties.getApiKey())
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public SimpleAgentClient simpleAgentClient(
            OpenAIClient openAIClient,
            SimpleAgentProperties properties) {
        return new SimpleAgentClient(openAIClient, properties.getModel());
    }
}
