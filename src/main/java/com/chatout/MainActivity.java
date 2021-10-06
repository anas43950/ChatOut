package com.chatout;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.chatout.contactlistdata.ContactListDatabase;
import com.chatout.dialogs.SearchNewUserDialog;
import com.chatout.launchTimeActivities.SplashActivity;
import com.chatout.messagesdata.MessagesDbHelper;
import com.chatout.models.Contact;
import com.chatout.recyclerviewutils.ContactsAdapter;


import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {

    private ContactsAdapter contactsAdapter;
    private FirebaseDatabase mDatabase;
    private FirebaseAuth mAuth;
    public DatabaseReference contactsUIDReference, contactsReference;
    private String currentUserUID;
    private static final String TAG = MainActivity.class.getSimpleName();
    private ContactListDatabase mDb;

    private RecyclerView contactsRV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mDb = ContactListDatabase.getInstance(getApplicationContext());


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ExecutorService executor = Executors.newFixedThreadPool(1);

        contactsRV = findViewById(R.id.contactsRV);
        final LiveData<List<Contact>> contacts = mDb.contactsDao().loadAllContacts();
        contacts.observe(this, contacts1 -> contactsAdapter.setmContacts(contacts1));
        contactsAdapter = new ContactsAdapter(this);
        contactsRV.setAdapter(contactsAdapter);
        contactsRV.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.search_fab).setOnClickListener(l -> {
            FragmentManager fragmentManager = getSupportFragmentManager();
            SearchNewUserDialog searchNewUserDialog = new SearchNewUserDialog();
            searchNewUserDialog.show(fragmentManager, "SearchNewUserDialog");
        });

        mDatabase = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUserUID = mAuth.getCurrentUser().getUid();
        contactsUIDReference = mDatabase.getReference("contactList").child(currentUserUID);
        contactsReference = mDatabase.getReference("contacts");
        contactsUIDReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String contactUID = snapshot.getValue().toString();
                contactsReference.child(contactUID).get().addOnCompleteListener(task -> {
                    if (task.getResult().getValue() != null) {
                        Contact contact = task.getResult().getValue(Contact.class);
                        executor.execute(() -> mDb.contactsDao().insertContact(contact));
                    }
                });


            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String deletedContactUID = snapshot.getValue().toString();
                contactsReference.child(deletedContactUID).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Contact contact = task.getResult().getValue(Contact.class);
                        executor.execute(() -> mDb.contactsDao().deleteContactById(contact.getMUserID()));
                        MessagesDbHelper messagesDbHelper = new MessagesDbHelper(MainActivity.this, deletedContactUID);
                        SQLiteDatabase mDb = messagesDbHelper.getWritableDatabase();
                        mDb.delete("receiver" + deletedContactUID, null, null);
                        mDb.close();
                    }
                });


            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_menu, menu);


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.sign_out_menu) {


            AuthUI.getInstance().signOut(this);
            Toast.makeText(MainActivity.this, "Signed Out!", Toast.LENGTH_SHORT).show();
            getSharedPreferences("PREFERENCE", MODE_PRIVATE).edit()
                    .putBoolean("isFirstRun", true).apply();
            startActivity(new Intent(MainActivity.this, SplashActivity.class));
        }

        return super.onOptionsItemSelected(item);

    }




}