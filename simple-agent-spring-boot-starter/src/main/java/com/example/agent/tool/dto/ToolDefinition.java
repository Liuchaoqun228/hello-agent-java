package com.example.agent.tool.dto;

import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ToolDefinition {

    private final String name;
    private final String description;
    private final List<ToolParameter> parameters;

    public ToolDefinition(String name, String description, List<ToolParameter> parameters) {
        Assert.hasText(name, "tool name must not be empty");
        Assert.hasText(description, "tool description must not be empty");
        Assert.notNull(parameters, "tool parameters must not be null");

        // 注册完成后冻结工具定义，避免请求期间的 Schema 被外部修改。
        this.name = name;
        this.description = description;
        this.parameters = Collections.unmodifiableList(new ArrayList<>(parameters));
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<ToolParameter> getParameters() {
        return parameters;
    }
}
