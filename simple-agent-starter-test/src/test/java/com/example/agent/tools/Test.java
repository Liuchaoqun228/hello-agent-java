package com.example.agent.tools;

import com.example.agent.TestApplication;
import com.example.agent.agent.Agent;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest(classes = TestApplication.class)
public class Test {

    @Resource
    private Agent agent;

    @org.junit.jupiter.api.Test
    void callTool() {
        // 使用启动类自动配置的 Agent 和自动扫描注册的 CommonTool。
        String result = agent.chat("请调用 add 工具计算 12 + 30，只返回结果。");
        System.out.println(result);
    }
}
