package com.mychatapp;

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
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mychatapp.dialogs.ImagePreviewDialog;
import com.mychatapp.messagesdata.MessageContract.MessageDetails;
import com.mychatapp.messagesdata.MessagesDbHelper;
import com.mychatapp.models.Message;
import com.mychatapp.recyclerviewutils.ContactsAdapter;
import com.mychatapp.recyclerviewutils.MessagesAdapter;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import java.util.List;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ChatActivity extends AppCompatActivity {
    // Firebase instance variables
    private FirebaseDatabase mDatabase;
    private DatabaseReference mMessagesDatabaseReference, mContactNameReference, mSenderContactsReference, mReceiverContactsReference;
    private ChildEventListener messagesListener;
    private ValueEventListener singleValueMessageListener;
    private FirebaseAuth mFirebaseAuth;
    public String currentUserUID, receiverUID,receiverName;
    private static final String TAG = ChatActivity.class.getSimpleName();
    public static final String IMAGE_PATH_KEY = "image-path-key";
    public static final String IMAGE_URI_KEY = "image-uri-key";
    public static final String RECEIVER_UID_KEY = "image-uri-key";
    private String path;
    private MessagesDbHelper messagesDbHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Intent intentThatLaunchedActivity = getIntent();
        receiverUID = intentThatLaunchedActivity.getStringExtra(ContactsAdapter.receiverUIDKey);
        receiverName=intentThatLaunchedActivity.getStringExtra(ContactsAdapter.nameKey);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        //Initializing local database
        messagesDbHelper = new MessagesDbHelper(this, receiverUID);
        //Initializing firebase variables
        mDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        currentUserUID = mFirebaseAuth.getCurrentUser().getUid();
        mMessagesDatabaseReference = mDatabase.getReference("messages");
        mContactNameReference = mDatabase.getReference("contacts").child(receiverUID);
        mSenderContactsReference = mDatabase.getReference("contactList").child(currentUserUID).child(receiverUID);
        mReceiverContactsReference = mDatabase.getReference("contactList").child(receiverUID).child(currentUserUID);


        RecyclerView messagesRV = findViewById(R.id.messagesRV);
        MaterialButton sendButton = findViewById(R.id.send_button);
        sendButton.setEnabled(false);

        TextInputEditText messageEditText = findViewById(R.id.messageEditText);
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this);
        messagesRV.getRecycledViewPool().setMaxRecycledViews(0, 0);


        MessagesAdapter messagesAdapter = new MessagesAdapter(this, receiverUID,mLinearLayoutManager);
        messagesAdapter.setMessages((ArrayList<Message>) loadAllMessagesOfThisChat());

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
        findViewById(R.id.chat_activity_back_button).setOnClickListener(l -> {
            finish();
        });
        MaterialButton imagePickerButton = findViewById(R.id.image_picker_button);
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
        MaterialButton chatMenu = findViewById(R.id.chat_activity_menu_button);
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
                            }
                        }
                    });
                    return true;
                }
                return false;
            });
            popupMenu.show();
        });


        singleValueMessageListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getChildrenCount() == 1) {
                    mSenderContactsReference.setValue(receiverUID);
                    mReceiverContactsReference.setValue(currentUserUID);

                } else if (snapshot.getChildrenCount() > 1) {

                } else {
                    mMessagesDatabaseReference.child(currentUserUID).child(receiverUID).removeEventListener(singleValueMessageListener);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };


        messagesRV.setAdapter(messagesAdapter);
        messagesRV.setLayoutManager(mLinearLayoutManager);
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
        sendButton.setOnClickListener(l -> executor.execute(() -> {
            try {
                String timestamp = String.valueOf(MainActivity.getCurrentTimestamp());
                Message message = new Message(messageEditText.getText().toString(), currentUserUID, timestamp, null);

                mMessagesDatabaseReference.child(currentUserUID).child(receiverUID).child(timestamp).setValue(message).addOnSuccessListener(unused -> mMessagesDatabaseReference.child(currentUserUID).child(receiverUID).addValueEventListener(singleValueMessageListener));
                mMessagesDatabaseReference.child(receiverUID).child(currentUserUID).child(timestamp).setValue(message);

            } catch (IOException e) {
                Toast.makeText(ChatActivity.this, R.string.send_message_failed, Toast.LENGTH_SHORT).show();
            }
            runOnUiThread(() -> {
                messageEditText.setText("");
                sendButton.setEnabled(false);

            });
        }));
        messagesListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull @NotNull DataSnapshot snapshot, @Nullable @org.jetbrains.annotations.Nullable String previousChildName) {

                Message message = snapshot.getValue(Message.class);
                SQLiteDatabase mDb = messagesDbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put(MessageDetails.TIMESTAMP_ID, message.getTimestamp());
                values.put(MessageDetails.COLUMN_MESSAGE, message.getMessage());
                values.put(MessageDetails.COLUMN_IMAGE_URI, message.getImageUrl());
                values.put(MessageDetails.COLUMN_SENDER_UID, message.getUserID());
                long rowId = mDb.insert("receiver" + receiverUID, null, values);
                Toast.makeText(ChatActivity.this, "Row saved with Id: " + rowId, Toast.LENGTH_SHORT).show();
//                messagesRV.scrollToPosition(messagesAdapter.getItemCount() - 1);


            }

            @Override
            public void onChildChanged(@NonNull @NotNull DataSnapshot snapshot, @Nullable @org.jetbrains.annotations.Nullable String previousChildName) {


            }

            @Override
            public void onChildRemoved(@NonNull @NotNull DataSnapshot snapshot) {
                if (snapshot.getChildrenCount() > 1) {
//                    messages.clear();
                    messagesAdapter.notifyDataSetChanged();
                } else {
                    Message message = snapshot.getValue(Message.class);
                    messagesAdapter.removeMessage(message);
                    messagesRV.scrollToPosition(messagesAdapter.getItemCount() - 1);
                }
            }

            @Override
            public void onChildMoved(@NonNull @NotNull DataSnapshot snapshot, @Nullable @org.jetbrains.annotations.Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        };
        mMessagesDatabaseReference.child(currentUserUID).child(receiverUID).startAfter("1631764331992").addChildEventListener(messagesListener);
        ((TextView) findViewById(R.id.chat_name_tv)).setText(receiverName);



    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMessagesDatabaseReference.child(currentUserUID).child(receiverUID).removeEventListener(messagesListener);

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

    private List<Message> loadAllMessagesOfThisChat() {
        List<Message> messages = new ArrayList<>();

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
        while (cursor.moveToNext()) {
            Message message = new Message();
            message.setMessage(cursor.getString(messageColumnIndex));
            message.setImageUrl(cursor.getString(imageURIColumnIndex));
            message.setTimestamp(String.valueOf(cursor.getInt(timestampColumnIndex)));
            message.setUserID(cursor.getString(senderUIDColumnIndex));
            messages.add(message);

        }
        cursor.close();
        mDb.close();
        return messages;
    }

}