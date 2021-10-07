package com.chatout;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.chatout.dialogs.DeleteDialog;
import com.chatout.dialogs.ImagePreviewDialog;
import com.chatout.messagesdata.MessageContract.MessageDetails;
import com.chatout.messagesdata.MessagesDbHelper;
import com.chatout.models.Message;
import com.chatout.recyclerviewutils.ContactsAdapter;
import com.chatout.recyclerviewutils.MessagesAdapter;

import java.io.File;
import java.util.ArrayList;


public class ChatActivity extends AppCompatActivity implements MessagesAdapter.ClickListener, DeleteDialog.Listener {
    // Firebase instance variables
    private FirebaseDatabase mDatabase;
    private DatabaseReference mMessagesDatabaseReference, mSenderContactsReference, mReceiverContactsReference;
    private ValueEventListener addContactSingleListener, loadAllMessagesSingleListener;
    private FirebaseAuth mFirebaseAuth;
    public String currentUserUID, receiverUID, receiverName;
    private static final String TAG = ChatActivity.class.getSimpleName();
    public static final String IMAGE_PATH_KEY = "image-path-key";
    public static final String IMAGE_URI_KEY = "image-uri-key";
    public static final String RECEIVER_UID_KEY = "image-uri-key";
    private String path;
    private long messageTimestamp;
    private MessagesDbHelper messagesDbHelper;
    private ArrayList<Message> messages;
    private MessagesAdapter messagesAdapter;
    private ChildEventListener childEventListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Intent intentThatLaunchedActivity = getIntent();
        receiverUID = intentThatLaunchedActivity.getStringExtra(ContactsAdapter.receiverUIDKey);
        receiverName = intentThatLaunchedActivity.getStringExtra(ContactsAdapter.nameKey);
        ((TextView) findViewById(R.id.chat_name_tv)).setText(receiverName);

        //Initializing firebase variables
        mDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        currentUserUID = mFirebaseAuth.getCurrentUser().getUid();
        mMessagesDatabaseReference = mDatabase.getReference("messages");
        mSenderContactsReference = mDatabase.getReference("contactList").child(currentUserUID).child(receiverUID);
        mReceiverContactsReference = mDatabase.getReference("contactList").child(receiverUID).child(currentUserUID);

        RecyclerView messagesRV = findViewById(R.id.messagesRV);
        ImageButton sendButton = findViewById(R.id.send_button);
        ImageButton imagePickerButton = findViewById(R.id.image_picker_button);
        sendButton.setEnabled(false);

        EditText messageEditText = findViewById(R.id.messageEditText);
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this);
        messagesRV.getRecycledViewPool().setMaxRecycledViews(0, 0);


        messagesAdapter = new MessagesAdapter(this, receiverUID, mLinearLayoutManager);
        messagesAdapter.setClickListener(this);
        messages = loadAllMessagesOfThisChat();
        messagesAdapter.setMessages(messages);
        messagesRV.setAdapter(messagesAdapter);
        messagesRV.setLayoutManager(mLinearLayoutManager);

        //On layout change listener for scrolling to bottom after keyboard popup
        messagesRV.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) {
                messagesRV.post(() -> {
                    if (messagesAdapter.getItemCount() != 0) {
                        messagesRV.scrollToPosition(messagesAdapter.getItemCount() - 1);
                    }
                    imagePickerButton.setVisibility(View.GONE);
                });
            } else if (bottom == oldBottom) {
                imagePickerButton.setVisibility(View.VISIBLE);
            }
        });


        //setting activity result launcher for image picker button
        ActivityResultLauncher<Intent> getImageUri = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == RESULT_OK) {
                    Uri uri = result.getData().getData();
                    path = null;
                    try {
                        File image_file = FileUtils.getFileFromUri(ChatActivity.this, uri);
                        path = image_file.getAbsolutePath();
                        Bundle b = new Bundle();
                        b.putString(IMAGE_URI_KEY, uri.toString());
                        b.putString(IMAGE_PATH_KEY, path);
                        b.putString(RECEIVER_UID_KEY, receiverUID);
                        ImagePreviewDialog imagePreviewDialog = new ImagePreviewDialog();
                        FragmentManager fragmentManager = getSupportFragmentManager();
                        imagePreviewDialog.setArguments(b);
                        imagePreviewDialog.show(fragmentManager, "ImagePreview");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });


        //setting up menu
        findViewById(R.id.chat_activity_back_button).setOnClickListener(l -> finish());
        imagePickerButton.setOnClickListener(l -> {
            if (isNetworkConnected()) {
                Intent intentGalley = new Intent(Intent.ACTION_PICK);
                intentGalley.setType("image/*");
                if (isStoragePermissionGranted()) {
                    getImageUri.launch(intentGalley);
                }
            } else {
                Toast.makeText(this, R.string.no_internet, Toast.LENGTH_SHORT).show();
            }
        });
        ImageButton chatMenu = findViewById(R.id.chat_activity_menu_button);
        chatMenu.setOnClickListener(l -> {
            PopupMenu popupMenu = new PopupMenu(ChatActivity.this, chatMenu);
            popupMenu.getMenuInflater().inflate(R.menu.chat_activity_menu, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(item -> {
                int item_id = item.getItemId();
                if (item_id == R.id.delete_all_messages) {


                    mMessagesDatabaseReference.child(currentUserUID).child(receiverUID).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (!task.isSuccessful()) {
                                Toast.makeText(ChatActivity.this, R.string.delete_all_messages_failed, Toast.LENGTH_SHORT).show();
                            } else if (task.isSuccessful()) {
                                messagesAdapter.removeAllMessages();
                                deleteAllMessagesFromDatabase();
                            }
                        }
                    });
                    deleteAllMessagesFromDatabase();
                    return true;
                }
                return false;
            });
            popupMenu.show();
        });

        //Single value even listener for adding contact in contact list
        addContactSingleListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mSenderContactsReference.get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String contactUID = (String) task.getResult().getValue();
                        if (contactUID == null) {
                            mSenderContactsReference.setValue(receiverUID);
                            mReceiverContactsReference.setValue(currentUserUID);
                            mMessagesDatabaseReference.child(currentUserUID).child(receiverUID).removeEventListener(addContactSingleListener);
                        } else {
                            mMessagesDatabaseReference.child(currentUserUID).child(receiverUID).removeEventListener(addContactSingleListener);

                        }
                    }
                });

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        //Single event listener for downloading all previous messages of this chat
        loadAllMessagesSingleListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getChildrenCount() > 1 && getLatestMessageTimestampForThisChat().equals("0")) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        Log.d(TAG, "onDataChange: single event listener called");
                        Message message = ds.getValue(Message.class);
                        addMessageToDatabase(message);
                        messagesAdapter.addMessage(message);
                    }

                } else {

                    mMessagesDatabaseReference.child(currentUserUID).child(receiverUID).removeEventListener(loadAllMessagesSingleListener);
                    mMessagesDatabaseReference.child(currentUserUID).child(receiverUID)
                            .orderByKey()
                            .startAfter(String.valueOf(getLatestMessageTimestampForThisChat()))
                            .addChildEventListener(childEventListener);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };


        //Edit text change listener
        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sendButton.setEnabled(!s.toString().equals(""));

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        sendButton.setOnClickListener(v -> {
            if (!isNetworkConnected()) {
                Toast.makeText(ChatActivity.this, R.string.no_internet, Toast.LENGTH_SHORT).show();
                return;
            }
            long timestamp = System.currentTimeMillis();
            Message message = new Message(messageEditText.getText().toString(), currentUserUID, timestamp, null);
            messagesAdapter.addMessage(message);
            messageEditText.setText("");
            sendButton.setEnabled(false);

            mMessagesDatabaseReference.child(currentUserUID).child(receiverUID).child(String.valueOf(timestamp)).setValue(message).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        mMessagesDatabaseReference.child(currentUserUID).child(receiverUID).addValueEventListener(addContactSingleListener);
                        mMessagesDatabaseReference.child(receiverUID).child(currentUserUID).child(String.valueOf(timestamp)).setValue(message);
                    } else if (!task.isSuccessful()) {
                        Toast.makeText(ChatActivity.this, R.string.send_message_failed, Toast.LENGTH_SHORT).show();
                        messagesAdapter.removeMessage(timestamp);
                    }
                }
            });

        });
        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Message message = snapshot.getValue(Message.class);
                addMessageToDatabase(message);
                if (!message.getUserID().equals(currentUserUID)) {
                    messagesAdapter.addMessage(message);
                } else if (message.getImageUrl() != null) {
                    messagesAdapter.addMessage(message);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        mMessagesDatabaseReference.child(currentUserUID).child(receiverUID).addValueEventListener(loadAllMessagesSingleListener);
    }


    //Method to check whether the permission is granted or not
    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission is granted");
                return true;
            } else {

                Log.v(TAG, "Permission is revoked");
                askForStoragePermission();
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "Permission is granted");
            return true;
        }
    }

    //Method to ask for storage permissions
    private void askForStoragePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
    }

    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    private ArrayList<Message> loadAllMessagesOfThisChat() {

        ArrayList<Message> messages = new ArrayList<>();

        //Initializing local database
        messagesDbHelper = new MessagesDbHelper(this, receiverUID);
        SQLiteDatabase mDb = messagesDbHelper.getReadableDatabase();
        String[] projection = {
                MessageDetails.COLUMN_SENDER_UID,
                MessageDetails.COLUMN_MESSAGE,
                MessageDetails.TIMESTAMP_ID,
                MessageDetails.COLUMN_IMAGE_URI};
        Cursor cursor = mDb.query("receiver" + receiverUID, projection, null, null, null, null, null);
        int messageColumnIndex = cursor.getColumnIndex(MessageDetails.COLUMN_MESSAGE);
        int timestampColumnIndex = cursor.getColumnIndex(MessageDetails.TIMESTAMP_ID);
        int imageURIColumnIndex = cursor.getColumnIndex(MessageDetails.COLUMN_IMAGE_URI);
        int senderUIDColumnIndex = cursor.getColumnIndex(MessageDetails.COLUMN_SENDER_UID);
        try {
            while (cursor.moveToNext()) {
                Message message = new Message();
                message.setMessage(cursor.getString(messageColumnIndex));
                message.setImageUrl(cursor.getString(imageURIColumnIndex));
                message.setTimestamp(cursor.getLong(timestampColumnIndex));
                message.setUserID(cursor.getString(senderUIDColumnIndex));
                messages.add(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cursor.close();
            mDb.close();
        }

        return messages;
    }

    //Method to add message to database once it is added to firebase
    private void addMessageToDatabase(Message message) {
        SQLiteDatabase mDb = messagesDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(MessageDetails.TIMESTAMP_ID, message.getTimestamp());
        values.put(MessageDetails.COLUMN_MESSAGE, message.getMessage());
        values.put(MessageDetails.COLUMN_IMAGE_URI, message.getImageUrl());
        values.put(MessageDetails.COLUMN_SENDER_UID, message.getUserID());
        mDb.insert("receiver" + receiverUID, null, values);
        mDb.close();


    }


    private void deleteAllMessagesFromDatabase() {
        SQLiteDatabase mDb = messagesDbHelper.getWritableDatabase();
        mDb.delete("receiver" + receiverUID, null, null);
        mDb.close();
    }


    @Override
    public void onItemLongClick(int position) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        DeleteDialog deleteDialog = new DeleteDialog();
        deleteDialog.setListener(ChatActivity.this);
        deleteDialog.show(fragmentManager, "DeleteMessageDialog");
        messageTimestamp = messages.get(position).getTimestamp();

    }

    @Override
    public void onConfirmClicked(int val) {
        mMessagesDatabaseReference.child(currentUserUID).child(receiverUID).child(Long.toString(messageTimestamp)).removeValue().addOnCompleteListener(task -> {

            if (task.isSuccessful()) {
                deleteMessageFromDatabase(messageTimestamp);

            } else {
            }
        });

    }

    //Method to delete message from database once it is deleted from firebase
    private void deleteMessageFromDatabase(long messageTimestamp) {
        String tableName = "receiver" + receiverUID;
        String whereClause = MessageDetails.TIMESTAMP_ID + "=?";
        MessagesDbHelper messagesDbHelper = new MessagesDbHelper(this, receiverUID);
        SQLiteDatabase mDb = messagesDbHelper.getWritableDatabase();
        int rowsAffected = mDb.delete(tableName, whereClause, new String[]{Long.toString(messageTimestamp)});
        if (rowsAffected == 1) {
            messagesAdapter.removeMessage(messageTimestamp);
        }
        mDb.close();
    }

    private String getLatestMessageTimestampForThisChat() {
        MessagesDbHelper messagesDbHelper = new MessagesDbHelper(this, receiverUID);
        SQLiteDatabase mDb = messagesDbHelper.getReadableDatabase();
        Cursor cursor = mDb.query("receiver" + receiverUID,
                null,
                null,
                null,
                null,
                null,
                MessageDetails.TIMESTAMP_ID + " DESC",
                "1");
        int timestampColumnIndex = cursor.getColumnIndex(MessageDetails.TIMESTAMP_ID);
        String latestMessageTimestamp = "0";
        try {
            while (cursor.moveToNext()) {
                latestMessageTimestamp = String.valueOf(cursor.getLong(timestampColumnIndex));
            }
        } finally {
            mDb.close();
        }
        return latestMessageTimestamp;
    }


}