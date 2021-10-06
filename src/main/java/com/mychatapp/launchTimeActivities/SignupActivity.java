package com.mychatapp.launchTimeActivities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;

import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mychatapp.models.Contact;
import com.mychatapp.MainActivity;
import com.mychatapp.R;

public class SignupActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private String name, username, email, password;
    private FirebaseUser mUser;
    private FirebaseDatabase mDatabase;
    private DatabaseReference mUsernameReference, mUIDReference;
    private TextView accountNotVerifiedSignupTV, emailTV, passwordTV;
    private MaterialButton signUpButton;
    private EditText signupPasswordET, signupEmailET;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        //initializing firebase related variables
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();


        //Initializing views
        emailTV = findViewById(R.id.sign_up_email_tv);
        passwordTV = findViewById(R.id.sign_up_password_tv);
        TextView loginTv = findViewById(R.id.login_instead_tv);
        //initializing a clickable string for opening login activity
        SpannableString ss = new SpannableString(getResources().getString(R.string.sign_in_instead));
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                startActivity(new Intent(SignupActivity.this, LoginActivity.class));

            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
            }
        };
        ss.setSpan(clickableSpan, 16, 23, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        loginTv.setText(ss);
        loginTv.setMovementMethod(LinkMovementMethod.getInstance());
        loginTv.setHighlightColor(Color.BLUE);


        //Sign Up view setup
        signupEmailET = findViewById(R.id.sign_up_email);
        signupPasswordET = findViewById(R.id.sign_up_password);
        signUpButton = findViewById(R.id.sign_up_button);
        accountNotVerifiedSignupTV = findViewById(R.id.account_verification_tv_signup);
        accountNotVerifiedSignupTV.setVisibility(View.GONE);


        signUpButton.setOnClickListener(l -> {


            if (!TextUtils.isEmpty(signupEmailET.getText().toString())) {
                email = signupEmailET.getText().toString();
                signupEmailET.setEnabled(false);

            } else {
                Toast.makeText(this, "Please enter an email", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!TextUtils.isEmpty(signupPasswordET.getText().toString()) && signupPasswordET.getText().toString().length() > 5) {
                password = signupPasswordET.getText().toString();
                signupPasswordET.setEnabled(false);
            } else {
                Toast.makeText(this, "Please enter a password of 6 characters or more", Toast.LENGTH_SHORT).show();
                return;
            }

            signUpButton.setClickable(false);
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    sendVerificationEmail();

                } else if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                    Toast.makeText(SignupActivity.this, "Account with this email already exist, try sign in instead", Toast.LENGTH_SHORT).show();
                    signUpButton.setClickable(true);

                } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                    Toast.makeText(SignupActivity.this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
                    signUpButton.setClickable(true);
                } else if (task.getException() instanceof FirebaseNetworkException) {
                    Toast.makeText(SignupActivity.this, R.string.no_internet, Toast.LENGTH_SHORT).show();
                    signUpButton.setClickable(true);
                }

            });


        });
        signUpButton.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().equals("Continue")) {
                    signUpButton.setOnClickListener(v -> {

                        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task1 -> {
                            if (!task1.isSuccessful()) {

                            } else {
                                checkIfEmailVerified();
                            }
                        });

                    });
                }

            }
        });

    }

    private void sendVerificationEmail() {
        mUser = mAuth.getCurrentUser();
        mUser.sendEmailVerification().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                accountNotVerifiedSignupTV.setVisibility(View.VISIBLE);
                accountNotVerifiedSignupTV.setText(R.string.verification_link_sent);
                signUpButton.setClickable(true);
                signUpButton.setText("Continue");


            }
        });


    }

    private void checkIfEmailVerified() {
        mUser = mAuth.getCurrentUser();
        if (mUser != null) {
            if (mUser.isEmailVerified()) {

                //adjusting sign up form views for username and name input
                emailTV.setText("Name");
                passwordTV.setText("Username");
                signUpButton.setText("Submit");
                signupEmailET.setText("");
                signupPasswordET.setText("");
                signupEmailET.setEnabled(true);
                signupPasswordET.setEnabled(true);
                signupPasswordET.setInputType(InputType.TYPE_CLASS_TEXT);
                accountNotVerifiedSignupTV.setText("Please provide a display name and a unique username");
                name = signupEmailET.getText().toString();


                //assigning new onclicklistener to signup button to upload name and username in firebase
                signUpButton.setOnClickListener(l -> {
                    if (!TextUtils.isEmpty(signupEmailET.getText().toString())) {
                        name = signupEmailET.getText().toString();
                    } else {
                        Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!TextUtils.isEmpty(signupPasswordET.getText().toString())) {
                        username = signupPasswordET.getText().toString();
                    } else {
                        Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    uploadNameUsernameUIDToFirebase(name, username);
                });


            } else {
                accountNotVerifiedSignupTV.setText(R.string.account_not_verified);
                mAuth.signOut();


            }
        }
    }


    private void uploadNameUsernameUIDToFirebase(String name, String username) {

        String currentUserUID = mUser.getUid();

        mUsernameReference = mDatabase.getReference("usernames").child(username);
        mUIDReference = mDatabase.getReference("contacts").child(currentUserUID);
        Contact currentUserContact = new Contact(name, username, currentUserUID);
        //uploading username and email relation
        mUsernameReference.setValue(currentUserUID).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                //uploading uid and name,username relation
                mUIDReference.setValue(currentUserContact).addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        getSharedPreferences("PREFERENCE", MODE_PRIVATE).edit()
                                .putBoolean("isFirstRun", false).apply();
                        startActivity(new Intent(SignupActivity.this, MainActivity.class));
                    }
                });

            } else {
                accountNotVerifiedSignupTV.setText(R.string.username_already_taken);
            }
        });


    }
}