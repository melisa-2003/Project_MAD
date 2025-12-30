package com.example.bookcatalog_asg2

import android.app.Application
import android.content.Intent
import android.util.Log

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 在应用创建时，立即启动后台服务
        startFirestoreListenerService()
    }

    private fun startFirestoreListenerService() {
        Log.d("MyApplication", "Attempting to start FirestoreListenerService from Application class.")
        val intent = Intent(this, FirestoreListenerService::class.java)
        // 使用 this (Application Context) 来启动服务
        this.startService(intent)
    }
}