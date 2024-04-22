package com.example.capybaramess;

public class Contact {
    private String name;
    private int profileImage; // Resource ID of the drawable

    public Contact(String name, int profileImage) {
        this.name = name;
        this.profileImage = profileImage;
    }

    public String getName() {
        return name;
    }

    public int getProfileImage() {
        return profileImage;
    }
}
