package com.easyboardingfinder.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import androidx.appcompat.app.AppCompatActivity
import com.easyboardingfinder.databinding.ActivitySplashBinding
import com.easyboardingfinder.ui.auth.LoginActivity
import com.easyboardingfinder.ui.main.MainActivity
import com.google.firebase.auth.FirebaseAuth
import android.widget.Toast

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Animate logo - scale up + fade in
        val scaleAnimation = ScaleAnimation(
            0.5f, 1.0f, 0.5f, 1.0f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 800
            fillAfter = true
        }

        val fadeAnimation = AlphaAnimation(0f, 1f).apply {
            duration = 800
            fillAfter = true
        }

        val animationSet = AnimationSet(true).apply {
            addAnimation(scaleAnimation)
            addAnimation(fadeAnimation)
        }

        binding.ivLogo.startAnimation(animationSet)

        // Animate app name - fade in with delay
        val nameAnimation = AlphaAnimation(0f, 1f).apply {
            duration = 600
            startOffset = 400
            fillAfter = true
        }
        binding.tvAppName.startAnimation(nameAnimation)

        // Navigate after delay - go to Main if logged in and not timed out, else Login
        Handler(Looper.getMainLooper()).postDelayed({
            val auth = FirebaseAuth.getInstance()
            val sharedPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            val lastActiveTime = sharedPrefs.getLong("last_active_time", 0)
            val currentTime = System.currentTimeMillis()

            // Timeout limit: 15 minutes = 15 * 60 * 1000 milliseconds = 900,000 ms
            val timeoutLimit = 15 * 60 * 1000

            val isTimedOut = lastActiveTime > 0 && (currentTime - lastActiveTime > timeoutLimit)

            if (isTimedOut && auth.currentUser != null) {
                auth.signOut()
                Toast.makeText(this, "Session expired due to inactivity. Please log in again.", Toast.LENGTH_LONG).show()
                sharedPrefs.edit().putLong("last_active_time", 0).apply()
            } else if (auth.currentUser != null) {
                // If they are logged in and not timed out, update active time to now
                sharedPrefs.edit().putLong("last_active_time", currentTime).apply()
            }

            val destination = if (auth.currentUser != null) {
                Intent(this, MainActivity::class.java)
            } else {
                Intent(this, LoginActivity::class.java)
            }
            startActivity(destination)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 2500)
    }
}
