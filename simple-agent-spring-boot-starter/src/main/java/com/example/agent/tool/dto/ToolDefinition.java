package com.example.agent.tool.dto;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ToolDefinition {

    private final String name;
    private final String description;
    private final List<ToolParameter> parameters;
    private final Object target;
    private final Method method;

    public ToolDefinition(String name, String description, List<ToolParameter> parameters, Object target, Method method) {
        // 工具定义同时保存模型 Schema 与实际调用目标，注册后不可变。
        this.name = name;
        this.description = description;
        this.parameters = Collections.unmodifiableList(new ArrayList<>(parameters));
        this.target = target;
        this.method = method;
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

    public Object getTarget() {
        return target;
    }

    public Method getMethod() {
        return method;
    }
}
