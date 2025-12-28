package com.example.bookcatalog_asg2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.bookcatalog_asg2.databinding.ItemAdminEventBinding

class AdminEventAdapter(
    private val events: MutableList<Event>,
    private val onEditClick: (Event) -> Unit,
    private val onDeleteClick: (Event) -> Unit
) : RecyclerView.Adapter<AdminEventAdapter.AdminEventViewHolder>() {

    class AdminEventViewHolder(
        val binding: ItemAdminEventBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminEventViewHolder {
        val binding = ItemAdminEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AdminEventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdminEventViewHolder, position: Int) {
        val event = events[position]

        holder.binding.apply {
            tvAdminEventTitle.text = event.title
            tvAdminDateTime.text = event.dateTime
            tvAdminVenue.text = event.venue

            ivAdminEdit.setOnClickListener {
                onEditClick(event)
            }

            ivAdminDelete.setOnClickListener {
                onDeleteClick(event)
            }
        }
    }

    override fun getItemCount(): Int = events.size

    fun updateList(newEvents: List<Event>) {
        val diffCallback = AdminEventDiffCallback(events, newEvents)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        events.clear()
        events.addAll(newEvents)

        diffResult.dispatchUpdatesTo(this)
    }

    class AdminEventDiffCallback(
        private val oldList: List<Event>,
        private val newList: List<Event>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}

