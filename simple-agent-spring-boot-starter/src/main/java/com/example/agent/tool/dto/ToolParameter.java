package com.example.agent.tool.dto;

public final class ToolParameter {

    private final String name;
    private final String type;

    public ToolParameter(String name, String type) {
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
