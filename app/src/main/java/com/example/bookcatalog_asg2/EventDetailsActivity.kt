package com.example.bookcatalog_asg2

import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.*

class EventDetailsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var isBookmarked = false
    private var isRegistered = false
    private var eventId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_details)

        // Views
        val backButton = findViewById<ImageButton>(R.id.backButton)
        val shareButton = findViewById<ImageView>(R.id.btn_share)
        val bookmarkButton = findViewById<ImageView>(R.id.btn_bookmark)
        val registerButton = findViewById<MaterialButton>(R.id.btn_register)
        val addToCalendarBtn = findViewById<MaterialButton>(R.id.btn_add_to_calendar)

        val bannerImage = findViewById<ImageView>(R.id.iv_event_banner)
        val categoryTag = findViewById<TextView>(R.id.tv_category_tag)
        val titleText = findViewById<TextView>(R.id.tv_event_title)
        val dateTimeText = findViewById<TextView>(R.id.tv_date_time)
        val venueText = findViewById<TextView>(R.id.tv_venue)
        val organizerText = findViewById<TextView>(R.id.tv_organizer)
        val overviewText = findViewById<TextView>(R.id.tv_overview_description)
        val highlightsText = findViewById<TextView>(R.id.tv_highlights_description)

        backButton.setOnClickListener { finish() }

        // Get event ID
        eventId = intent.getStringExtra("EVENT_ID") ?: ""
        if (eventId.isBlank()) {
            Toast.makeText(this, "Invalid event", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load user interaction status (Bookmark/Register)
        checkUserStatus(bookmarkButton, registerButton)

        // Load event
        db.collection("events").document(eventId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                incrementViewCount(eventId)

                val title = doc.getString("title")
                val category = doc.getString("category")
                val dateTime = doc.getString("dateTime")
                val venue = doc.getString("venue")
                val organizer = doc.getString("organizerName")
                val overview = doc.getString("overview")
                val highlights = doc.getString("highlights")
                val imageRef = doc.getString("imageRef")

                // Bind data
                titleText.text = title.orEmpty()
                categoryTag.text = category.orEmpty()
                dateTimeText.text = dateTime.orEmpty()
                venueText.text = venue.orEmpty()
                organizerText.text = organizer.orEmpty()
                overviewText.text = overview.orEmpty()
                highlightsText.text = highlights.orEmpty()

                // Image
                if (!imageRef.isNullOrBlank()) {
                    Glide.with(this)
                        .load(imageRef)
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .into(bannerImage)
                } else {
                    bannerImage.setImageResource(R.drawable.ic_launcher_background)
                }

                // Share
                shareButton.setOnClickListener {
                    shareEvent(title, dateTime, venue)
                }

                // Add to Calendar
                addToCalendarBtn.setOnClickListener {
                    addToCalendar(title, dateTime, venue, overview)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load event", Toast.LENGTH_SHORT).show()
            }

        // Handle Bookmark Click
        bookmarkButton.setOnClickListener {
            toggleBookmark(bookmarkButton)
        }

        // Handle Register Click
        registerButton.setOnClickListener {
            toggleRegistration(registerButton)
        }
    }

    private fun checkUserStatus(bookmarkBtn: ImageView, registerBtn: MaterialButton) {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid)
            .collection("myEvents").document(eventId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    isBookmarked = doc.getBoolean("bookmarked") == true
                    isRegistered = doc.getBoolean("registered") == true
                }
                updateBookmarkUI(bookmarkBtn)
                updateRegisterUI(registerBtn)
            }
    }

    private fun toggleBookmark(bookmarkBtn: ImageView) {
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        isBookmarked = !isBookmarked
        updateBookmarkUI(bookmarkBtn)

        val data = mapOf("bookmarked" to isBookmarked, "timestamp" to FieldValue.serverTimestamp())
        db.collection("users").document(uid)
            .collection("myEvents").document(eventId)
            .set(data, SetOptions.merge())
    }

    private fun toggleRegistration(registerBtn: MaterialButton) {
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        isRegistered = !isRegistered
        updateRegisterUI(registerBtn)

        val data = mapOf("registered" to isRegistered, "timestamp" to FieldValue.serverTimestamp())
        db.collection("users").document(uid)
            .collection("myEvents").document(eventId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                val msg = if (isRegistered) "Registered successfully" else "Registration cancelled"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateBookmarkUI(btn: ImageView) {
        if (isBookmarked) {
            btn.setColorFilter(ContextCompat.getColor(this, R.color.orange_accent))
        } else {
            btn.setColorFilter(ContextCompat.getColor(this, android.R.color.black))
        }
    }

    private fun updateRegisterUI(btn: MaterialButton) {
        if (isRegistered) {
            btn.text = "Registered"
            btn.isEnabled = false // Or allow cancel?
            // The requirement says "The user can cancel registration... in MyEventsFragment"
            // But usually you can toggle here too.
            // For now, let's keep it toggleable or just indicate state.
            // If we want to allow cancel here, we keep enabled.
            btn.isEnabled = true
            btn.text = "Cancel Registration"
            btn.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        } else {
            btn.text = getString(R.string.register_now)
            btn.isEnabled = true
            btn.setBackgroundColor(ContextCompat.getColor(this, R.color.orange_accent))
        }
    }

    // ------------ MOST CLICKED EVENT = TRENDING NOW EVENTS ----------------
    private fun incrementViewCount(eventId: String) {
        db.collection("events")
            .document(eventId)
            .update("viewCount", FieldValue.increment(1))
    }

    // ---------------- SHARE ----------------
    private fun shareEvent(title: String?, dateTime: String?, venue: String?) {
        val shareText = buildString {
            append(title ?: "Event")
            if (!dateTime.isNullOrEmpty()) append("\nDate & Time: $dateTime")
            if (!venue.isNullOrEmpty()) append("\nVenue: $venue")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        startActivity(Intent.createChooser(intent, "Share Event"))
    }

    // ---------------- ADD TO CALENDAR ----------------
    private fun addToCalendar(
        title: String?,
        dateTime: String?,
        venue: String?,
        description: String?
    ) {
        if (dateTime.isNullOrBlank()) {
            Toast.makeText(this, "Event date not available", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val startDate = format.parse(dateTime) ?: return
            val startMillis = startDate.time
            val endMillis = startMillis + 60 * 60 * 1000 // 1 hour

            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.Events.EVENT_LOCATION, venue)
                putExtra(CalendarContract.Events.DESCRIPTION, description)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            }

            startActivity(intent)

        } catch (_: Exception) {
            Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun start(context: android.content.Context, eventId: String) {
            val intent = Intent(context, EventDetailsActivity::class.java).apply {
                putExtra("EVENT_ID", eventId)
            }
            context.startActivity(intent)
        }
    }
}
