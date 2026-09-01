package com.example.agent.memory;

import com.example.agent.memory.dto.MemoryItem;

import java.util.List;

// 记忆存储的统一接口：默认实现为 JsonMemoryStrategy，用户提供自定义 Bean 时自动替换。
public interface Memory {

    MemoryItem add(String userId, String content, Double importance);

    List<MemoryItem> search(String userId, String query, int limit);

    List<MemoryItem> list(String userId);

    MemoryItem update(String userId, String id, String content, Double importance);
}
