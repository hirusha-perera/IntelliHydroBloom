package com.example.intellihydrobloom;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.text.Editable;
import android.util.Patterns;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.regex.Matcher;

public class ActivityLoginTest {

    @Mock
    private FirebaseAuth mAuth;

    @Mock
    private FirebaseUser mUser;

    private ActivityLogin activityLogin;



    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        activityLogin = new ActivityLogin();

        // Mocking the FirebaseAuth instance inside ActivityLogin
        activityLogin.mAuth = mAuth;



        when(mAuth.getCurrentUser()).thenReturn(mUser);
    }



    @Test
    public void testLoginUser_withEmptyEmail() {

        Editable editableEmpty = mock(Editable.class);
        when(editableEmpty.toString()).thenReturn("");


        activityLogin.txt_login_email = mock(EditText.class);
        when(activityLogin.txt_login_email.getText()).thenReturn(editableEmpty);
        activityLogin.loginUser();
        verify(activityLogin.txt_login_email).setError("This field is required");
    }

    @Test
    public void testLoginUser_withInvalidEmail() {

        Editable editableInvalidEmail = mock(Editable.class);
        when(editableInvalidEmail.toString()).thenReturn("invalidEmail");
        activityLogin.txt_login_email = mock(EditText.class);
        when(activityLogin.txt_login_email.getText()).thenReturn(editableInvalidEmail);
        when(Patterns.EMAIL_ADDRESS.matcher(any(CharSequence.class))).thenReturn(mock(Matcher.class));
        activityLogin.loginUser();
        verify(activityLogin.txt_login_email).setError("Please provide valid email!");
    }


}
