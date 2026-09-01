package com.example.agent.memory;

import com.example.agent.memory.dto.MemoryItem;
import com.example.agent.tool.anno.Tool;
import com.example.agent.tool.anno.ToolDescription;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;

// 记忆工具：将记忆读写能力暴露给大模型，由模型决定何时记住、何时查询。
// 是否启用记忆由调用方决定：把本工具注册进 ToolManager 即启用，不注册则不生效。
@Tool
public class MemoryTool {

    private static final String DEFAULT_USER_ID = "default";
    private static final int DEFAULT_LIMIT = 5;

    private final MemoryStrategy memoryStrategy;
    private String userId = DEFAULT_USER_ID;

    public MemoryTool(MemoryStrategy memoryStrategy) {
        Assert.notNull(memoryStrategy, "memoryStrategy must not be null");
        this.memoryStrategy = memoryStrategy;
    }

    // 每轮对话开始前由 Agent 绑定当前用户，工具执行与模型调用同线程进行，绑定安全。
    public void setUserId(String userId) {
        this.userId = StringUtils.hasText(userId) ? userId : DEFAULT_USER_ID;
    }

    @ToolDescription("记住用户透露的个人信息、偏好或经历；content 为要记住的内容，importance 为重要性（0-1，可省略）")
    public String addMemory(String content, Double importance) {
        MemoryItem item = memoryStrategy.add(userId, content, importance);
        return "已记住（id=" + item.getId() + "）：" + item.getContent();
    }

    @ToolDescription("按关键词搜索当前用户的记忆，返回最相关的记忆条目")
    public String searchMemory(String query, Integer limit) {
        List<MemoryItem> items = memoryStrategy.search(userId, query, limit == null ? DEFAULT_LIMIT : limit);
        if (items.isEmpty()) {
            return "没有找到相关记忆。";
        }
        return format(items, "找到 " + items.size() + " 条相关记忆");
    }

    @ToolDescription("列出当前用户的全部记忆（含 id），用于整体回顾，也用于获取更新某条记忆所需的 id")
    public String listMemory() {
        List<MemoryItem> items = memoryStrategy.list(userId);
        if (items.isEmpty()) {
            return "当前没有记忆。";
        }
        return format(items, "当前用户共 " + items.size() + " 条记忆");
    }

    @ToolDescription("按 id 更新一条记忆的 content；importance 可省略表示保持不变")
    public String updateMemory(String id, String content, Double importance) {
        MemoryItem item = memoryStrategy.update(userId, id, content, importance);
        return "已更新（id=" + item.getId() + "）：" + item.getContent();
    }

    private String format(List<MemoryItem> items, String headline) {
        StringBuilder result = new StringBuilder(headline).append("：\n");
        for (MemoryItem item : items) {
            result.append("- [").append(item.getId()).append("] ").append(item.getContent())
                    .append("（重要性 ").append(item.getImportance()).append("）\n");
        }
        return result.toString();
    }
}
