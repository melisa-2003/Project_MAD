package com.example.bookcatalog_asg2

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class NotificationActivity : AppCompatActivity() {

    private lateinit var adapter: NotificationAdapter
    private val db = FirebaseFirestore.getInstance()

    private val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        val backButton = findViewById<ImageButton>(R.id.backButton)
        val rv = findViewById<RecyclerView>(R.id.rv_notifications)

        // Custom back button → Home
        backButton.setOnClickListener {
            goToHome()
        }

        // System back gesture / hardware back → Home
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    goToHome()
                }
            }
        )

        adapter = NotificationAdapter()
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

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

                val now = LocalDateTime.now()
                val upcoming = mutableListOf<Event>()
                val recent = mutableListOf<Event>()

                result.documents.forEach { doc ->
                    val event = doc.toObject(Event::class.java)
                        ?.copy(id = doc.id) ?: return@forEach

                    val eventDateTime = try {
                        LocalDateTime.parse(event.dateTime, formatter)
                    } catch (_: DateTimeParseException) {
                        return@forEach
                    }

                    if (eventDateTime.isAfter(now) || eventDateTime.isEqual(now)) {
                        upcoming.add(event)
                    } else {
                        recent.add(event)
                    }
                }

                val finalList = mutableListOf<NotificationItem>()

                if (upcoming.isNotEmpty()) {
                    finalList.add(NotificationItem.Header("Upcoming"))
                    upcoming.sortedBy {
                        LocalDateTime.parse(it.dateTime, formatter)
                    }.forEach {
                        finalList.add(NotificationItem.EventItem(it))
                    }
                }

                if (recent.isNotEmpty()) {
                    finalList.add(NotificationItem.Header("Recent"))
                    recent.sortedByDescending {
                        LocalDateTime.parse(it.dateTime, formatter)
                    }.forEach {
                        finalList.add(NotificationItem.EventItem(it))
                    }
                }

                adapter.submitList(finalList)
            }
    }
}




