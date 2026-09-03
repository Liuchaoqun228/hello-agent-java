package com.example.agent;

import com.example.agent.agent.Agent;
import com.example.agent.agent.ChatOptions;
import com.example.agent.agent.impl.BaseAgent;
import com.example.agent.autoconfigure.LLMProperties;
import com.openai.client.OpenAIClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * 演示基础 Agent 的创建方式和多轮对话能力。
 */
@SpringBootTest(classes = ExampleApplication.class)
class ChatAgentTest {

    @Resource
    private OpenAIClient openAIClient;

    @Resource
    private LLMProperties llmProperties;

    /**
     * 验证同一个 Agent 能够在后续对话中使用已有上下文。
     */
    @Test
    void shouldKeepConversationContext() {
        // 直接使用 Starter 自动配置的客户端和模型创建 Agent。
        Agent agent = new BaseAgent(openAIClient, llmProperties.getModel());
        ChatOptions options = new ChatOptions();
        options.setSystemPrompt("你是一位友善的 Java 学习助手，回答尽量简洁。");

        // 复用同一个 Agent，第二轮会自动携带第一轮的对话历史。
        String firstReply = agent.chat("我叫小明，正在学习 Spring Boot。", options);
        System.out.println("第一轮回答：" + firstReply);

        String secondReply = agent.chat("我叫什么名字？正在学习什么？");
        System.out.println("第二轮回答：" + secondReply);
        Assertions.assertTrue(secondReply.contains("小明"), "Agent 应记得用户名字");
        Assertions.assertTrue(secondReply.contains("Spring Boot") || secondReply.contains("SpringBoot"), "Agent 应记得学习内容");
    }
}
