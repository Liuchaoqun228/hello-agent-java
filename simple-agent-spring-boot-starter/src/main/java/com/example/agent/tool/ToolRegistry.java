package com.example.agent.tool;

import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ToolRegistry {

    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public ToolRegistry() {
    }

    public ToolRegistry(List<Tool> tools) {
        if (tools != null) {
            for (Tool tool : tools) {
                register(tool);
            }
        }
    }

    public synchronized void register(Tool tool) {
        Assert.notNull(tool, "tool must not be null");
        Assert.isTrue(!tools.containsKey(tool.getName()),
                "tool name already registered: " + tool.getName());
        tools.put(tool.getName(), tool);
    }

    public synchronized List<Tool> getTools() {
        return Collections.unmodifiableList(new ArrayList<>(tools.values()));
    }

    public String execute(String name, Map<String, Object> arguments) {
        Assert.hasText(name, "tool name must not be empty");

        Tool tool;
        synchronized (this) {
            tool = tools.get(name);
        }
        Assert.notNull(tool, "tool not registered: " + name);
        return tool.execute(arguments == null ? Collections.emptyMap() : arguments);
    }
}
