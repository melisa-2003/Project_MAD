package com.example.bookcatalog_asg2

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirestoreListenerService : Service() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var eventListener: ListenerRegistration? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    companion object {
        const val FOREGROUND_CHANNEL_ID = "firestore_listener_channel"
        const val NEW_EVENT_CHANNEL_ID = "new_event_channel"
        private const val PREFS_NAME = "app_notifications"
        private const val BADGE_COUNT_KEY = "badge_count"
        private const val FOREGROUND_SERVICE_NOTIFICATION_ID = 1

        fun resetBadgeCount(context: Context) {
            val sharedPref = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            sharedPref.edit {
                putInt(BADGE_COUNT_KEY, 0)
            }
            Log.d("FirestoreListener", "Badge count reset to 0 and committed.")
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        // Set up the authentication state listener
        setupAuthStateListener()
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createForegroundNotification()
        startForeground(FOREGROUND_SERVICE_NOTIFICATION_ID, notification)
        Log.d("FirestoreListener", "Service started as a foreground service.")
        return START_STICKY
    }

    private fun setupAuthStateListener() {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                // User is logged in, start listening for events
                Log.d("FirestoreListener", "User is authenticated. Starting Firestore listener.")
                startListeningForEvents()
            } else {
                // User is logged out, stop listening
                Log.d("FirestoreListener", "User is not authenticated. Stopping Firestore listener.")
                eventListener?.remove()
            }
        }
        auth.addAuthStateListener(authStateListener!!)
    }

    private fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setContentTitle("Event Hub is active")
            .setContentText("Listening for new event notifications in the background.")
            .setSmallIcon(R.drawable.notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannels() {
        val foregroundChannel = NotificationChannel(FOREGROUND_CHANNEL_ID, "Background Service", NotificationManager.IMPORTANCE_LOW)
        val newEventChannel = NotificationChannel(NEW_EVENT_CHANNEL_ID, "New Event Notifications", NotificationManager.IMPORTANCE_HIGH)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(foregroundChannel)
        manager.createNotificationChannel(newEventChannel)
    }

    private fun sendNewEventNotification(event: Event) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, NEW_EVENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.notification)
            .setContentTitle("✨ New Event Published!")
            .setContentText("Check out the new event: ${event.title}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up listeners
        authStateListener?.let { auth.removeAuthStateListener(it) }
        eventListener?.remove()
        Log.d("FirestoreListener", "Service destroyed, all listeners removed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startListeningForEvents() {
        // Ensure previous listener is removed before starting a new one
        eventListener?.remove()
        Log.d("FirestoreListener", "Starting to listen for event changes...")
        eventListener = db.collection("events")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("FirestoreListener", "Listen failed.", e)
                    return@addSnapshotListener
                }

                for (dc in snapshots!!.documentChanges) {
                    if (dc.type == DocumentChange.Type.ADDED) {
                        val event = dc.document.toObject(Event::class.java).copy(id = dc.document.id)
                        Log.d("FirestoreListener", "New event detected: ${event.title}")
                        handleNewEvent(event)
                    }
                }
            }
    }

    private fun handleNewEvent(event: Event) {
        if (!isEventUpcoming(event.dateTime)) {
            Log.d("FirestoreListener", "Event '${event.title}' is not upcoming. Skipping.")
            return
        }

        val currentUserId = auth.currentUser?.uid
        // Do not notify if there is no user or if the user created the event themselves
        if (currentUserId == null || event.createdBy == currentUserId) {
            Log.d("FirestoreListener", "Skipping notification: User is creator or not logged in.")
            return
        }
        
        // We don't need a role check if we are already skipping the creator.
        // Students will get the notification, and admins won't get it for events they create.
        Log.d("FirestoreListener", "User is not the creator. Updating badge and sending notification.")
        incrementBadgeCount()
        sendNewEventNotification(event)
    }

    private fun isEventUpcoming(dateTimeString: String): Boolean {
        if (dateTimeString.isBlank()) return false
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US)
            format.parse(dateTimeString.trim())?.after(Date()) ?: false
        } catch (e: Exception) {
            Log.e("FirestoreListener", "Failed to parse date string: $dateTimeString", e)
            false
        }
    }

    private fun incrementBadgeCount() {
        val sharedPref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val currentTotal = sharedPref.getInt(BADGE_COUNT_KEY, 0)
        val newTotal = currentTotal + 1

        sharedPref.edit { putInt(BADGE_COUNT_KEY, newTotal) }
        Log.d("FirestoreListener", "Badge count updated. New total: $newTotal")
    }
}
