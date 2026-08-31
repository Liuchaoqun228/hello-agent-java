package com.example.agent.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MessageHistoryManager {

    private final List<Message> history = new ArrayList<>();

    public synchronized void add(Message message) {
        Objects.requireNonNull(message, "message must not be null");
        history.add(message);
    }

    public synchronized void addAll(List<Message> messages) {
        Objects.requireNonNull(messages, "messages must not be null");
        for (Message message : messages) {
            Objects.requireNonNull(message, "message must not be null");
        }
        history.addAll(messages);
    }

    public synchronized boolean remove(Message message) {
        return history.remove(message);
    }

    public synchronized boolean isEmpty() {
        return history.isEmpty();
    }

    public synchronized List<Message> getHistory() {
        return new ArrayList<>(history);
    }

    public synchronized void clear() {
        history.clear();
    }
}
