package com.chatout.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.chatout.ChatActivity;
import com.chatout.R;
import com.chatout.recyclerviewutils.ContactsAdapter;

public class SearchNewUserDialog extends DialogFragment {
    private FirebaseDatabase mDatabase;
    private DatabaseReference usernamesReference, contactNameReference;
    private static final String TAG = SearchNewUserDialog.class.getSimpleName();


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context context = getContext();
        View view = inflater.inflate(R.layout.search_by_username, container, false);
        mDatabase = FirebaseDatabase.getInstance();
        EditText searchByUsernameET = view.findViewById(R.id.search_by_username_edit_text);

        MaterialButton cancelSearchButton = view.findViewById(R.id.cancel_search);
        cancelSearchButton.setOnClickListener(v -> {
            if (getDialog() != null) {
                getDialog().dismiss();

            }
        });
        MaterialButton searchByUsernameButton = view.findViewById(R.id.perform_search);
        searchByUsernameButton.setOnClickListener(v -> {
            if (!isNetworkConnected()) {
                Toast.makeText(context, R.string.no_internet, Toast.LENGTH_SHORT).show();
                return;
            }
            searchByUsernameButton.setEnabled(false);
            if (!TextUtils.isEmpty(searchByUsernameET.getText().toString())) {
                String enteredUsername = searchByUsernameET.getText().toString();
                usernamesReference = mDatabase.getReference("usernames").child(enteredUsername);

                usernamesReference.get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().getValue() == null) {
                            Toast.makeText(context, R.string.username_not_found, Toast.LENGTH_SHORT).show();
                            searchByUsernameButton.setEnabled(true);
                        } else {
                            String receiverUID = task.getResult().getValue().toString();
                            contactNameReference = mDatabase.getReference("contacts").child(receiverUID).child("name");
                            contactNameReference.get().addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    String receiverName = task1.getResult().getValue().toString();
                                    Intent intentToLaunchChatActivity = new Intent(context, ChatActivity.class);
                                    intentToLaunchChatActivity.putExtra(ContactsAdapter.receiverUIDKey, receiverUID);
                                    intentToLaunchChatActivity.putExtra(ContactsAdapter.nameKey, receiverName);
                                    getDialog().dismiss();
                                    startActivity(intentToLaunchChatActivity);
                                }
                            });

                        }
                    }
                });

            } else {
                Toast.makeText(context, R.string.empty_username, Toast.LENGTH_SHORT).show();
            }

        });

        return view;
    }


    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }


    }

    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }


}
