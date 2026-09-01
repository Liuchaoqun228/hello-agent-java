package com.example.agent;

import com.example.agent.memory.JsonMemoryStrategy;
import com.example.agent.memory.Memory;
import com.example.agent.memory.dto.MemoryItem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

class JsonMemoryStrategyTest {

    @TempDir
    Path tempDir;

    private Memory memory;

    @BeforeEach
    void setUp() {
        memory = new JsonMemoryStrategy(tempDir.toString());
    }

    @Test
    void shouldAddAndSearchMemory() {
        memory.add("user1", "用户是 Java 开发者", 0.9);

        List<MemoryItem> result = memory.search("user1", "开发者", 5);
        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(result.get(0).getContent().contains("Java"));
    }

    @Test
    void shouldSortSearchResultByImportance() {
        memory.add("user1", "不重要的工作信息", 0.2);
        memory.add("user1", "重要的工作信息", 0.9);

        List<MemoryItem> result = memory.search("user1", "工作信息", 5);
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("重要的工作信息", result.get(0).getContent());
    }

    @Test
    void shouldUpdateMemoryById() {
        MemoryItem added = memory.add("user1", "原始内容", 0.5);
        MemoryItem updated = memory.update("user1", added.getId(), "更新后的内容", 0.9);

        Assertions.assertEquals(added.getId(), updated.getId());
        Assertions.assertEquals("更新后的内容", updated.getContent());
        Assertions.assertEquals(0.9, updated.getImportance(), 0.001);
        Assertions.assertEquals(1, memory.list("user1").size());
    }

    @Test
    void shouldIsolateMemoriesByUser() {
        memory.add("userA", "用户 A 的记忆", 0.5);

        Assertions.assertTrue(memory.list("userB").isEmpty());
        Assertions.assertTrue(memory.search("userB", "记忆", 5).isEmpty());
    }

    @Test
    void shouldPersistAcrossInstances() {
        memory.add("user1", "持久化的记忆", 0.7);

        // 重新创建实例模拟应用重启，数据应从文件恢复。
        Memory reloaded = new JsonMemoryStrategy(tempDir.toString());
        Assertions.assertEquals(1, reloaded.list("user1").size());
    }
}
