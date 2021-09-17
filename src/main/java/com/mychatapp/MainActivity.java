package com.mychatapp;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
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
import com.mychatapp.contactlistdata.ContactListDatabase;
import com.mychatapp.dialogs.DeleteDialog;
import com.mychatapp.dialogs.SearchNewUserDialog;
import com.mychatapp.launchTimeActivities.SplashActivity;
import com.mychatapp.models.Contact;
import com.mychatapp.recyclerviewutils.ContactsAdapter;


import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;
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

    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main_menu, menu);


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
        } else if (item.getItemId() == R.id.search_by_username) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            SearchNewUserDialog searchNewUserDialog = new SearchNewUserDialog();
            searchNewUserDialog.show(fragmentManager, "SearchNewUserDialog");

        } else if (item.getItemId() == R.id.delete_all_users) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            DeleteDialog deleteDialog = new DeleteDialog();
            deleteDialog.show(fragmentManager, "DeleteAllUsersDialog");
        }
        return super.onOptionsItemSelected(item);

    }

    public static long getCurrentTimestamp() throws IOException {
        String TIME_SERVER = "time-a.nist.gov";
        NTPUDPClient timeClient = new NTPUDPClient();
        InetAddress inetAddress = InetAddress.getByName(TIME_SERVER);
        TimeInfo timeInfo = timeClient.getTime(inetAddress);
        long returnTime = timeInfo.getMessage().getReceiveTimeStamp().getTime();
        Date time = new Date(returnTime);
        return time.getTime();
    }


}