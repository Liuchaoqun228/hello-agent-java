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

/**
 * 演示如何使用 MemoryTool 跨对话保存和读取用户记忆。
 */
@SpringBootTest(classes = ExampleApplication.class)
class MemoryAgentTest {

    @Resource
    private OpenAIClient openAIClient;

    @Resource
    private LLMProperties llmProperties;

    @Resource
    private MemoryTool memoryTool;

    /**
     * 验证无对话历史的新 Agent 能够读取另一个 Agent 写入的记忆。
     */
    @Test
    void shouldRecallMemoryAcrossAgents() {
        String userId = "memory-example-" + UUID.randomUUID();

        // 第一个 Agent 通过 MemoryTool 将用户信息写入持久化存储。
        memoryTool.setUserId(userId);
        ToolManager writeToolManager = new ToolManager();
        writeToolManager.register(memoryTool);
        Agent writeAgent = new BaseAgent(openAIClient, llmProperties.getModel());
        writeAgent.setToolManager(writeToolManager);

        ChatOptions writeOptions = new ChatOptions();
        writeOptions.setUserId(userId);
        writeOptions.setSystemPrompt("你是带有长期记忆的助手。用户要求记住信息时，必须调用 addMemory。");
        String writeReply = writeAgent.chat("请记住：我叫小明，最喜欢的编程语言是 Java。", writeOptions);
        System.out.println("写入记忆回答：" + writeReply);
        Assertions.assertTrue(memoryTool.searchMemory("小明", 5).contains("Java"), "MemoryTool 应持久化用户记忆");

        // 第二个 Agent 没有上一次的对话历史，只能通过 MemoryTool 找回答案。
        memoryTool.setUserId(userId);
        ToolManager readToolManager = new ToolManager();
        readToolManager.register(memoryTool);
        Agent readAgent = new BaseAgent(openAIClient, llmProperties.getModel());
        readAgent.setToolManager(readToolManager);

        ChatOptions readOptions = new ChatOptions();
        readOptions.setUserId(userId);
        readOptions.setSystemPrompt("你是带有长期记忆的助手。回答用户个人信息前，必须先调用 searchMemory。");
        String readReply = readAgent.chat("我叫什么名字？最喜欢哪门编程语言？", readOptions);
        System.out.println("读取记忆回答：" + readReply);
        Assertions.assertTrue(readReply.contains("小明") && readReply.contains("Java"), "新 Agent 应通过记忆回答用户信息");
    }
}
