package com.mychatapp.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mychatapp.R;
import com.mychatapp.recyclerviewutils.ContactsAdapter;
import com.mychatapp.recyclerviewutils.MessagesAdapter;

public class DeleteDialog extends DialogFragment {
    private FirebaseDatabase mDatabase;
    private DatabaseReference mMessageReference, mContactListReference, mMessagesReferenceForOneUser, mMessagesReferenceForAllUsers;
    private FirebaseAuth mAuth;
    private static final String TAG = DeleteDialog.class.getSimpleName();
    private String currentUserUID;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.delete_dialog, container, false);
        Context context = getContext();
        Bundle bundle = getArguments();
        Dialog dialog = getDialog();
        mDatabase = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUserUID = mAuth.getCurrentUser().getUid();
        MaterialButton deleteButton = view.findViewById(R.id.perform_delete);
        MaterialButton cancelDeleteButton = view.findViewById(R.id.cancel_delete);
        cancelDeleteButton.setOnClickListener(v -> {
            if (dialog != null) {
                dialog.dismiss();
            }
        });
//We are using this dialog for two purposes hence we have to specify for which purpose this dialog has been created
        if (getTag().equals("DeleteMessageDialog")) {
            String timestamp = bundle.getString(MessagesAdapter.TIMESTAMP_KEY, "");
            String receiverUID = bundle.getString(MessagesAdapter.CONVERSATION_DATABASE_PATH, "");
            mMessageReference = mDatabase.getReference("messages").child(currentUserUID).child(receiverUID).child(timestamp);
            deleteButton.setOnClickListener(v -> {
                dialog.dismiss();

                mMessageReference.removeValue().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(context, R.string.delete_message_successful, Toast.LENGTH_SHORT).show();
                    }
                    if (!task.isSuccessful()) {
                        Toast.makeText(context, R.string.delete_message_failed, Toast.LENGTH_SHORT).show();

                    }
                });
            });
        } else if (getTag().equals("DeleteUserDialog")) {
            ((TextView) view.findViewById(R.id.delete_dialog_tv)).setText(R.string.delete_user);

            String selectedUserUID = bundle.getString(ContactsAdapter.SELECTED_CONTACT_UID);
            mContactListReference = mDatabase.getReference("contactList").child(currentUserUID).child(selectedUserUID);
            mMessagesReferenceForOneUser = mDatabase.getReference("messages").child(currentUserUID).child(selectedUserUID);
            deleteButton.setOnClickListener(l -> {
                dialog.dismiss();

                mMessagesReferenceForOneUser.removeValue().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        mContactListReference.removeValue().addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                Toast.makeText(context, R.string.delete_user_successful, Toast.LENGTH_SHORT).show();
                            } else if (!task1.isSuccessful()) {
                                Toast.makeText(context, R.string.delete_user_failed, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else if (!task.isSuccessful()) {
                        Toast.makeText(context, R.string.delete_user_failed, Toast.LENGTH_SHORT).show();
                    }
                });
            });


        } else if (getTag().equals("DeleteAllUsersDialog")) {
            ((TextView) view.findViewById(R.id.delete_dialog_tv)).setText(R.string.delete_all_users);

            mMessagesReferenceForAllUsers = mDatabase.getReference("messages").child(currentUserUID);
            mContactListReference = mDatabase.getReference("contactList").child(currentUserUID);
            deleteButton.setOnClickListener(l -> {
                dialog.dismiss();
                mMessagesReferenceForAllUsers.removeValue().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        mContactListReference.removeValue().addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                Toast.makeText(context, R.string.delete_all_users_successful, Toast.LENGTH_SHORT).show();
                            } else if (!task1.isSuccessful()) {
                                Toast.makeText(context, R.string.delete_all_users_failed, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else if (!task.isSuccessful()) {
                        Toast.makeText(context, R.string.delete_all_users_failed, Toast.LENGTH_SHORT).show();
                    }
                });
            });

        }
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
