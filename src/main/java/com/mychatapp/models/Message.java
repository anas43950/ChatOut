package com.mychatapp.models;


public class Message {

    private String userID;
    private String message;
    private String timestamp;
    private String imageUrl;

    public Message(String message, String userID, String timestamp, String imageUrl) {
        this.message = message;
        this.userID = userID;
        this.timestamp = timestamp;
        this.imageUrl=imageUrl;
    }

    public Message() {

    }

    public String getMessage() {
        return message;
    }

    public String getUserID() {
        return userID;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

}
