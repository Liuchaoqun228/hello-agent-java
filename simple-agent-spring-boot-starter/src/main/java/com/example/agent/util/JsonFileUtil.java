package com.example.agent.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON 文件通用读写工具：负责安全生成文件路径、列表反序列化和对象落盘。
 */
public final class JsonFileUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 禁止实例化工具类。
     */
    private JsonFileUtil() {
    }

    /**
     * 根据目录、前缀和标识生成安全的 JSON 文件路径。
     */
    public static Path fileFor(Path directory, String filePrefix, String identifier) {
        try {
            return directory.resolve(filePrefix + URLEncoder.encode(identifier, "UTF-8") + ".json");
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException("cannot encode identifier: " + identifier, exception);
        }
    }

    /**
     * 读取 JSON 文件中的列表数据，文件不存在或内容为空时返回空列表。
     */
    public static <T> List<T> readList(Path file, Class<T> itemType) {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try {
            List<T> items = OBJECT_MAPPER.readValue(file.toFile(),
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, itemType));
            return items == null ? new ArrayList<>() : items;
        } catch (IOException exception) {
            throw new IllegalStateException("cannot read JSON file: " + file, exception);
        }
    }

    /**
     * 将对象序列化写入指定文件，父目录不存在时自动创建。
     */
    public static void write(Path file, Object value) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            OBJECT_MAPPER.writeValue(file.toFile(), value);
        } catch (IOException exception) {
            throw new IllegalStateException("cannot write JSON file: " + file, exception);
        }
    }
}
