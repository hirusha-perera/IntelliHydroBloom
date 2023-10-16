package com.example.intellihydrobloom;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ActivityRegister extends AppCompatActivity {

    protected EditText txt_register_user_name, txt_register_email, txt_register_mobile_number, txt_register_farm_name, txt_register_location, txt_register_password, txt_register_confirmed_password;
    private Spinner sp_register_country;
    private ProgressDialog loadingBar;
    protected Button btn_register;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeFields();
        setupRegisterButton();
        setupLoginLink();
    }

    private void initializeFields() {
        txt_register_user_name = findViewById(R.id.txt_register_user_name);
        txt_register_email = findViewById(R.id.txt_register_email);
        txt_register_mobile_number = findViewById(R.id.txt_register_mobile_number);
        txt_register_farm_name = findViewById(R.id.txt_register_farm_name);
        txt_register_location = findViewById(R.id.txt_register_location);
        txt_register_password = findViewById(R.id.txt_register_password);
        txt_register_confirmed_password = findViewById(R.id.txt_register_confirmed_password);
        sp_register_country = findViewById(R.id.sp_register_country);
        loadingBar = new ProgressDialog(this);
    }

    private void setupRegisterButton() {
        Button btn_register = findViewById(R.id.btn_register);
        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validInputs()) {
                    String txtUserName = txt_register_user_name.getText().toString().trim();
                    String txtEmail = txt_register_email.getText().toString().trim();
                    String txtMobile = txt_register_mobile_number.getText().toString().trim();
                    String txtFarmName = txt_register_farm_name.getText().toString().trim();
                    String txtLocation = txt_register_location.getText().toString().trim();
                    String txtCountry = sp_register_country.getSelectedItem().toString();
                    String txtPassword = txt_register_password.getText().toString().trim();

                    loadingBar.setTitle("Creating New Account");
                    loadingBar.setMessage("Please wait, while we are creating new account for you...");
                    loadingBar.setCanceledOnTouchOutside(true);
                    loadingBar.show();

                    registerUser(txtUserName, txtEmail, txtMobile, txtFarmName, txtLocation, txtCountry, txtPassword);
                }
            }
        });
    }

    private boolean validInputs() {
        String txtEmail = txt_register_email.getText().toString().trim();
        String txtMobile = txt_register_mobile_number.getText().toString().trim();
        String txtPassword = txt_register_password.getText().toString().trim();
        String txtConfirmedPassword = txt_register_confirmed_password.getText().toString().trim();

        if (TextUtils.isEmpty(txt_register_user_name.getText().toString().trim())) {
            txt_register_user_name.setError("Your Name is required.");
            return false;
        }

        if (TextUtils.isEmpty(txtEmail) || !Patterns.EMAIL_ADDRESS.matcher(txtEmail).matches()) {
            txt_register_email.setError("Valid e-mail is required.");
            return false;
        }

        if (TextUtils.isEmpty(txtMobile) || txtMobile.length() != 10) {
            txt_register_mobile_number.setError("Mobile No. should be 10 digits.");
            return false;
        }

        if (TextUtils.isEmpty(txt_register_farm_name.getText().toString().trim())) {
            txt_register_farm_name.setError("Garden Type is required.");
            return false;
        }

        if (TextUtils.isEmpty(txt_register_location.getText().toString().trim())) {
            txt_register_location.setError("Location is required.");
            return false;
        }

        if (TextUtils.isEmpty(txtPassword) || txtPassword.length() < 6) {
            txt_register_password.setError("Password should be at least 6 characters.");
            return false;
        }

        if (!txtPassword.equals(txtConfirmedPassword)) {
            txt_register_confirmed_password.setError("Passwords do not match.");
            return false;
        }

        return true;
    }

    private void registerUser(String txtUserName, String txtEmail, String txtMobile, String txtFarmName, String txtLocation, String txtCountry, String txtPassword) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        mAuth.createUserWithEmailAndPassword(txtEmail, txtPassword)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());

                            dbRef.child("username").setValue(txtUserName);
                            dbRef.child("email").setValue(txtEmail);
                            dbRef.child("mobile").setValue(txtMobile);
                            dbRef.child("farmName").setValue(txtFarmName);
                            dbRef.child("location").setValue(txtLocation);
                            dbRef.child("country").setValue(txtCountry);

                            Toast.makeText(ActivityRegister.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();
                            // Redirect to another activity or main page
                        } else {
                            Toast.makeText(ActivityRegister.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();
                        }
                    }
                });
    }

    private void setupLoginLink() {
        TextView already_have_an_account_link = findViewById(R.id.already_have_an_account_link);
        already_have_an_account_link.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUserToLoginActivity();
            }
        });
    }

    private void sendUserToLoginActivity() {
        Intent loginIntent = new Intent(ActivityRegister.this, ActivityLogin.class);
        startActivity(loginIntent);
    }
}
