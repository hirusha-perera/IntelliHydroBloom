package com.example.intellihydrobloom;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ActivityLogin extends AppCompatActivity {

    EditText txt_login_email;
    private EditText txt_login_password;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        txt_login_email = findViewById(R.id.txt_login_email);
        txt_login_password = findViewById(R.id.txt_login_password);

        ImageView img_show_hide_password = findViewById(R.id.img_show_hide_password);
        img_show_hide_password.setOnClickListener(view -> {
            if (txt_login_password.getTransformationMethod().equals(android.text.method.HideReturnsTransformationMethod.getInstance())) {
                txt_login_password.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
            } else {
                txt_login_password.setTransformationMethod(android.text.method.HideReturnsTransformationMethod.getInstance());
            }
        });

        Button btn_login = findViewById(R.id.btn_login);
        btn_login.setOnClickListener(view -> loginUser());

        TextView forget_password_link = findViewById(R.id.forget_password_link);
        forget_password_link.setOnClickListener(view -> {
            Intent forgetPasswordIntent = new Intent(ActivityLogin.this, ActivityForgotPassword.class);
            forgetPasswordIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(forgetPasswordIntent);
            finish();
        });

        TextView new_user_register_link = findViewById(R.id.new_user_register_link);
        new_user_register_link.setOnClickListener(view -> {
            Intent registerIntent = new Intent(ActivityLogin.this, ActivityRegister.class);
            registerIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(registerIntent);
            finish();
        });
    }

    void loginUser() {
        String email = txt_login_email.getText().toString().trim();
        String password = txt_login_password.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            txt_login_email.setError("This field is required");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            txt_login_email.setError("Please provide valid email!");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            txt_login_password.setError("This field is required");
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        sendUserToMainActivity();
                        Toast.makeText(ActivityLogin.this, "Authentication succeeded.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ActivityLogin.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendUserToMainActivity() {
        Intent mainIntent = new Intent(ActivityLogin.this, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(mainIntent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            sendUserToMainActivity();
        }
    }
}
