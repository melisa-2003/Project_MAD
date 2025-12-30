package com.example.bookcatalog_asg2

import android.content.Intent
import android.os.Bundle
import android.util.Log
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

    // Use a single, consistent date formatter
    private val firestoreFormatter = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US)

    private data class EventWithDate(val event: Event, val date: Date)

    companion object {
        private const val TAG = "NotificationActivity"
    }

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

        adapter = NotificationAdapter()
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        loadAllEvents()
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
                Log.d(TAG, "Successfully fetched ${result.size()} documents from Firestore.")
                val now = Date()
                val upcoming = mutableListOf<EventWithDate>()
                val recent = mutableListOf<EventWithDate>()

                result.documents.forEach { doc ->
                    Log.d(TAG, "Doc ID: ${doc.id}, Raw dateTime value: '${doc.getString("dateTime")}'")

                    val event = doc.toObject(Event::class.java)?.copy(id = doc.id) ?: return@forEach
                    
                    val eventDate = try {
                        firestoreFormatter.parse(event.dateTime)
                    } catch (e: Exception) {
                        null
                    }

                    if (eventDate != null) {
                        if (eventDate.after(now)) {
                            upcoming.add(EventWithDate(event, eventDate))
                        } else {
                            recent.add(EventWithDate(event, eventDate))
                        }
                    } else {
                        Log.w(TAG, "Could not parse date for event '${event.title}'. Value was '${event.dateTime}'")
                    }
                }

                val finalList = mutableListOf<NotificationItem>()

                if (upcoming.isNotEmpty()) {
                    finalList.add(NotificationItem.Header("Upcoming"))
                    val sortedUpcoming = upcoming.sortedBy { it.date }
                    sortedUpcoming.forEach { finalList.add(NotificationItem.EventItem(it.event)) }
                }

                if (recent.isNotEmpty()) {
                    finalList.add(NotificationItem.Header("Recent"))
                    val sortedRecent = recent.sortedByDescending { it.date }
                    sortedRecent.forEach { finalList.add(NotificationItem.EventItem(it.event)) }
                }
                Log.d(TAG, "Submitting a final list of ${finalList.size} items to the adapter.")
                adapter.submitList(finalList)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching events from Firestore.", e)
            }
    }
}
