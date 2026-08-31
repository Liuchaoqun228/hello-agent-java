package com.example.agent.tool.dto;

import org.springframework.util.Assert;

public final class ToolParameter {

    private final String name;
    private final String type;

    public ToolParameter(String name, String type) {
        Assert.hasText(name, "tool parameter name must not be empty");
        Assert.hasText(type, "tool parameter type must not be empty");
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
}
