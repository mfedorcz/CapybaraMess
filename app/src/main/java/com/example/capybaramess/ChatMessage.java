package com.example.capybaramess;

public class ChatMessage {
    private String id;  // Unique identifier for the message
    private String sender;  // Identifier of the sender (could be phone number or user ID)
    private String content;  // Text content of the message
    private long timestamp;  // Timestamp for when the message was sent
    private MessageType type;  // Type of message (incoming or outgoing)

    // Enum to distinguish between incoming and outgoing messages
    public enum MessageType {
        INCOMING, OUTGOING
    }

    // Constructor
    public ChatMessage(String id, String sender, String content, long timestamp, MessageType type) {
        this.id = id;
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
        this.type = type;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public MessageType getType() {
        return type;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setType(MessageType type) {
        this.type = type;
    }
}
