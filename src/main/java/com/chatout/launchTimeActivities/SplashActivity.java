package com.chatout.launchTimeActivities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.chatout.MainActivity;

public class SplashActivity extends AppCompatActivity {




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean isFirstRun = getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                .getBoolean("isFirstRun", true);
        super.onCreate(savedInstanceState);
        if (isFirstRun) {
              startActivity(new Intent(SplashActivity.this, SignupActivity.class));
            }
            else {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            }



    }
}