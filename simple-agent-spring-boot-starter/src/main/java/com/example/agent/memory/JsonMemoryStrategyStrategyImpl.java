package com.example.agent.memory;

import com.example.agent.memory.dto.MemoryItem;
import com.example.agent.util.JsonFileUtil;
import com.example.agent.util.MemoryItemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 默认记忆实现：每个用户一个 JSON 文件，按关键词相关度、重要性和时间排序检索。
 */
public class JsonMemoryStrategyStrategyImpl implements MemoryStrategy {

    private static final Logger log = LoggerFactory.getLogger(JsonMemoryStrategyStrategyImpl.class);

    private final Path storagePath;

    /**
     * 使用指定目录存储用户记忆文件。
     */
    public JsonMemoryStrategyStrategyImpl(String storagePath) {
        Assert.hasText(storagePath, "storagePath must not be empty");
        this.storagePath = Paths.get(storagePath);
    }

    /**
     * 新增一条用户记忆并持久化。
     */
    @Override
    public synchronized MemoryItem add(String userId, String content, Double importance) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.hasText(content, "content must not be empty");

        // 找到记忆缓存文件位置
        // 读取现有记忆并补齐新记录的元数据后落盘。
        Path filePath = JsonFileUtil.fileFor(storagePath, "memory-", userId);
        List<MemoryItem> items = JsonFileUtil.readList(filePath, MemoryItem.class);
        MemoryItem item = new MemoryItem();
        item.setId(UUID.randomUUID().toString());
        item.setContent(content);
        item.setImportance(MemoryItemUtil.normalizeImportance(importance));
        long now = System.currentTimeMillis();
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        items.add(item);
        JsonFileUtil.write(filePath, items);
        log.info("新增记忆：userId={}, id={}, content={}", userId, item.getId(), item.getContent());
        return item;
    }

    /**
     * 检索用户记忆并返回相关度最高的记录。
     */
    @Override
    public synchronized List<MemoryItem> search(String userId, String query, int limit) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.hasText(query, "query must not be empty");
        if (limit <= 0) {
            return new ArrayList<>();
        }

        // 筛选关键词有命中的记忆记录。
        Path filePath = JsonFileUtil.fileFor(storagePath, "memory-", userId);
        List<MemoryItem> result = new ArrayList<>();
        for (MemoryItem item : JsonFileUtil.readList(filePath, MemoryItem.class)) {
            if (MemoryItemUtil.score(item.getContent(), query) > 0) {
                result.add(item);
            }
        }
        // 优先按关键词相关度排序，再按重要性和更新时间取优。
        result.sort(Comparator.comparingInt((MemoryItem item) -> MemoryItemUtil.score(item.getContent(), query)).reversed()
                .thenComparing(Comparator.comparingDouble(MemoryItem::getImportance).reversed())
                .thenComparing(Comparator.comparingLong(MemoryItem::getUpdatedAt).reversed()));
        return limit >= result.size() ? result : new ArrayList<>(result.subList(0, limit));
    }

    /**
     * 获取用户全部记忆并按更新时间倒序排列。
     */
    @Override
    public synchronized List<MemoryItem> list(String userId) {
        Assert.hasText(userId, "userId must not be empty");
        // 加载用户记忆并按最近更新时间排序。
        Path filePath = JsonFileUtil.fileFor(storagePath, "memory-", userId);
        List<MemoryItem> items = JsonFileUtil.readList(filePath, MemoryItem.class);
        items.sort(Comparator.comparingLong(MemoryItem::getUpdatedAt).reversed());
        return items;
    }

    /**
     * 更新指定记忆的内容和可选重要性。
     */
    @Override
    public synchronized MemoryItem update(String userId, String id, String content, Double importance) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.hasText(id, "id must not be empty");
        Assert.hasText(content, "content must not be empty");

        // 查找目标记忆，更新内容和可选重要性后写回文件。
        Path filePath = JsonFileUtil.fileFor(storagePath, "memory-", userId);
        List<MemoryItem> items = JsonFileUtil.readList(filePath, MemoryItem.class);
        for (MemoryItem item : items) {
            if (item.getId().equals(id)) {
                item.setContent(content);
                if (importance != null) {
                    item.setImportance(MemoryItemUtil.normalizeImportance(importance));
                }
                item.setUpdatedAt(System.currentTimeMillis());
                JsonFileUtil.write(filePath, items);
                log.info("更新记忆：userId={}, id={}, content={}", userId, id, content);
                return item;
            }
        }
        throw new IllegalArgumentException("memory not found: " + id);
    }

}
