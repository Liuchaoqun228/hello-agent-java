package com.example.agent;

import com.example.agent.agent.Agent;
import com.example.agent.agent.impl.BaseAgent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SimpleAgentStarterTest {

    @Resource
    private Agent agent;

    @Test
    void shouldCreateDefaultAgent() {
        assertThat(agent).isInstanceOf(BaseAgent.class);
    }

}
