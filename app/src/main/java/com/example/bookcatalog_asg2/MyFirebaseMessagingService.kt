package com.example.bookcatalog_asg2


import android.util.Log
import androidx.core.content.edit
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

        Log.d("FCM", "Message received! Data payload: ${remoteMessage.data}")


        if (remoteMessage.data.isNotEmpty()) {

            Log.d("FCM", "Message has data. Incrementing badge count...")
            incrementBadgeCount()
        }
    }

    private fun incrementBadgeCount() {

        val sharedPref = getSharedPreferences("app_notifications", MODE_PRIVATE)


        val currentCount = sharedPref.getInt("badge_count", 0)


        sharedPref.edit {
            putInt("badge_count", currentCount + 1)
        }

        Log.d("FCM", "Badge count incremented. New count: ${currentCount + 1}")
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