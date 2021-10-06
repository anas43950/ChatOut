package com.chatout.recyclerviewutils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import com.chatout.ChatActivity;
import com.chatout.models.Contact;
import com.chatout.dialogs.DeleteDialog;
import com.chatout.R;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> {
    private List<Contact> mContacts;
    private Context mContext;
    public static final String receiverUIDKey = "receiver-uid";
    public static final String nameKey = "name-uid";
    private static final String TAG = ContactsAdapter.class.getSimpleName();
    public static final String SELECTED_CONTACT_UID = "selected-contact-uid";


    public ContactsAdapter(Context context) {
        mContext = context;

    }

    @NonNull
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_person, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ContactsAdapter.ViewHolder holder, int position) {
        Contact contact = mContacts.get(position);
        holder.nameTextView.setText(contact.getName());
        holder.itemView.setOnClickListener(l -> {

            Intent intent = new Intent(mContext, ChatActivity.class);

            intent.putExtra(receiverUIDKey, contact.getMUserID());
            intent.putExtra(nameKey,contact.getName());
            mContext.startActivity(intent);

        });
        holder.itemView.setOnLongClickListener(v -> {
            Bundle b = new Bundle();
            b.putString(SELECTED_CONTACT_UID, contact.getMUserID());
            DeleteDialog deleteDialog = new DeleteDialog();
            deleteDialog.setArguments(b);
            FragmentManager fragmentManager = ((AppCompatActivity) mContext).getSupportFragmentManager();
            deleteDialog.show(fragmentManager, "DeleteUserDialog");

            return false;
        });


    }

    @Override
    public int getItemCount() {
        if (mContacts == null) {
            return 0;
        }
        return mContacts.size();
    }

//
//    private Contact getContact(int position) {
//        return mContacts.get(position);
//    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView nameTextView;

        public ViewHolder(View v) {
            super(v);
            nameTextView = v.findViewById(R.id.contactTextView);
        }


    }


    public void setmContacts(List<Contact> mContacts) {
        this.mContacts = mContacts;
        notifyDataSetChanged();
    }
}
