package com.example.agent;

import com.example.agent.agent.Agent;
import com.example.agent.tool.Tool;
import com.example.agent.tool.ToolParameter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {TestApplication.class, LocalModelToolCallingTest.ToolConfiguration.class})
class LocalModelToolCallingTest {

    private static final String SECRET_CODE = "LOCAL_TOOL_CODE_7391";

    @Resource
    private Agent agent;

    @Test
    void shouldCallToolWithLocalModel() {
        // 通过未知验证码确认模型确实执行了工具，而不是自行生成答案。
        String answer = agent.chat("请调用 get_secret_code 工具获取验证码，并只返回工具给出的验证码。");

        assertThat(answer).contains(SECRET_CODE);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ToolConfiguration {

        @Bean
        Tool secretCodeTool() {
            return new Tool("get_secret_code", "获取本地保存的验证码") {
                @Override
                public List<ToolParameter> getParameters() {
                    return Collections.emptyList();
                }

                @Override
                public String execute(Map<String, Object> arguments) {
                    return SECRET_CODE;
                }
            };
        }
    }
}
