package com.mychatapp.recyclerviewutils;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.chrisbanes.photoview.PhotoView;
import com.google.firebase.auth.FirebaseAuth;

import com.mychatapp.dialogs.DeleteDialog;
import com.mychatapp.models.Message;
import com.mychatapp.R;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


    private ArrayList<Message> messages;
    private String conversationDatabasePath;
    private FirebaseAuth mAuth;
    String currentUserUID;

    private final int TYPE_SENT = 1;
    private final int TYPE_RECEIVED = 2;
    private final int TYPE_RECEIVED_IMAGE = 3;
    private final int TYPE_SENT_IMAGE= 4;
    public static final String TIMESTAMP_KEY = "message-timestamp";
    public static final String CONVERSATION_DATABASE_PATH = "conversation-database-path";
    private Context context;
    private LinearLayoutManager mLinearLayoutManager;

    public MessagesAdapter(Context context, String conversationDatabasePath, LinearLayoutManager mLinearLayoutManager) {
        this.context=context;
        this.conversationDatabasePath = conversationDatabasePath;
        this.mLinearLayoutManager=mLinearLayoutManager;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == TYPE_RECEIVED) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_received_message, parent, false);
            return new textMessageViewHolder(view);
        } else if (viewType == TYPE_SENT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sent_message, parent, false);
            return new textMessageViewHolder(view);
        } else if (viewType==TYPE_SENT_IMAGE){
            view=LayoutInflater.from(context).inflate(R.layout.item_sent_image,parent,false );
            return new imageMessageViewHolder(view);
        } else if(viewType==TYPE_RECEIVED_IMAGE){
            view=LayoutInflater.from(context).inflate(R.layout.item_received_image,parent,false );
            return new imageMessageViewHolder(view);
        }
        else {
            throw new RuntimeException("The View has to be sent or received");
        }

    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        mAuth = FirebaseAuth.getInstance();

        currentUserUID = mAuth.getCurrentUser().getUid();

        if (message.getUserID().equals(currentUserUID)&&message.getImageUrl()==null) {
            return TYPE_SENT;
        } else if(!message.getUserID().equals(currentUserUID)&&message.getImageUrl()==null) {
            return TYPE_RECEIVED;
        }
        else if(message.getUserID().equals(currentUserUID)&&message.getImageUrl()!=null){
            return TYPE_SENT_IMAGE;
        }
        else return TYPE_RECEIVED_IMAGE;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        long messageTimestamp = Long.parseLong(messages.get(position).getTimestamp());
        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(messageTimestamp));
        if(holder.getItemViewType()==TYPE_RECEIVED||holder.getItemViewType()==TYPE_SENT){

            ((textMessageViewHolder)holder).messageTimeView.setText(time);
            ((textMessageViewHolder)holder).messageTextView.setText(messages.get(position).getMessage());
        }
        else if(holder.getItemViewType()==TYPE_SENT_IMAGE||holder.getItemViewType()==TYPE_RECEIVED_IMAGE){
            ((imageMessageViewHolder)holder).messageTimeView.setText(time);
            Picasso.get().load(messages.get(position).getImageUrl()).placeholder(R.drawable.loading).into(((imageMessageViewHolder)holder).chatImageView);

        }

        holder.itemView.setOnLongClickListener(v -> {
            Context context = v.getContext();
            FragmentManager fragmentManager = ((AppCompatActivity) context).getSupportFragmentManager();
            DeleteDialog deleteDialog = new DeleteDialog();
            Bundle b = new Bundle();
            b.putString(TIMESTAMP_KEY, String.valueOf(messageTimestamp));
            b.putString(CONVERSATION_DATABASE_PATH, conversationDatabasePath);
            deleteDialog.setArguments(b);
            deleteDialog.show(fragmentManager, "DeleteMessageDialog");
            return false;
        });

    }

    public void removeMessage(Message message) {
        for (Message message1 : messages) {
            if (message.getTimestamp().equals(message1.getTimestamp())) {
                messages.remove(message1);
                notifyDataSetChanged();
            }
        }

    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static class textMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageTextView;
        private final TextView messageTimeView;

        public textMessageViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.message_text_view);
            messageTimeView = itemView.findViewById(R.id.message_time_view);
        }
    }
    public static class imageMessageViewHolder extends RecyclerView.ViewHolder{
        private final PhotoView chatImageView;
        private final TextView messageTimeView;
        public imageMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            chatImageView=itemView.findViewById(R.id.message_image_view);
            messageTimeView=itemView.findViewById(R.id.message_time_view);
        }

    }
    public void setMessages(ArrayList<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
        mLinearLayoutManager.scrollToPosition(getItemCount()-1);
    }



}
