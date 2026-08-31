package com.example.agent.tool;

import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;

public abstract class Tool {

    private final String name;
    private final String description;

    protected Tool(String name, String description) {
        Assert.hasText(name, "tool name must not be empty");
        Assert.hasText(description, "tool description must not be empty");
        this.name = name;
        this.description = description;
    }

    public final String getName() {
        return name;
    }

    public final String getDescription() {
        return description;
    }

    public abstract List<ToolParameter> getParameters();

    public abstract String execute(Map<String, Object> arguments);
}
