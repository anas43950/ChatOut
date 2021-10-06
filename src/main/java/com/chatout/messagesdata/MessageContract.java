package com.chatout.messagesdata;

import android.provider.BaseColumns;

public class MessageContract {

    private MessageContract() {
    }

    public static final class MessageDetails implements BaseColumns {
        public static final String TIMESTAMP_ID=BaseColumns._ID;
        public static final String COLUMN_MESSAGE="message";
        public static final String COLUMN_IMAGE_URI="imageUri";
        public static final String COLUMN_SENDER_UID="senderUID";
    }
}
