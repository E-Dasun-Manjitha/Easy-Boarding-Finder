package com.easyboardingfinder

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class EasyBoardingFinderApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var startedActivitiesCount = 0

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {
                startedActivitiesCount++
            }

            override fun onActivityResumed(activity: Activity) {}

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                startedActivitiesCount--
                if (startedActivitiesCount == 0) {
                    // App went to background — save timestamp
                    val sharedPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                    sharedPrefs.edit().putLong("last_active_time", System.currentTimeMillis()).apply()
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {}
        })

        if (USE_EMULATOR) {
            setupEmulators()
        }
    }

    private fun setupEmulators() {
        Log.d(TAG, "Configuring Firebase Emulators...")
        try {
            // Android emulator loopback host is 10.0.2.2
            val host = "10.0.2.2"

            // 1. Auth Emulator (Port 9099)
            FirebaseAuth.getInstance().useEmulator(host, 9099)
            Log.d(TAG, "Auth Emulator connected to $host:9099")

            // 2. Firestore Emulator (Port 8080)
            FirebaseFirestore.getInstance().useEmulator(host, 8080)
            Log.d(TAG, "Firestore Emulator connected to $host:8080")

            // 3. Cloud Storage Emulator (Port 9199)
            FirebaseStorage.getInstance().useEmulator(host, 9199)
            Log.d(TAG, "Storage Emulator connected to $host:9199")

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up Firebase Emulators: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "EasyBoardingApp"

        lateinit var instance: EasyBoardingFinderApplication
            private set
        
        /**
         * Set this to true to redirect Firebase traffic to a locally running
         * Firebase Emulator Suite on your development PC.
         * 
         * Note:
         * - If running on the standard Android Emulator (AVD), the host "10.0.2.2" is correct.
         * - If running on Genymotion, change host to "10.0.3.2".
         * - If running on a physical Android device, change host to your PC's local IP address
         *   (e.g., "192.168.1.100") and ensure your device is connected to the same Wi-Fi network.
         */
        private const val USE_EMULATOR = false
    }
}
