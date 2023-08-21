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

    RelativeLayout relativeLayout;
    Animation layoutAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_page);

        layoutAnimation = AnimationUtils.loadAnimation(SplashPage.this, R.anim.bottom_to_top);
        relativeLayout = findViewById(R.id.splash);

        boolean loggedIn = false;
        Intent intent = getIntent();
        if (intent != null) {
            loggedIn = intent.getBooleanExtra("loggedIn", false);
        }

        if (loggedIn) {
            Intent homeIntent = new Intent(SplashPage.this, HomePage.class);
            startActivity(homeIntent);
            finish();
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    relativeLayout.setVisibility(View.VISIBLE);
                    relativeLayout.setAnimation(layoutAnimation);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent mainIntent = new Intent(SplashPage.this, MainActivity.class);
                            startActivity(mainIntent);
                            finish();
                        }
                    }, 2800);
                }
            }, 300);
        }
    }
}
