package com.map.buscity

import android.app.Application
import com.google.firebase.FirebaseApp
import android.util.Log

/**
 * Application class for BusCity app
 * Initializes Firebase on app startup
 */
class BusCityApplication : Application() {
    companion object {
        private const val TAG = "BusCityApplication"
    }

    override fun onCreate() {
        super.onCreate()
        
        try {
            // Initialize Firebase (auto-configured via google-services.json)
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "✅ Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase initialization failed", e)
        }
    }
}
