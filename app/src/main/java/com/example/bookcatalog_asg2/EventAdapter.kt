package com.example.bookcatalog_asg2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bookcatalog_asg2.databinding.ItemEventCardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FieldValue

class EventAdapter(
    private val onEventClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    private val allEvents = mutableListOf<Event>()
    private val displayedEvents = mutableListOf<Event>()
    private val bookmarkedEventIds = mutableSetOf<String>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    class EventViewHolder(val binding: ItemEventCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = displayedEvents[position]

        holder.binding.apply {
            tvEventTitle.text = event.title
            tvDateTime.text = event.dateTime
            tvVenue.text = event.venue
            tvCategoryTag.text = event.category
            tvOrganizer.text = event.organizerName

            // Load image
            if (event.imageRef.isNotBlank()) {
                Glide.with(root.context)
                    .load(event.imageRef)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(ivEventImage)
            } else {
                ivEventImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            // Bookmark Status
            if (bookmarkedEventIds.contains(event.id)) {
                ivSave.setColorFilter(ContextCompat.getColor(root.context, R.color.orange_accent))
            } else {
                ivSave.setColorFilter(ContextCompat.getColor(root.context, android.R.color.black))
            }

            // Bookmark Click Listener
            ivSave.setOnClickListener {
                toggleBookmark(event, ivSave)
            }

            // Click listeners
            root.setOnClickListener { onEventClick(event) }
            btnViewDetails.setOnClickListener { onEventClick(event) }
        }
    }

    private fun toggleBookmark(event: Event, bookmarkBtn: android.widget.ImageView) {
        val uid = auth.currentUser?.uid ?: return
        val isCurrentlyBookmarked = bookmarkedEventIds.contains(event.id)
        val newStatus = !isCurrentlyBookmarked

        if (newStatus) {
            bookmarkedEventIds.add(event.id)
            bookmarkBtn.setColorFilter(ContextCompat.getColor(bookmarkBtn.context, R.color.orange_accent))
        } else {
            bookmarkedEventIds.remove(event.id)
            bookmarkBtn.setColorFilter(ContextCompat.getColor(bookmarkBtn.context, android.R.color.black))
        }

        // Update Firestore
        val data = mapOf("bookmarked" to newStatus, "timestamp" to FieldValue.serverTimestamp())
        db.collection("users").document(uid)
            .collection("myEvents").document(event.id)
            .set(data, SetOptions.merge())
    }

    override fun getItemCount(): Int = displayedEvents.size

    // Called once after Firestore load
    fun submitEvents(events: List<Event>) {
        allEvents.clear()
        allEvents.addAll(events)

        displayedEvents.clear()
        displayedEvents.addAll(events)

        loadUserBookmarks()

        notifyDataSetChanged()
    }

    private fun loadUserBookmarks() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .collection("myEvents")
            .whereEqualTo("bookmarked", true)
            .get()
            .addOnSuccessListener { snapshot ->
                bookmarkedEventIds.clear()
                for (doc in snapshot.documents) {
                    bookmarkedEventIds.add(doc.id)
                }
                notifyDataSetChanged()
            }
    }

    // Called on category/search changes
    fun filter(category: String, query: String) {
        displayedEvents.clear()

        displayedEvents.addAll(
            allEvents.filter { event ->
                val matchCategory =
                    category.equals("All", true) ||
                            event.category.equals(category, true)

                val matchSearch =
                    query.isEmpty() ||
                            event.title.contains(query, ignoreCase = true)

                matchCategory && matchSearch
            }
        )

        notifyDataSetChanged()
    }
}
