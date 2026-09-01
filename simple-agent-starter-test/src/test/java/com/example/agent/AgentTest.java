package com.example.agent;

import com.example.agent.agent.Agent;
import com.example.agent.agent.ChatOptions;
import com.example.agent.agent.impl.BaseAgent;
import com.example.agent.autoconfigure.LLMProperties;
import com.example.agent.memory.MemoryTool;
import com.example.agent.tool.ToolManager;
import com.openai.client.OpenAIClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.UUID;

@SpringBootTest(classes = TestApplication.class)
class AgentTest {

    @Resource
    private Agent agent;

    @Resource
    private CommonTool commonTool;

    @Resource
    private OpenAIClient openAIClient;

    @Resource
    private LLMProperties llmProperties;

    @Resource
    private MemoryTool memoryTool;

    @Test
    void shouldUseCommonTool() {

        ToolManager toolManager = new ToolManager();
        // 调用方自行决定向 Agent 使用的工具管理器注册哪些工具。
        toolManager.register(commonTool);
        agent.setToolManager(toolManager);
        String result = agent.chat("请调用 add 工具计算 12 + 30，只返回计算结果。");
        System.out.println(result);
    }

    @Test
    void shouldRememberUserAcrossConversations() {
        ToolManager toolManager = new ToolManager();


        ChatOptions options = new ChatOptions();
        options.setSystemPrompt("你是一个 AI 助手 当用户问你不知道的东西时候 你应该优先检索记忆 如果检索不到 你在回答");
        // 每次运行使用独立用户，避免历史记忆文件干扰断言。
        options.setUserId("demo-user-1");
        memoryTool.setUserId(options.getUserId());
        // 注册记忆工具即启用记忆，是否启用由调用方决定。
        toolManager.register(memoryTool);


//        BaseAgent firstAgent = new BaseAgent(openAIClient, llmProperties.getModel());
//        firstAgent.setToolManager(toolManager);
//        firstAgent.chat("你好，我叫张三，是一名 Java 开发者。请把这些信息记下来。", options);

        // 新实例没有对话历史，只能依赖记忆工具回答问题，从而验证记忆的真实生效。
        BaseAgent secondAgent = new BaseAgent(openAIClient, llmProperties.getModel());
        secondAgent.setToolManager(toolManager);
        String result = secondAgent.chat("我叫什么名字？我是做什么工作的？", options);
        System.out.println(result);
        Assertions.assertTrue(result.contains("张三"), "模型应通过记忆工具想起用户的名字");
    }
}
