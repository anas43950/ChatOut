package com.mychatapp.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import com.mychatapp.ChatActivity;
import com.mychatapp.R;
import com.mychatapp.recyclerviewutils.ContactsAdapter;

public class SearchNewUserDialog extends DialogFragment {
    private FirebaseDatabase mDatabase;
    private DatabaseReference usernamesReference;
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
        MaterialButton searchByUsernameButton =view.findViewById(R.id.perform_search);
        searchByUsernameButton.setOnClickListener(v -> {
            searchByUsernameButton.setEnabled(false);
            if (!TextUtils.isEmpty(searchByUsernameET.getText().toString())) {
                String enteredUsername = searchByUsernameET.getText().toString();
                usernamesReference = mDatabase.getReference("usernames").child(enteredUsername);

                usernamesReference.get().addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        if(task.getResult().getValue()==null){
                            Toast.makeText(context,R.string.username_not_found,Toast.LENGTH_SHORT).show();
                            searchByUsernameButton.setEnabled(true);
                        }
                        else{
                            Intent intentToLaunchChatActivity=new Intent(context, ChatActivity.class );
                            intentToLaunchChatActivity.putExtra(ContactsAdapter.receiverUIDKey,task.getResult().getValue().toString());
                            Log.d(TAG, "onCreateView: receiverUID: "+task.getResult().getValue().toString());
                            getDialog().dismiss();
                            startActivity(intentToLaunchChatActivity);
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
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
        }


    }


}
