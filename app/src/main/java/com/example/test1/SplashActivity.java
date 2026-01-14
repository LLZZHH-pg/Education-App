package com.example.test1;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen =SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        splashScreen.setOnExitAnimationListener(provider -> {
            android.view.View splashView = provider.getView();
            splashView.animate()
                    .alpha(0f)
                    .setDuration(400L)
                    .withEndAction(provider::remove)
                    .start();
        });

        new Handler().postDelayed(() -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }, 200);
    }
}