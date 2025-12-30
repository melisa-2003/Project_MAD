package com.example.bookcatalog_asg2

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationActivity : AppCompatActivity() {

    private lateinit var adapter: NotificationAdapter
    private val db = FirebaseFirestore.getInstance()
    private val firestoreFormatter = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        val backButton = findViewById<ImageButton>(R.id.backButton)
        val rv = findViewById<RecyclerView>(R.id.rv_notifications)

        backButton.setOnClickListener { goToHome() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goToHome()
            }
        })

        // 1. 在这里初始化 Adapter
        adapter = NotificationAdapter { event ->
            handleEventClick(event)
        }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        // 每次返回页面都刷新数据
        loadAllEvents()
    }

    private fun handleEventClick(event: Event) {
        val intent = Intent(this, EventDetailsActivity::class.java)
        intent.putExtra("EVENT_ID", event.id)
        startActivity(intent)
    }

    private fun goToHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    private fun loadAllEvents() {
        db.collection("events")
            .get()
            .addOnSuccessListener { result ->
                val now = Date()
                val upcoming = mutableListOf<Event>()
                val recent = mutableListOf<Event>()

                result.documents.forEach { doc ->
                    // 2. toObject 会因为 @PropertyName 正确映射 "dayTime"
                    val event = doc.toObject(Event::class.java)
                        ?.copy(id = doc.id) ?: return@forEach

                    val eventDate = try {
                        firestoreFormatter.parse(event.dateTime)
                    } catch (_: Exception) {
                        return@forEach
                    }

                    if (eventDate == null) return@forEach

                    // 过去的日期放在 "Recent"
                    if (eventDate.after(now)) {
                        upcoming.add(event)
                    } else {
                        recent.add(event)
                    }
                }

                val finalList = mutableListOf<NotificationItem>()

                if (upcoming.isNotEmpty()) {
                    finalList.add(NotificationItem.Header("Upcoming"))
                    val sortedUpcoming = upcoming.sortedBy { firestoreFormatter.parse(it.dateTime) }
                    sortedUpcoming.forEach { finalList.add(NotificationItem.EventItem(it)) }
                }

                if (recent.isNotEmpty()) {
                    finalList.add(NotificationItem.Header("Recent"))
                    val sortedRecent = recent.sortedByDescending { firestoreFormatter.parse(it.dateTime) }
                    sortedRecent.forEach { finalList.add(NotificationItem.EventItem(it)) }
                }

                adapter.submitList(finalList)
            }
            .addOnFailureListener {
                // Handle error
            }
    }
}


