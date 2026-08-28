package com.example.agent.message;

import java.util.Map;

public class Message {

    public Message() {
    }

    public Message(String content, MessageRoleEnum messageRole) {
        this.content = content;
        this.messageRole = messageRole;
        this.timestamp = System.currentTimeMillis();
    }

    private String content;
    private MessageRoleEnum messageRole;
    private Long timestamp;
    private Map<String, Object> meta;



    public MessageRoleEnum getMessageRole() {
        return messageRole;
    }

    public void setMessageRole(MessageRoleEnum messageRole) {
        this.messageRole = messageRole;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, Object> meta) {
        this.meta = meta;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
