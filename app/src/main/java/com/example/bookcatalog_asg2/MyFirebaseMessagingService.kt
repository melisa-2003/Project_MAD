package com.example.bookcatalog_asg2

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New FCM Token: $token")

        sendTokenToFirestore(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("FCM", "Message received in foreground. Data payload: ${remoteMessage.data}")
    }
}

    private fun sendTokenToFirestore(token: String?) {
        if (token == null) return

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            val userData = mapOf("fcmToken" to token)
            db.collection("users").document(userId)
                .update(userData)
                .addOnSuccessListener {
                    Log.d("FCM", "FCM Token successfully saved to Firestore for user: $userId")
                }
                .addOnFailureListener { e ->
                    Log.e("FCM", "Error saving FCM Token to Firestore", e)
                }
        }
    }