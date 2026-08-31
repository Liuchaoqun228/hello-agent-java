package com.example.agent.message;

import com.example.agent.tool.dto.ToolCall;

import java.util.List;
import java.util.Map;

public class Message {

    private String content;
    private MessageRoleEnum messageRole;
    private Long timestamp;
    private Map<String, Object> meta;
    private List<ToolCall> toolCallList;
    private String toolCallId;

    public Message() {
    }

    public Message(String content, MessageRoleEnum messageRole) {
        this.content = content;
        this.messageRole = messageRole;
        this.timestamp = System.currentTimeMillis();
    }

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

    public List<ToolCall> getToolCallList() {
        return toolCallList;
    }

    public void setToolCallList(List<ToolCall> toolCallList) {
        this.toolCallList = toolCallList;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }
}
