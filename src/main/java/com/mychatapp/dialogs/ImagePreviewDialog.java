package com.mychatapp.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.Image;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mychatapp.ChatActivity;
import com.mychatapp.MainActivity;
import com.mychatapp.models.Message;
import com.mychatapp.R;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ImagePreviewDialog extends DialogFragment {
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private FirebaseDatabase mDatabase;
    private DatabaseReference messagesReference;
    private FirebaseAuth mAuth;
    private String currentUserUid, receiverUID, path, uri_string;
    private static final String TAG = ImagePreviewDialog.class.getSimpleName();
    private byte[] downsizedImageBytes;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {


//        String timestamp = String.valueOf(realDate.getTime());
        path = getArguments().getString(ChatActivity.IMAGE_PATH_KEY);
        uri_string = getArguments().getString(ChatActivity.IMAGE_URI_KEY);
        receiverUID = getArguments().getString(ChatActivity.RECEIVER_UID_KEY);
        View view;
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference().child("chat_photos");
        mDatabase = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUserUid = mAuth.getCurrentUser().getUid();
        messagesReference = mDatabase.getReference("messages");
        view = inflater.inflate(R.layout.chat_image_preview, container, false);

        PhotoView imagePreviewView = view.findViewById(R.id.image_preview);
        Bitmap myBitmap = BitmapFactory.decodeFile(path);

        try {
            int scaleDivider = 2;

            // 2. Get the downsized image content as a byte[]
            int scaleWidth = myBitmap.getWidth() / scaleDivider;
            int scaleHeight = myBitmap.getHeight() / scaleDivider;
            downsizedImageBytes =
                    getDownsizedImageBytes(myBitmap, scaleWidth, scaleHeight);
            Bitmap compressedBitmap = BitmapFactory.decodeByteArray(downsizedImageBytes, 0, downsizedImageBytes.length);
            imagePreviewView.setImageBitmap(compressedBitmap);

        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        }

        ImageButton cancelSendButton=view.findViewById(R.id.cancel_send_image_button);
        cancelSendButton.setOnClickListener(l -> getDialog().dismiss());

    ImageButton sendImageButton=view.findViewById(R.id.perform_send_image_button);
    sendImageButton.setOnClickListener(v -> {

        cancelSendButton.setEnabled(false);
        sendImageButton.setEnabled(false);
            long timestamp = System.currentTimeMillis();
            storageReference.child(currentUserUid + timestamp).putBytes(downsizedImageBytes).addOnSuccessListener(taskSnapshot -> {
                if (taskSnapshot.getMetadata() != null) {
                    if (taskSnapshot.getMetadata().getReference() != null) {
                        Task<Uri> result = taskSnapshot.getStorage().getDownloadUrl();
                        result.addOnSuccessListener(uri -> {
                            getDialog().dismiss();

                            String downloadUrl = uri.toString();
                            long timestamp1 = System.currentTimeMillis();
                            Message message = new Message(null, currentUserUid, timestamp1, downloadUrl);
                            messagesReference.child(currentUserUid).child(receiverUID).child(String.valueOf(timestamp1)).setValue(message).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()){
                                        Log.d(TAG, "onComplete: Image Sent and url updated");
                                    }else{
                                        Log.d(TAG, "onComplete: Url updation failed: "+task.getException().toString());
                                    }
                                }
                            });
                            messagesReference.child(receiverUID).child(currentUserUid).child(String.valueOf(timestamp1)).setValue(message);

                        });
                    }
                }
            });
        });

        return view;
    }


    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
        }


    }

    public byte[] getDownsizedImageBytes(Bitmap fullBitmap, int scaleWidth, int scaleHeight) throws IOException {

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(fullBitmap, scaleWidth, scaleHeight, true);

        // 2. Instantiate the downsized image content as a byte[]
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

        return baos.toByteArray();

    }

}
