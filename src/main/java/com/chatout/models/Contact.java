package com.chatout.models;


import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "contacts")
public class Contact {
    @ColumnInfo(name = "name")
    private String mName;
    @ColumnInfo(name = "username")
    private String mUserName;
    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "id")
    private String mUserID;

    @NonNull
    public String getMUserID() {
        return mUserID;
    }

    public void setMUserID(@NonNull String mUserID) {
        this.mUserID = mUserID;
    }







    public String getName() {
        return mName;
    }

    public String getUserName() {
        return mUserName;
    }

    public Contact(String name, String username, String userID) {
        mName = name;
        mUserName = username;
        mUserID = userID;
    }

    public Contact() {
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public void setUserName(String mUserName) {
        this.mUserName = mUserName;
    }


}
