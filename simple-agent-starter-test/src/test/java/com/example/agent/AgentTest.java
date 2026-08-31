package com.example.agent;

import com.example.agent.agent.Agent;
import com.example.agent.tool.ToolManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest(classes = TestApplication.class)
class AgentTest {

    @Resource
    private Agent agent;

    @Resource
    private CommonTool commonTool;

    @Test
    void shouldUseCommonTool() {

        ToolManager toolManager = new ToolManager();
        // 调用方自行决定向 Agent 使用的工具管理器注册哪些工具。
        toolManager.register(commonTool);
        agent.setToolManager(toolManager);
        String result = agent.chat("请调用 add 工具计算 12 + 30，只返回计算结果。");
        System.out.println(result);
    }
}
