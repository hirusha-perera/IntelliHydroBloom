package com.example.intellihydrobloom;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withInputType;

import android.text.InputType;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.idling.CountingIdlingResource;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ActivityLoginTest {

   //@Rule
    //public ActivityScenarioRule<ActivityLogin> activityRule = new ActivityScenarioRule<>(ActivityLogin.class);

    @Rule
    public IntentsTestRule<ActivityLogin> activityRule = new IntentsTestRule<>(ActivityLogin.class);


    // Assuming you have a CountingIdlingResource in your Activity for Firebase operations
    private CountingIdlingResource idlingResource;


    @Before
    public void setUp() {
        ActivityLogin activity = activityRule.getActivity();
        idlingResource = new CountingIdlingResource("Firebase");
        IdlingRegistry.getInstance().register(idlingResource);
    }


    @After
    public void tearDown() {
        IdlingRegistry.getInstance().unregister(idlingResource);
    }

    @Test
    public void testLoginButton_click_withEmptyFields_showsError() {
        // Click on the login button without entering any data
        onView(withId(R.id.btn_login)).perform(click());

        // Check if error message is displayed for email
        onView(withId(R.id.txt_login_email))
                .check(matches(hasErrorText("This field is required")));


    }

    @Test
    public void testEmailField_inputInvalidEmail_showsError() {
        // Type an invalid email
        onView(withId(R.id.txt_login_email))
                .perform(typeText("invalidEmail"), closeSoftKeyboard());

        // Click on the login button
        onView(withId(R.id.btn_login)).perform(click());

        // Check if error message "Please provide valid email!" is displayed
        onView(withId(R.id.txt_login_email))
                .check(matches(hasErrorText("Please provide valid email!")));

    }

    @Test
    public void testPasswordField_inputEmptyPassword_showsError() {
        // Type a valid email but leave the password field empty
        onView(withId(R.id.txt_login_email))
                .perform(typeText("test@example.com"), closeSoftKeyboard());

        // Click on the login button
        onView(withId(R.id.btn_login)).perform(click());

        // Check if error message "This field is required" is displayed for password
        onView(withId(R.id.txt_login_password))
                .check(matches(hasErrorText("This field is required")));
    }


    @Test
    public void testPasswordVisibilityToggle() {
        // Type a password
        onView(withId(R.id.txt_login_password))
                .perform(typeText("testPassword"), closeSoftKeyboard());

        // Initially, the password should be hidden
        onView(withId(R.id.txt_login_password))
                .check(matches(withInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)));

        // Click on the visibility toggle button
        onView(withId(R.id.img_show_hide_password)).perform(click());


    }


    @Test
    public void testForgotPasswordLink() {
        // Click on the "forgot password" link
        onView(withId(R.id.forget_password_link)).perform(click());

        // Check if the ActivityForgotPassword is displayed
        intended(hasComponent(ActivityForgotPassword.class.getName()));
    }

    @Test
    public void testRegisterLink() {
        // Click on the "register" link
        onView(withId(R.id.new_user_register_link)).perform(click());

        // Check if the ActivityRegister is displayed
        intended(hasComponent(ActivityRegister.class.getName()));
    }


}
