package com.chatout.contactlistdata;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.chatout.models.Contact;

@Database(entities = {Contact.class},version = 1,exportSchema = false)
public abstract class ContactListDatabase extends RoomDatabase {
    private static final String TAG = ContactListDatabase.class.getSimpleName();
    public static final String DATABASE_NAME="contactlist";
    private static ContactListDatabase sInstance;
    private static final Object LOCK=new Object();
    public abstract ContactsDao contactsDao();

    public static ContactListDatabase getInstance(Context context){
        if(sInstance==null){
            synchronized (LOCK){
                sInstance= Room.
                        databaseBuilder(context.getApplicationContext(),
                                ContactListDatabase.class,DATABASE_NAME).
                        build();
            }
        }
        return sInstance;
    }
}
