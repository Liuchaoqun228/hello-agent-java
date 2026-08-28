package com.example.agent;

import com.openai.models.chat.completions.ChatCompletion;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        properties = {
                "simple.agent.openai.api-key=test-key",
                "simple.agent.openai.base-url=http://localhost:1234/v1",
                "simple.agent.openai.model=google/gemma-4-e2b"
        }
)
class SimpleAgentStarterTest {

    @Resource
    private SimpleAgentClient simpleAgentClient;

    @Test
    void shouldCallChatCompletion() {
        assertThat(simpleAgentClient).isNotNull();
        ChatCompletion chatResult = simpleAgentClient.chat("你好");

        assertThat(chatResult.choices()).isNotEmpty();
        String content = chatResult.choices().get(0).message().content().orElse("");
        assertThat(content).isNotBlank();
        System.out.println("LLM 返回结果: " + content);
    }

}
