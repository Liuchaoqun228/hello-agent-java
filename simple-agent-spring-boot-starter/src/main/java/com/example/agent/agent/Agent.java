package com.example.agent.agent;


public interface Agent {

    String chat(String input);

    String chat(String input, ChatOptions config);
}
