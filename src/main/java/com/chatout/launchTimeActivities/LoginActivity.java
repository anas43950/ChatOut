package com.chatout.launchTimeActivities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.chatout.models.Contact;
import com.chatout.MainActivity;
import com.chatout.R;


public class LoginActivity extends AppCompatActivity {
    private String email, password;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private TextView accountNotVerifiedTV;
    private FirebaseDatabase mDatabase;
    private String name, username;
    private DatabaseReference mUIDReference, mUsernameReference;
    private TextView passwordTV, emailTV;
    private MaterialButton loginButton;
    EditText loginEmailET, loginPasswordET;
    private String emailForFirebase;
    private String currentUserUID;
    private static final String TAG = LoginActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        accountNotVerifiedTV = findViewById(R.id.account_not_verified_tv_login);
        accountNotVerifiedTV.setVisibility(View.GONE);

        mAuth = FirebaseAuth.getInstance();
        loginEmailET = findViewById(R.id.login_email);
        loginPasswordET = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        emailTV = findViewById(R.id.login_email_tv);
        passwordTV = findViewById(R.id.login_password_tv);


        mDatabase = FirebaseDatabase.getInstance();

        loginButton.setOnClickListener(l -> {
            email = loginEmailET.getText().toString();
            password = loginPasswordET.getText().toString();
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(LoginActivity.this, "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }
            loginEmailET.setEnabled(false);
            loginPasswordET.setEnabled(false);
            loginButton.setClickable(false);


            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {

                        if (!task.isSuccessful()) {
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                Toast.makeText(LoginActivity.this, "Wrong password", Toast.LENGTH_SHORT).show();
                                loginEmailET.setEnabled(true);
                                loginPasswordET.setEnabled(true);
                                loginButton.setClickable(true);
                            } else if (task.getException() instanceof FirebaseNetworkException) {
                                loginButton.setClickable(true);
                                Toast.makeText(LoginActivity.this, R.string.no_internet, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(LoginActivity.this, "Failed to login , please try again.", Toast.LENGTH_SHORT).show();
                                loginButton.setClickable(true);
                            }
                        } else {
                            checkIfEmailVerified();
                        }
                    });

        });
        loginButton.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                //Doing this to upload username,email and uid relation to firebase
                if (s.toString().equals("Submit")) {
                    loginButton.setOnClickListener(l -> {
                        if (!TextUtils.isEmpty(loginEmailET.getText().toString())) {
                            name = loginEmailET.getText().toString();
                        } else {
                            Toast.makeText(LoginActivity.this, "Please provide a name", Toast.LENGTH_SHORT).show();
                        }
                        if (!TextUtils.isEmpty(loginPasswordET.getText().toString())) {
                            username = loginPasswordET.getText().toString();
                        } else {
                            Toast.makeText(LoginActivity.this, "Please provide a username", Toast.LENGTH_SHORT).show();
                        }
                        currentUserUID = mAuth.getCurrentUser().getUid();

                        mUsernameReference = mDatabase.getReference("usernames").child(username);
                        mUIDReference = mDatabase.getReference("uids").child(currentUserUID);
                        Contact currentUserContact = new Contact(name, username, currentUserUID);
                        //uploading username and email relation
                        mUsernameReference.setValue(currentUserUID).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                //uploading uid and name,username relation
                                mUIDReference.setValue(currentUserContact).addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        Toast.makeText(LoginActivity.this, R.string.signuploginfailed, Toast.LENGTH_SHORT).show();
                                    }
                                });

                            } else {
                                accountNotVerifiedTV.setText(R.string.username_already_taken);
                            }
                        });
                    });

                }

            }
        });


    }

    private void checkIfEmailVerified() {
        mUser = mAuth.getCurrentUser();
        if (mUser != null) {
            if (mUser.isEmailVerified()) {
                accountNotVerifiedTV.setVisibility(View.GONE);
                checkIfUsernameProvided();


            } else {
                mAuth.signOut();
                accountNotVerifiedTV.setVisibility(View.VISIBLE);
                accountNotVerifiedTV.setText(R.string.account_not_verified);

            }
        }
    }

    private void checkIfUsernameProvided() {
        emailForFirebase = email.replaceAll("\\.", ",");
        currentUserUID = mAuth.getCurrentUser().getUid();
        mUIDReference = mDatabase.getReference("contacts").child(currentUserUID);


        mUIDReference.get().addOnCompleteListener(task -> {
            if (task.getResult().getValue() == null) {

                accountNotVerifiedTV.setVisibility(View.VISIBLE);
                accountNotVerifiedTV.setText("Please provide a username for this account");
                emailTV.setText("Name");
                passwordTV.setText("Username");
                loginButton.setText("Submit");
                loginPasswordET.setInputType(InputType.TYPE_CLASS_TEXT);
                loginEmailET.setEnabled(true);
                loginPasswordET.setEnabled(true);
                loginButton.setClickable(true);
                loginEmailET.setText("");
                loginPasswordET.setText("");


            } else {

                getSharedPreferences("PREFERENCE", MODE_PRIVATE).edit()
                        .putBoolean("isFirstRun", false).apply();
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();


            }
        });


    }

}