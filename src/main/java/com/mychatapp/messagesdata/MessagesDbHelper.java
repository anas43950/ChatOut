package com.mychatapp.messagesdata;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.mychatapp.messagesdata.MessageContract.MessageDetails;


public class MessagesDbHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME="messages.db";
    public static final int DATABASE_VERSION=1;
    private static final String TAG = MessagesDbHelper.class.getSimpleName();
    private String receiverUID;
    public MessagesDbHelper(Context context,String receiverUID) {
        super(context,DATABASE_NAME,null,DATABASE_VERSION);
        this.receiverUID =receiverUID;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String SQL_CREATE_MESSAGES_TABLE="CREATE TABLE receiver"+receiverUID+" ("+MessageDetails.TIMESTAMP_ID
                +" INTEGER PRIMARY KEY, "+ MessageDetails.COLUMN_MESSAGE+" TEXT,"+MessageDetails.COLUMN_IMAGE_URI+" TEXT, "
                +MessageDetails.COLUMN_SENDER_UID+" TEXT);";
        db.execSQL(SQL_CREATE_MESSAGES_TABLE);
        Log.d(TAG, "onCreate: Database created");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}
