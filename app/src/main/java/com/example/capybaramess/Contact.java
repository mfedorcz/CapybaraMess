package com.example.capybaramess;

import android.os.Parcel;
import android.os.Parcelable;

public class Contact implements Parcelable {
    private String name;
    private String snippet;
    private String profileImage;
    private long timestamp;
    private int type;
    private long threadId;
    private String address;
    private boolean isRegistered;
    private String lastMessage;

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

    protected Contact(Parcel in) {
        name = in.readString();
        snippet = in.readString();
        profileImage = in.readString();
        timestamp = in.readLong();
        type = in.readInt();
        threadId = in.readLong();
        address = in.readString();
        isRegistered = in.readByte() != 0;
    }

    public static final Creator<Contact> CREATOR = new Creator<Contact>() {
        @Override
        public Contact createFromParcel(Parcel in) {
            return new Contact(in);
        }

        @Override
        public Contact[] newArray(int size) {
            return new Contact[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(snippet);
        dest.writeString(profileImage);
        dest.writeLong(timestamp);
        dest.writeInt(type);
        dest.writeLong(threadId);
        dest.writeString(address);
        dest.writeByte((byte) (isRegistered ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Getters and Setters...

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getType() {
        return type;
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

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
}
