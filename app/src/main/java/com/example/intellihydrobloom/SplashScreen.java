package com.example.intellihydrobloom;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        ImageView logo = findViewById(R.id.splash_logo);

        // Load the animation from the XML
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new android.view.animation.DecelerateInterpolator());
        fadeIn.setDuration(2000);

        logo.setAnimation(fadeIn);

        // Navigate to MainActivity after a delay
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashScreen.this, ActivityLogin.class));
            finish();
        }, 4000);  // Delay for 4 seconds. 2 seconds for the animation and 2 seconds of showing the fully opaque logo.
    }
}
