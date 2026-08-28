package com.example.agent;

import com.example.agent.agent.Agent;
import com.example.agent.agent.BaseAgent;
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
    private Agent agent;

    @Test
    void shouldCreateDefaultAgent() {
        assertThat(agent).isInstanceOf(BaseAgent.class);
    }

}
