package com.example.bookcatalog_asg2

import android.os.Bundle
import android.provider.CalendarContract
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookcatalog_asg2.databinding.FragmentMyEventsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Intent
import com.google.android.material.bottomnavigation.BottomNavigationView

class MyEventsFragment : Fragment(R.layout.fragment_my_events) {

    private lateinit var binding: FragmentMyEventsBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var adapter: MyEventAdapter
    private var isRegistrationTab = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMyEventsBinding.bind(view)

        setupRecyclerView()
        setupListeners()
        loadMyEvents()
    }

    private fun setupRecyclerView() {
        adapter = MyEventAdapter(
            onEventClick = { event ->
                EventDetailsActivity.start(requireContext(), event.id)
            },
            onActionClick = { event ->
                handleAction(event)
            }
        )

        binding.rvMyEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMyEvents.adapter = adapter
    }

    private fun setupListeners() {
        // Back Button
        binding.backButton.setOnClickListener {
            // Since this is a main navigation tab, "back" usually means go to Home
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
            bottomNav.selectedItemId = R.id.nav_home
        }

        // Toggle Group
        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            isRegistrationTab = (checkedId == R.id.btn_registered)
            loadMyEvents()
        }

        // Open Calendar Button
        binding.btnOpenCalendar.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = CalendarContract.CONTENT_URI.buildUpon()
                    .appendPath("time")
                    .build()
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "No calendar app found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadMyEvents() {
        val uid = auth.currentUser?.uid ?: return

        binding.layoutEmpty.visibility = View.GONE
        
        // Clear adapter first to show loading state or transition
        adapter.submitList(emptyList(), isRegistrationTab)

        val fieldToCheck = if (isRegistrationTab) "registered" else "bookmarked"

        db.collection("users")
            .document(uid)
            .collection("myEvents")
            .whereEqualTo(fieldToCheck, true)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    showEmpty()
                    return@addOnSuccessListener
                }

                val events = mutableListOf<Event>()
                var loadedCount = 0
                val totalDocs = snapshot.documents.size

                snapshot.documents.forEach { myEventDoc ->
                    val eventId = myEventDoc.id

                    db.collection("events")
                        .document(eventId)
                        .get()
                        .addOnSuccessListener { eventDoc ->
                            if (eventDoc.exists()) {
                                val event = eventDoc.toObject(Event::class.java)
                                if (event != null) {
                                    // Manually set ID if not in document
                                    val finalEvent = event.copy(id = eventId)
                                    events.add(finalEvent)
                                }
                            }
                            
                            loadedCount++
                            if (loadedCount == totalDocs) {
                                if (events.isEmpty()) {
                                    showEmpty()
                                } else {
                                    binding.layoutEmpty.visibility = View.GONE
                                    adapter.submitList(events, isRegistrationTab)
                                }
                            }
                        }
                        .addOnFailureListener {
                            loadedCount++
                            if (loadedCount == totalDocs && events.isEmpty()) showEmpty()
                        }
                }
            }
            .addOnFailureListener {
                showEmpty()
            }
    }

    private fun handleAction(event: Event) {
        val uid = auth.currentUser?.uid ?: return
        val fieldToUpdate = if (isRegistrationTab) "registered" else "bookmarked"

        db.collection("users").document(uid)
            .collection("myEvents").document(event.id)
            .update(fieldToUpdate, false)
            .addOnSuccessListener {
                adapter.removeItem(event)
                val msg = if (isRegistrationTab) "Registration cancelled" else "Bookmark removed"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                
                if (adapter.itemCount == 0) {
                    showEmpty()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Action failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEmpty() {
        binding.layoutEmpty.visibility = View.VISIBLE
        adapter.submitList(emptyList(), isRegistrationTab)
    }
    
    override fun onResume() {
        super.onResume()
        // Reload data when returning to fragment (e.g. from Details)
        loadMyEvents()
    }
}
