package com.example.bookcatalog_asg2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class NotificationAdapter(
    private val onItemClick: (Event) -> Unit
) : ListAdapter<NotificationItem, RecyclerView.ViewHolder>(DiffCallback()) {

    // --- ViewHolder ---
    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_EVENT = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is NotificationItem.Header -> VIEW_TYPE_HEADER
            is NotificationItem.EventItem -> VIEW_TYPE_EVENT
        }
    }

    // --- Create ViewHolder ---
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            HeaderViewHolder(
                inflater.inflate(R.layout.item_notification_header, parent, false)
            )
        } else {
            EventViewHolder(
                inflater.inflate(R.layout.item_notification, parent, false)
            )
        }
    }

    // ---  onBindViewHolder ---
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is NotificationItem.Header -> {
                (holder as HeaderViewHolder).bind(item.title)
            }
            is NotificationItem.EventItem -> {
                val eventViewHolder = holder as EventViewHolder
                val event = item.event
                eventViewHolder.bind(event)

                eventViewHolder.itemView.setOnClickListener {
                    onItemClick(event)
                }
            }
        }
    }

    // ---------------- HEADER ViewHolder ----------------
    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvHeader: TextView = view.findViewById(R.id.tvHeader)

        fun bind(title: String) {
            tvHeader.text = title
        }
    }

    // ---------------- EVENT ITEM ViewHolder ----------------
    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        private val tvMessage: TextView = view.findViewById(R.id.tvMessage)

        fun bind(event: Event) {
            tvTitle.text = event.title
            tvMessage.text = event.dateTime
        }
    }

    // ---------------- DIFF CALLBACK ----------------
    class DiffCallback : DiffUtil.ItemCallback<NotificationItem>() {
        override fun areItemsTheSame(
            oldItem: NotificationItem,
            newItem: NotificationItem
        ): Boolean {
            return when {
                oldItem is NotificationItem.Header && newItem is NotificationItem.Header ->
                    oldItem.title == newItem.title
                oldItem is NotificationItem.EventItem && newItem is NotificationItem.EventItem ->
                    oldItem.event.id == newItem.event.id
                else -> false
            }
        }

        override fun areContentsTheSame(
            oldItem: NotificationItem,
            newItem: NotificationItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}





