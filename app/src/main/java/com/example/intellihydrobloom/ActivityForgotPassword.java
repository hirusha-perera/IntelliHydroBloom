package com.example.intellihydrobloom;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ActivityForgotPassword extends AppCompatActivity {

    private EditText fpEmail;
    private Button btnResetPassword;
    private ProgressDialog loadingBar;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();

        initializeField();

        btnResetPassword.setOnClickListener(view -> resetPassword());
    }

    private void resetPassword() {
        String email = fpEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            fpEmail.setError("Email is required!");
            fpEmail.requestFocus();
            return;
        }

        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            fpEmail.setError("Please provide a valid email!");
            fpEmail.requestFocus();
            return;
        }

        loadingBar.setTitle("Sending Reset Email");
        loadingBar.setMessage("Please wait...");
        loadingBar.setCanceledOnTouchOutside(true);
        loadingBar.show();

        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                Toast.makeText(ActivityForgotPassword.this, "Check your email to reset your password!", Toast.LENGTH_SHORT).show();
                sendUserToLoginActivity();
            }else {
                Toast.makeText(ActivityForgotPassword.this, "Try again! Something wrong happened!", Toast.LENGTH_SHORT).show();
            }
            loadingBar.dismiss();
        });
    }

    private void initializeField() {
        fpEmail = findViewById(R.id.fp_email);
        btnResetPassword = findViewById(R.id.btn_resetPassword);
        loadingBar = new ProgressDialog(this);
    }

    private void sendUserToLoginActivity() {
        Intent loginIntent = new Intent(ActivityForgotPassword.this, ActivityLogin.class);
        startActivity(loginIntent);
    }
}
