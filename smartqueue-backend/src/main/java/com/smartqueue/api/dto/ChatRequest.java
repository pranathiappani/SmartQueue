package com.smartqueue.api.dto;

import java.util.UUID;

public class ChatRequest {
    private String message;
    private UUID queueId;

    public ChatRequest() {}

    public ChatRequest(String message, UUID queueId) {
        this.message = message;
        this.queueId = queueId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UUID getQueueId() {
        return queueId;
    }

    public void setQueueId(UUID queueId) {
        this.queueId = queueId;
    }
}
