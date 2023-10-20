package com.example.intellihydrobloom;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.cardview.widget.CardView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

// Import Firebase Authentication
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {


    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mAuth = FirebaseAuth.getInstance();

        CardView btnScan = findViewById(R.id.btn_scan);
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openScanFragment();
            }
        });


        CardView btnSettings = findViewById(R.id.btn_settings);
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });

        CardView btnDevice = findViewById(R.id.btn_devices);
        btnDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {openDeviceFragment();
            }
        });
    }

    private void openScanFragment() {
        ScanFragment scanFragment = new ScanFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, scanFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void openDeviceFragment() {
        DeviceFragment deviceFragment = new DeviceFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, deviceFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void logoutUser() {

        mAuth.signOut();

        Intent loginIntent = new Intent(MainActivity.this, ActivityLogin.class);
        startActivity(loginIntent);
        finish();
    }
}
