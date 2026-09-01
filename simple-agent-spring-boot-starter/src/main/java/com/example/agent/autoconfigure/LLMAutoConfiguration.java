package com.example.agent.autoconfigure;

import com.example.agent.agent.Agent;
import com.example.agent.agent.impl.BaseAgent;
import com.example.agent.memory.JsonMemoryStrategyStrategyImpl;
import com.example.agent.memory.MemoryStrategy;
import com.example.agent.memory.MemoryTool;
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
@EnableConfigurationProperties({LLMProperties.class, MemoryProperties.class})
public class LLMAutoConfiguration {

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
        BaseAgent agent = new BaseAgent(openAIClient, properties.getModel());
        return agent;
    }

    // 用户未提供自定义 MemoryStrategy 时，使用每用户一个 JSON 文件的默认实现。
    @Bean
    @ConditionalOnMissingBean
    public MemoryStrategy memory(MemoryProperties properties) {
        return new JsonMemoryStrategyStrategyImpl(properties.getStoragePath());
    }

    @Bean
    @ConditionalOnMissingBean
    public MemoryTool memoryTool(MemoryStrategy memoryStrategy) {
        return new MemoryTool(memoryStrategy);
    }
}
