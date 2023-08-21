package com.jivanpun.suitcaseapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

public class SplashPage extends AppCompatActivity {

    private RelativeLayout relativeLayout;
    private Animation layoutAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_page);

        // Load animation from resources
        layoutAnimation = AnimationUtils.loadAnimation(SplashPage.this, R.anim.bottom_to_top);
        // Initialize RelativeLayout
        relativeLayout = findViewById(R.id.splash);

        // Check if the user is already logged in
        boolean loggedIn = false;
        Intent intent = getIntent();
        if (intent != null) {
            loggedIn = intent.getBooleanExtra("loggedIn", false);
        }

        if (loggedIn) {
            // If user is already logged in, navigate to Home Page
            Intent homeIntent = new Intent(SplashPage.this, HomePage.class);
            startActivity(homeIntent);
            finish();
        } else {
            // If not logged in, show splash animation and navigate to Login Page after a delay
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Make RelativeLayout visible and apply animation
                    relativeLayout.setVisibility(View.VISIBLE);
                    relativeLayout.setAnimation(layoutAnimation);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // Navigate to the Login Page (MainActivity)
                            Intent mainIntent = new Intent(SplashPage.this, MainActivity.class);
                            startActivity(mainIntent);
                            finish();
                        }
                    }, 2800); // Delay before navigating to the Login Page
                }
            }, 300); // Initial delay before showing the splash animation
        }
    }
}
