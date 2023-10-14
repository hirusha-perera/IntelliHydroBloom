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

    // Declare Firebase Authentication instance
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        CardView btnScan = findViewById(R.id.btn_scan);
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openScanFragment();
            }
        });

        // Set up logout for btn_notes
        CardView btnNotes = findViewById(R.id.btn_notes);
        btnNotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutUser();
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

    private void logoutUser() {
        // Sign out the user from Firebase Authentication
        mAuth.signOut();

        // Redirect to ActivityLogin
        Intent loginIntent = new Intent(MainActivity.this, ActivityLogin.class);
        startActivity(loginIntent);
        finish();  // Close the MainActivity
    }
}
