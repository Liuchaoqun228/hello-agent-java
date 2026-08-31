package com.example.agent.util;

public final class ToolTypeUtil {

    private ToolTypeUtil() {
    }

    // 将 Java 参数类型转换为模型工具 Schema 使用的 JSON 类型。
    public static String toJsonType(Class<?> type) {
        if (type == String.class || type == Character.class || type == Character.TYPE || type.isEnum()) {
            return "string";
        }
        if (type == Boolean.class || type == Boolean.TYPE) {
            return "boolean";
        }
        if (type == Byte.class || type == Byte.TYPE || type == Short.class || type == Short.TYPE
                || type == Integer.class || type == Integer.TYPE || type == Long.class || type == Long.TYPE) {
            return "integer";
        }
        return "number";
    }
}
