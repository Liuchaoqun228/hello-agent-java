package com.example.agent.memory;

import com.example.agent.memory.dto.MemoryItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

// 默认记忆实现：每个用户一个 JSON 文件，按关键词相关度、重要性和时间排序检索。
public class JsonMemoryStrategyStrategyImpl implements MemoryStrategy {

    private static final Logger log = LoggerFactory.getLogger(JsonMemoryStrategyStrategyImpl.class);

    private static final TypeReference<List<MemoryItem>> ITEM_LIST_TYPE = new TypeReference<List<MemoryItem>>() {
    };

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path storagePath;

    public JsonMemoryStrategyStrategyImpl(String storagePath) {
        Assert.hasText(storagePath, "storagePath must not be empty");
        this.storagePath = Paths.get(storagePath);
    }

    @Override
    public synchronized MemoryItem add(String userId, String content, Double importance) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.hasText(content, "content must not be empty");

        List<MemoryItem> items = readAll(userId);
        MemoryItem item = new MemoryItem();
        item.setId(UUID.randomUUID().toString());
        item.setContent(content);
        item.setImportance(normalizeImportance(importance));
        long now = System.currentTimeMillis();
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        items.add(item);
        writeAll(userId, items);
        log.info("新增记忆：userId={}, id={}, content={}", userId, item.getId(), item.getContent());
        return item;
    }

    @Override
    public synchronized List<MemoryItem> search(String userId, String query, int limit) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.hasText(query, "query must not be empty");
        if (limit <= 0) {
            return new ArrayList<>();
        }

        List<MemoryItem> result = new ArrayList<>();
        for (MemoryItem item : readAll(userId)) {
            if (score(item.getContent(), query) > 0) {
                result.add(item);
            }
        }
        // 优先按关键词相关度排序，再按重要性和更新时间取优。
        result.sort(Comparator.comparingInt((MemoryItem item) -> score(item.getContent(), query)).reversed()
                .thenComparing(Comparator.comparingDouble(MemoryItem::getImportance).reversed())
                .thenComparing(Comparator.comparingLong(MemoryItem::getUpdatedAt).reversed()));
        return limit >= result.size() ? result : new ArrayList<>(result.subList(0, limit));
    }

    @Override
    public synchronized List<MemoryItem> list(String userId) {
        Assert.hasText(userId, "userId must not be empty");
        List<MemoryItem> items = readAll(userId);
        items.sort(Comparator.comparingLong(MemoryItem::getUpdatedAt).reversed());
        return items;
    }

    @Override
    public synchronized MemoryItem update(String userId, String id, String content, Double importance) {
        Assert.hasText(userId, "userId must not be empty");
        Assert.hasText(id, "id must not be empty");
        Assert.hasText(content, "content must not be empty");

        List<MemoryItem> items = readAll(userId);
        for (MemoryItem item : items) {
            if (item.getId().equals(id)) {
                item.setContent(content);
                if (importance != null) {
                    item.setImportance(normalizeImportance(importance));
                }
                item.setUpdatedAt(System.currentTimeMillis());
                writeAll(userId, items);
                log.info("更新记忆：userId={}, id={}, content={}", userId, id, content);
                return item;
            }
        }
        throw new IllegalArgumentException("memory not found: " + id);
    }

    // 用户 id 编码后拼入文件名，避免特殊字符造成路径穿越或非法文件名。
    private Path fileFor(String userId) {
        try {
            return storagePath.resolve("memory-" + URLEncoder.encode(userId, "UTF-8") + ".json");
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException("cannot encode userId: " + userId, exception);
        }
    }

    private List<MemoryItem> readAll(String userId) {
        Path file = fileFor(userId);
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try {
            List<MemoryItem> items = objectMapper.readValue(file.toFile(), ITEM_LIST_TYPE);
            return items == null ? new ArrayList<>() : items;
        } catch (IOException exception) {
            throw new IllegalStateException("cannot read memory file: " + file, exception);
        }
    }

    private void writeAll(String userId, List<MemoryItem> items) {
        Path file = fileFor(userId);
        try {
            Files.createDirectories(storagePath);
            objectMapper.writeValue(file.toFile(), items);
        } catch (IOException exception) {
            throw new IllegalStateException("cannot write memory file: " + file, exception);
        }
    }

    // 关键词命中计分：query 中每个字符出现在内容里得一分，内容整体包含 query 再加分。
    private int score(String content, String query) {
        int result = 0;
        for (char character : query.toCharArray()) {
            if (content.indexOf(character) >= 0) {
                result++;
            }
        }
        if (content.contains(query)) {
            result += query.length();
        }
        return result;
    }

    // 重要性缺省为 0.5，并限制在 0-1 区间。
    private double normalizeImportance(Double importance) {
        if (importance == null) {
            return 0.5;
        }
        return Math.max(0.0, Math.min(1.0, importance));
    }
}
