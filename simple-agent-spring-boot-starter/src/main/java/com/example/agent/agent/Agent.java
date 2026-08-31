package com.example.agent.agent;


import com.example.agent.tool.ToolManager;

public interface Agent {

    String chat(String input);

    String chat(String input, ChatOptions config);

    void setToolManager(ToolManager toolManager);

}
