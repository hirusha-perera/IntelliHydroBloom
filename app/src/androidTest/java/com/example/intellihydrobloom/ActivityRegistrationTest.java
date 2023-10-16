package com.example.intellihydrobloom;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.view.View;
import android.widget.EditText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ActivityRegistrationTest {

    @Rule
    public ActivityScenarioRule<ActivityRegister> activityScenarioRule = new ActivityScenarioRule<>(ActivityRegister.class);

    public static Matcher<View> hasErrorText(final String expectedError) {
        return new TypeSafeMatcher<View>() {
            @Override
            public boolean matchesSafely(View view) {
                if (!(view instanceof EditText)) {
                    return false;
                }
                EditText editText = (EditText) view;
                return expectedError.equals(editText.getError());
            }
            @Override
            public void describeTo(Description description) {
                description.appendText("with error: " + expectedError);
            }
        };
    }

    @Test
    public void testEmptyUserNameShowsError() {
        onView(withId(R.id.txt_register_user_name)).perform(clearText(), click());
        onView(withId(R.id.btn_register)).perform(scrollTo(), click());
        onView(withId(R.id.txt_register_user_name)).check(matches(hasErrorText("Your Name is required.")));

    }


    @Test
    public void testEmptyEmailShowsError() {
        onView(withId(R.id.txt_register_user_name)).perform(typeText("John"));
        onView(withId(R.id.txt_register_email)).perform(clearText(), click());
        onView(withId(R.id.btn_register)).perform(scrollTo(), click());
        onView(withId(R.id.txt_register_email)).check(matches(hasErrorText("Valid e-mail is required.")));
    }

    @Test
    public void testInvalidEmailShowsError() {
        onView(withId(R.id.txt_register_user_name)).perform(typeText("John"));
        onView(withId(R.id.txt_register_email)).perform(typeText("invalidEmail"), click());
        onView(withId(R.id.btn_register)).perform(scrollTo(), click());
        onView(withId(R.id.txt_register_email)).check(matches(hasErrorText("Valid e-mail is required.")));

    }


    @Test
    public void testShortMobileNumberShowsError() {
        onView(withId(R.id.txt_register_user_name)).perform(typeText("John"));
        onView(withId(R.id.txt_register_email)).perform(typeText("abc@gmail.com"));
        onView(withId(R.id.txt_register_mobile_number)).perform(typeText("12345"), click());
        onView(withId(R.id.btn_register)).perform(scrollTo(), click());
        onView(withId(R.id.txt_register_mobile_number)).check(matches(hasErrorText("Mobile No. should be 10 digits.")));
    }


    @Test
    public void testEmptyFarmNameShowsError() {
        onView(withId(R.id.txt_register_user_name)).perform(typeText("John"));
        onView(withId(R.id.txt_register_email)).perform(typeText("abc@gmail.com"));
        onView(withId(R.id.txt_register_mobile_number)).perform(typeText("1234567890"));
        onView(withId(R.id.txt_register_farm_name)).perform(clearText(), click());
        onView(withId(R.id.btn_register)).perform(scrollTo(), click());
        onView(withId(R.id.txt_register_farm_name)).check(matches(hasErrorText("Garden Type is required.")));
    }

    @Test
    public void testEmptyLocationShowsError() {
        onView(withId(R.id.txt_register_user_name)).perform(typeText("John"));
        onView(withId(R.id.txt_register_email)).perform(typeText("abc@gmail.com"));
        onView(withId(R.id.txt_register_mobile_number)).perform(typeText("1234567890"));
        onView(withId(R.id.txt_register_farm_name)).perform(typeText("Roof"), closeSoftKeyboard());
        onView(withId(R.id.txt_register_location)).perform(clearText(), click());
        onView(withId(R.id.btn_register)).perform(scrollTo(), click());
        onView(withId(R.id.txt_register_location)).check(matches(hasErrorText("Location is required.")));
    }

    @Test
    public void testShortPasswordShowsError() {
        onView(withId(R.id.txt_register_user_name)).perform(typeText("John"));
        onView(withId(R.id.txt_register_email)).perform(typeText("abc@gmail.com"));
        onView(withId(R.id.txt_register_mobile_number)).perform(typeText("1234567890"));
        onView(withId(R.id.txt_register_farm_name)).perform(typeText("Roof"), closeSoftKeyboard());
        onView(withId(R.id.txt_register_location)).perform(typeText("Colombo"), closeSoftKeyboard());
        onView(withId(R.id.txt_register_password)).perform(typeText("pass"), click(), closeSoftKeyboard());
        onView(withId(R.id.btn_register)).perform(scrollTo(), click());
        onView(withId(R.id.txt_register_password)).check(matches(hasErrorText("Password should be at least 6 characters.")));
    }

    @Test
    public void testMismatchedPasswordsShowsError() {
        onView(withId(R.id.txt_register_user_name)).perform(typeText("John"));
        onView(withId(R.id.txt_register_email)).perform(typeText("abc@gmail.com"));
        onView(withId(R.id.txt_register_mobile_number)).perform(typeText("1234567890"));
        onView(withId(R.id.txt_register_farm_name)).perform(typeText("Roof"), closeSoftKeyboard());
        onView(withId(R.id.txt_register_location)).perform(typeText("Colombo"), closeSoftKeyboard());
        onView(withId(R.id.txt_register_password)).perform(typeText("password123"), click(), closeSoftKeyboard());
        onView(withId(R.id.txt_register_confirmed_password)).perform(typeText("password124"), click(), closeSoftKeyboard());
        onView(withId(R.id.btn_register)).perform(scrollTo(), click());
        onView(withId(R.id.txt_register_confirmed_password)).check(matches(hasErrorText("Passwords do not match.")));
    }

    // ... any additional tests if needed

}


