package com.example.bookcatalog_asg2

import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class EventDetailsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_details)

        // Views
        val backButton = findViewById<ImageButton>(R.id.backButton)
        val shareButton = findViewById<ImageView>(R.id.btn_share)
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
        val eventId = intent.getStringExtra("EVENT_ID")
        if (eventId.isNullOrBlank()) {
            Toast.makeText(this, "Invalid event", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load event
        db.collection("events").document(eventId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

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
}
