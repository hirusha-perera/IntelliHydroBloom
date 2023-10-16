package com.example.intellihydrobloom;

import android.text.TextUtils;
import android.util.Patterns;

public class RegistrationValidation {

    public boolean isValidUserName(String userName) {
        return !TextUtils.isEmpty(userName);
    }

    public boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public boolean isValidMobile(String mobile) {
        return !TextUtils.isEmpty(mobile) && mobile.length() == 10;
    }

    public boolean isValidFarmName(String farmName) {
        return !TextUtils.isEmpty(farmName);
    }

    public boolean isValidLocation(String location) {
        return !TextUtils.isEmpty(location);
    }

    public boolean isValidPassword(String password) {
        return !TextUtils.isEmpty(password) && password.length() >= 6;
    }

    public boolean isPasswordConfirmed(String password, String confirmedPassword) {
        return password.equals(confirmedPassword);
    }
}
