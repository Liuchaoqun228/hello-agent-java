package com.example.agent;

import com.example.agent.agent.Agent;
import com.example.agent.agent.ChatOptions;
import com.example.agent.agent.impl.BaseAgent;
import com.example.agent.autoconfigure.LLMProperties;
import com.example.agent.tool.CalculatorTool;
import com.example.agent.tool.ToolManager;
import com.openai.client.OpenAIClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * 演示如何为 Agent 注册 Spring 管理的普通 Java 工具。
 */
@SpringBootTest(classes = ExampleApplication.class)
class ToolAgentTest {

    @Resource
    private OpenAIClient openAIClient;

    @Resource
    private LLMProperties llmProperties;

    @Resource
    private CalculatorTool calculatorTool;

    /**
     * 验证 Agent 能够调用注册的加法工具完成计算。
     */
    @Test
    void shouldCallCalculatorTool() {
        // 将独立创建的 Agent 与本次测试所需的工具绑定。
        Agent agent = new BaseAgent(openAIClient, llmProperties.getModel());
        ToolManager toolManager = new ToolManager();
        toolManager.register(calculatorTool);
        agent.setToolManager(toolManager);

        // 由模型生成工具调用参数，框架负责执行 CalculatorTool。
        ChatOptions options = new ChatOptions();
        options.setSystemPrompt("回答数学计算问题时，必须使用已提供的工具。");
        String reply = agent.chat("请计算 135 + 287，并只返回计算结果。", options);
        System.out.println("工具调用回答：" + reply);
        Assertions.assertTrue(reply.contains("422"), "Agent 应返回工具计算结果");
    }
}
