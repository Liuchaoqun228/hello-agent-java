package com.example.agent;

import com.example.agent.memory.JsonMemoryStrategyStrategyImpl;
import com.example.agent.memory.MemoryStrategy;
import com.example.agent.memory.dto.MemoryItem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

class JsonMemoryStrategyStrategyImplTest {

    @TempDir
    Path tempDir;

    private MemoryStrategy memoryStrategy;

    @BeforeEach
    void setUp() {
        memoryStrategy = new JsonMemoryStrategyStrategyImpl(tempDir.toString());
    }

    @Test
    void shouldAddAndSearchMemory() {
        memoryStrategy.add("user1", "用户是 Java 开发者", 0.9);

        List<MemoryItem> result = memoryStrategy.search("user1", "开发者", 5);
        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(result.get(0).getContent().contains("Java"));
    }

    @Test
    void shouldSortSearchResultByImportance() {
        memoryStrategy.add("user1", "不重要的工作信息", 0.2);
        memoryStrategy.add("user1", "重要的工作信息", 0.9);

        List<MemoryItem> result = memoryStrategy.search("user1", "工作信息", 5);
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("重要的工作信息", result.get(0).getContent());
    }

    @Test
    void shouldUpdateMemoryById() {
        MemoryItem added = memoryStrategy.add("user1", "原始内容", 0.5);
        MemoryItem updated = memoryStrategy.update("user1", added.getId(), "更新后的内容", 0.9);

        Assertions.assertEquals(added.getId(), updated.getId());
        Assertions.assertEquals("更新后的内容", updated.getContent());
        Assertions.assertEquals(0.9, updated.getImportance(), 0.001);
        Assertions.assertEquals(1, memoryStrategy.list("user1").size());
    }

    @Test
    void shouldIsolateMemoriesByUser() {
        memoryStrategy.add("userA", "用户 A 的记忆", 0.5);

        Assertions.assertTrue(memoryStrategy.list("userB").isEmpty());
        Assertions.assertTrue(memoryStrategy.search("userB", "记忆", 5).isEmpty());
    }

    @Test
    void shouldPersistAcrossInstances() {
        memoryStrategy.add("user1", "持久化的记忆", 0.7);

        // 重新创建实例模拟应用重启，数据应从文件恢复。
        MemoryStrategy reloaded = new JsonMemoryStrategyStrategyImpl(tempDir.toString());
        Assertions.assertEquals(1, reloaded.list("user1").size());
    }
}
