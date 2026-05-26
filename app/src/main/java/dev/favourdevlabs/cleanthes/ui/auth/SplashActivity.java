package dev.favourdevlabs.cleanthes.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import dev.favourdevlabs.cleanthes.R;

public class SplashActivity extends AppCompatActivity {

    // 1.8 seconds — enough to read the quote; short enough to respect the user's time
    private static final int SPLASH_DURATION_MS = 1800;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Must be the very first call — before super.onCreate()
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Seal the back button during the splash — there is no retreat
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Stoic: accept what comes. No escape.
            }
        });

        // After the duration, hand control to SetupActivity.
        // SetupActivity already knows whether to show Login or first-time Setup.
        handler.postDelayed(this::proceed, SPLASH_DURATION_MS);
    }

    private void proceed() {
        Intent intent = new Intent(SplashActivity.this, SetupActivity.class);
        startActivity(intent);
        // Fade — no slam-cut. Controlled transition.
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove pending callbacks to prevent leaks if the activity is destroyed early
        handler.removeCallbacksAndMessages(null);
    }
}
