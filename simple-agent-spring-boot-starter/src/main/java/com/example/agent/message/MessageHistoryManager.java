package com.example.agent.message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MessageHistoryManager {

    private final List<Message> history = new ArrayList<>();

    public synchronized void add(Message message) {
        Objects.requireNonNull(message, "message must not be null");
        history.add(message);
    }

    public synchronized List<Message> getHistory() {
        return Collections.unmodifiableList(new ArrayList<>(history));
    }

    public synchronized void clear() {
        history.clear();
    }
}
