package com.example.bookcatalog_asg2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bookcatalog_asg2.databinding.ItemEventCardBinding

class EventAdapter(
    private val onEventClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    private val allEvents = mutableListOf<Event>()
    private val displayedEvents = mutableListOf<Event>()

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

            // Click listeners
            root.setOnClickListener { onEventClick(event) }
            btnViewDetails.setOnClickListener { onEventClick(event) }
        }
    }

    override fun getItemCount(): Int = displayedEvents.size

    // Called once after Firestore load
    fun submitEvents(events: List<Event>) {
        allEvents.clear()
        allEvents.addAll(events)

        displayedEvents.clear()
        displayedEvents.addAll(events)

        notifyDataSetChanged()
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
