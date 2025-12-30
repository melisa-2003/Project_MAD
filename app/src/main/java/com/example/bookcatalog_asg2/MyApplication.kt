package com.example.bookcatalog_asg2

import android.app.Application
import android.content.Intent
import android.util.Log

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startFirestoreListenerService()
    }

    private fun startFirestoreListenerService() {
        Log.d("MyApplication", "Attempting to start FirestoreListenerService.")
        val intent = Intent(this, FirestoreListenerService::class.java)
        // On modern Android, we must use startForegroundService to run tasks in the background.
        this.startForegroundService(intent)
    }
}
