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
    private var isFirstLoad = true

    companion object {
        // 1. 修改 Channel ID，为服务本身和新事件通知创建不同的 Channel
        const val FOREGROUND_CHANNEL_ID = "firestore_listener_channel"
        const val NEW_EVENT_CHANNEL_ID = "new_event_channel"
        private const val PREFS_NAME = "app_notifications"
        private const val BADGE_COUNT_KEY = "badge_count"
        private const val FOREGROUND_SERVICE_NOTIFICATION_ID = 1 // 给前台服务一个固定的通知ID

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
        createNotificationChannels() // 修改为创建多个 Channel
        startListeningForEvents()
    }

    // 2. 修改为启动前台服务
    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createForegroundNotification()
        startForeground(FOREGROUND_SERVICE_NOTIFICATION_ID, notification)
        Log.d("FirestoreListener", "Service started as a foreground service.")
        // 使用 START_STICKY，如果服务被杀死，系统会尝试重启它
        return START_STICKY
    }

    // 3. 创建前台服务的常驻通知
    private fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setContentTitle("Event Hub is active")
            .setContentText("Listening for new event notifications in the background.")
            .setSmallIcon(R.drawable.notification) // 请确保这个图标存在
            .setPriority(NotificationCompat.PRIORITY_LOW) // 低优先级，避免打扰用户
            .build()
    }

    // 4. 修改为创建两个通知渠道
    private fun createNotificationChannels() {
        // 前台服务的通知渠道
        val foregroundChannel = NotificationChannel(
            FOREGROUND_CHANNEL_ID,
            "Background Service",
            NotificationManager.IMPORTANCE_LOW // 低重要性
        )
        // 新事件的通知渠道
        val newEventChannel = NotificationChannel(
            NEW_EVENT_CHANNEL_ID,
            "New Event Notifications",
            NotificationManager.IMPORTANCE_HIGH // 高重要性
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(foregroundChannel)
        manager.createNotificationChannel(newEventChannel)
    }

    private fun sendNewEventNotification(event: Event) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, NEW_EVENT_CHANNEL_ID) // 使用新事件的Channel ID
            .setSmallIcon(R.drawable.notification)
            .setContentTitle("✨ New Event Published!")
            .setContentText("Check out the new event: ${event.title}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    // --- 以下是其他未改变的方法 ---

    override fun onDestroy() {
        super.onDestroy()
        eventListener?.remove()
        Log.d("FirestoreListener", "Service destroyed, listener removed.")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startListeningForEvents() {
        Log.d("FirestoreListener", "Starting to listen for event changes...")
        eventListener = db.collection("events")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("FirestoreListener", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (isFirstLoad) {
                    isFirstLoad = false
                    Log.d("FirestoreListener", "Initial data loaded. Ready for new events.")
                    return@addSnapshotListener
                }

                for (dc in snapshots!!.documentChanges) {
                    val event = dc.document.toObject(Event::class.java).copy(id = dc.document.id)
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> {
                            Log.d("FirestoreListener", "New event detected: ${event.title}")
                            handleNewEvent(event)
                        }
                        DocumentChange.Type.REMOVED -> {
                            Log.d("FirestoreListener", "Event removed: ${event.title}")
                            handleRemovedEvent(event)
                        }
                        else -> { /* Do nothing for MODIFIED */ }
                    }
                }
            }
    }

    private fun handleNewEvent(event: Event) {
        // --- 新增的调试日志 ---    Log.d("FirestoreListener", "handleNewEvent triggered for event: ${event.title}")
        Log.d("FirestoreListener", "Event dateTime string is: '${event.dateTime}'")

        val isUpcoming = isEventUpcoming(event.dateTime)
        // --- 新增的调试日志 ---
        Log.d("FirestoreListener", "isEventUpcoming result: $isUpcoming")

        if (!isUpcoming) {
            Log.d("FirestoreListener", "Event '${event.title}' is not upcoming. Skipping.")
            return
        }

        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Log.d("FirestoreListener", "No user logged in, skipping notification.")
            return
        }

        updateBadgeCount(1)

        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { userDocument ->
                val role = userDocument.getString("role")
                if (role != "admin") {
                    Log.d("FirestoreListener", "Current user is a Student. Sending notification.")
                    sendNewEventNotification(event)
                } else {
                    Log.d("FirestoreListener", "User is an Admin. Badge updated, notification popup suppressed.")
                }
            }
            .addOnFailureListener {
                Log.e("FirestoreListener", "Failed to get user role.", it)
            }
    }

    private fun handleRemovedEvent(event: Event) {
        val isUpcoming = isEventUpcoming(event.dateTime)
        if (isUpcoming) {
            updateBadgeCount(-1)
        } else {
            Log.d("FirestoreListener", "Removed event '${event.title}' was not upcoming. Badge count unaffected.")
        }
    }

    private fun isEventUpcoming(dateTimeString: String): Boolean {
        if (dateTimeString.isBlank()) {
            return false
        }
        val format = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US)
        return try {
            val eventDate = format.parse(dateTimeString)
            eventDate?.after(Date()) ?: false
        } catch (e: Exception) {
            Log.e("FirestoreListener", "Failed to parse date string: $dateTimeString", e)
            false
        }
    }

    @SuppressLint("ApplySharedPref")
    private fun updateBadgeCount(change: Int) {
        val sharedPref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val currentTotal = sharedPref.getInt(BADGE_COUNT_KEY, 0)
        val newTotal = maxOf(0, currentTotal + change)

        sharedPref.edit(commit = true) {
            putInt(BADGE_COUNT_KEY, newTotal)
        }

        if (change > 0) {
            Log.d("FirestoreListener", "Badge count incremented. New total: $newTotal")
        } else if (change < 0) {
            Log.d("FirestoreListener", "Badge count decremented. New total: $newTotal")
        }
    }
}

