package com.example.agent.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

// 记忆模块配置：默认使用每用户一个 JSON 文件的本地存储。
@ConfigurationProperties(prefix = MemoryProperties.PREFIX)
public class MemoryProperties {

    public static final String PREFIX = "simple.agent.memory";

    private String storagePath = "./data/memory/";

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }
}
