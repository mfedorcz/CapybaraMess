package com.example.capybaramess;

public class Contact {
    private String name;
    private String snippet;
    private String profileImage;
    private long timestamp;
    private int type;
    private long threadId;
    private String address;
    private boolean isRegistered;
    public Contact(String name, String snippet, String profileImage, long timestamp, int type, long threadId, String address, boolean isRegistered) {
        this.name = name;
        this.snippet = snippet;
        this.profileImage = profileImage;
        this.timestamp = timestamp;
        this.type = type;
        this.threadId = threadId;
        this.address = address;
        this.isRegistered = isRegistered;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }
    public String getSnippet() {
        return snippet;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getDate() {
        return timestamp;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public int getType(){
        return type;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    public long getThreadId() {
        return threadId;
    }

    public String getAddress() {
        return address;
    }

    public boolean isRegistered() {
        return isRegistered;
    }

    public void setRegistered(boolean registered) {
        isRegistered = registered;
    }
}
