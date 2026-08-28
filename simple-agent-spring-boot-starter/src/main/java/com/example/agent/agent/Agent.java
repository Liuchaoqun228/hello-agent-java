package com.example.agent.agent;

import com.openai.models.chat.completions.ChatCompletion;

public interface Agent {

    ChatCompletion run(String input);
}
