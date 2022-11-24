package com.example.chatapp.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import com.example.chatapp.R;

public class SlashScreenActivity extends AppCompatActivity {

    TextView txtPhienBan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slash_screen);


        txtPhienBan = findViewById(R.id.txtPhienBan);
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(),0);
            txtPhienBan.setText(getString(R.string.phienban)+" "+ packageInfo.versionName);

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(getApplicationContext(),SignInActivity.class);
                    startActivity(intent);
                    finish();
                }
            },2000);



        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }
}