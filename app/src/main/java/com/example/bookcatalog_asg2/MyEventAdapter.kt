package com.example.bookcatalog_asg2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bookcatalog_asg2.databinding.ItemMyEventCardBinding

class MyEventAdapter(
    private val onEventClick: (Event) -> Unit,
    private val onActionClick: (Event) -> Unit
) : RecyclerView.Adapter<MyEventAdapter.MyEventViewHolder>() {

    private val events = mutableListOf<Event>()
    private var isRegistrationTab = true

    class MyEventViewHolder(val binding: ItemMyEventCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyEventViewHolder {
        val binding = ItemMyEventCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MyEventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyEventViewHolder, position: Int) {
        val event = events[position]

        holder.binding.apply {
            tvEventTitle.text = event.title
            tvDateTime.text = event.dateTime
            tvVenue.text = event.venue
            tvCategoryTag.text = event.category
            tvOrganizer.text = event.organizerName

            if (event.imageRef.isNotBlank()) {
                Glide.with(root.context)
                    .load(event.imageRef)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(ivEventImage)
            } else {
                ivEventImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            // Update button text based on tab
            btnAction.text = if (isRegistrationTab) "Cancel Registration" else "Remove Bookmark"

            // Click Listeners
            root.setOnClickListener { onEventClick(event) }
            btnAction.setOnClickListener { onActionClick(event) }
        }
    }

    override fun getItemCount(): Int = events.size

    fun submitList(newEvents: List<Event>, isRegistration: Boolean) {
        events.clear()
        events.addAll(newEvents)
        isRegistrationTab = isRegistration
        notifyDataSetChanged()
    }
    
    fun removeItem(event: Event) {
        val index = events.indexOfFirst { it.id == event.id }
        if (index != -1) {
            events.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}
