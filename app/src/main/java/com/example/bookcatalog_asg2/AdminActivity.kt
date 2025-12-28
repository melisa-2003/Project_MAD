package com.example.bookcatalog_asg2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookcatalog_asg2.databinding.ActivityAdminBinding
import com.google.firebase.firestore.FirebaseFirestore

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: AdminEventAdapter
    private val eventList = mutableListOf<Event>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadEvents()
    }

    private fun setupRecyclerView() {
        adapter = AdminEventAdapter(
            events = eventList,
            onEditClick = { event ->
                val intent = Intent(this, AddEditEventActivity::class.java)
                intent.putExtra("EVENT_ID", event.id)
                startActivity(intent)
            },
            onDeleteClick = { event ->
                showDeleteConfirmationDialog(event)
            }
        )

        binding.rvAdminEvents.layoutManager = LinearLayoutManager(this)
        binding.rvAdminEvents.adapter = adapter
    }

    private fun setupListeners() {
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.fabAddEvent.setOnClickListener {
            startActivity(Intent(this, AddEditEventActivity::class.java))
        }
    }

    private fun loadEvents() {
        db.collection("events")
            .get()
            .addOnSuccessListener { result ->
                val newList = mutableListOf<Event>()

                for (document in result) {
                    val event = document.toObject(Event::class.java)
                        .copy(id = document.id)
                    newList.add(event)
                }

                adapter.updateList(newList)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error loading events: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun showDeleteConfirmationDialog(event: Event) {
        AlertDialog.Builder(this)
            .setTitle("Delete Event")
            .setMessage("Are you sure you want to delete \"${event.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                deleteEvent(event)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteEvent(event: Event) {
        db.collection("events").document(event.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show()
                loadEvents()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error deleting event: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}

