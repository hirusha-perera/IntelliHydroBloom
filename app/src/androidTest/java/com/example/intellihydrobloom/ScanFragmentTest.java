package com.example.intellihydrobloom;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
public class ScanFragmentTest {

    private FragmentScenario<ScanFragment> fragmentScenario;

    @Before
    public void setUp() {
        fragmentScenario = FragmentScenario.launchInContainer(ScanFragment.class);
    }

    @Test
    public void testUIComponentsVisibility() {
        onView(withId(R.id.iv_plant_image)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_capture)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_pick_from_gallery)).check(matches(isDisplayed()));
        // Add checks for any other UI components you want to ensure are visible
    }

    // ... rest of the test cases ...

}

