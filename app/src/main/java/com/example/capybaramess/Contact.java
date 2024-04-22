package com.example.capybaramess;

public class Contact {
    private String name;
    private String snippet;
    private int profileImage; // Resource ID of the drawable
    private long timestamp;

    private int type;

    public Contact(String name, String snippet, int profileImage, long timestamp, int type) {
        this.name = name;
        this.snippet = snippet;
        this.profileImage = profileImage;
        this.timestamp = timestamp;
        this.type = type;
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

    public long getDate() {
        return timestamp;
    }

    public int getImageResource() {
        return profileImage;
    }

    public int getType(){
        return type;
    }
}
