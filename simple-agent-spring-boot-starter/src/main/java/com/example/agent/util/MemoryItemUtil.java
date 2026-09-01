package com.example.agent.util;

/**
 * 记忆记录的通用计算工具：集中维护相关度和重要性规则。
 */
public final class MemoryItemUtil {

    /**
     * 禁止实例化工具类。
     */
    private MemoryItemUtil() {
    }

    /**
     * 按关键词字符命中和完整包含关系计算内容相关度。
     */
    public static int score(String content, String query) {
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

    /**
     * 将重要性限制在 0 到 1 之间，缺省值为 0.5。
     */
    public static double normalizeImportance(Double importance) {
        if (importance == null) {
            return 0.5;
        }
        return Math.max(0.0, Math.min(1.0, importance));
    }
}
