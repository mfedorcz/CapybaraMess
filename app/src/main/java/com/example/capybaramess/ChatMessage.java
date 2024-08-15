package com.example.capybaramess;

import java.util.Objects;

public class ChatMessage {
    private String senderId;  // User ID of the sender
    private String recipientId;  // User ID of the recipient
    private String content;  // Text content of the message
    private long timestamp;  // Timestamp for when the message was sent
    private MessageType type;
    // Enum to distinguish between incoming and outgoing messages
    public enum MessageType {
        INCOMING, OUTGOING
    }
    // Constructor
    public ChatMessage(String senderId, String recipientId, String content, long timestamp, MessageType type) {
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.content = content;
        this.timestamp = timestamp;
        this.type = type;
    }

    // Getters and Setters
    public String getSenderId() {
        return senderId;
    }
    public void setType(MessageType type) {
        this.type = type;
    }
    public MessageType getType() {
        return type;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessage that = (ChatMessage) o;
        return timestamp == that.timestamp &&
                senderId.equals(that.senderId) &&
                content.equals(that.content);
    }
    @Override
    public int hashCode() {
        return Objects.hash(senderId, content, timestamp);
    }

}
