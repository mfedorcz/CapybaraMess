package com.example.capybaramess;

public class Contact {
    private String name;
    private String snippet;
    private int profileImage; // Resource ID of the drawable

    public Contact(String name, String snippet, int profileImage) {
        this.name = name;
        this.snippet = snippet;
        this.profileImage = profileImage;
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

    public int getImageResource() {
        return profileImage;
    }
}
