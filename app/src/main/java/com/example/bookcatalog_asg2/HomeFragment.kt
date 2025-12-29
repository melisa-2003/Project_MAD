package com.example.bookcatalog_asg2

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookcatalog_asg2.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    private lateinit var eventAdapter: EventAdapter
    private lateinit var categoryAdapter: CategoryAdapter

    private val allEvents = mutableListOf<Event>()

    // Filters
    private var selectedCategory = "All"
    private var filterCategories: List<String> = emptyList()
    private var filterSort: String = "Relevance"

    // Receive filters from FilterActivity
    private val filterLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult

                filterCategories =
                    data.getStringArrayListExtra("FILTER_CATEGORIES") ?: emptyList()
                filterSort = data.getStringExtra("FILTER_SORT") ?: "Relevance"

                applyFilters()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentHomeBinding.bind(view)
        auth = FirebaseAuth.getInstance()

        loadUsername()
        setupCategoryRecycler()
        setupEventRecycler()
        setupListeners()
        loadEventsFromFirestore()
    }

    // ---------------- LOAD USERNAME ----------------
    private fun loadUsername() {
        val currentUser = auth.currentUser ?: return

        db.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { doc ->
                val username = doc.getString("username")
                binding.tvGreeting.text = if (!username.isNullOrBlank()) {
                    getString(R.string.greeting_user, username)
                } else {
                    getString(R.string.greeting_default)
                }
            }
            .addOnFailureListener {
                binding.tvGreeting.text = getString(R.string.greeting_default)
            }
    }

    // ---------------- CATEGORY CHIPS ----------------
    private fun setupCategoryRecycler() {
        val categories = mutableListOf(
            Category("All", true),
            Category("Event"),
            Category("Workshop"),
            Category("Talk"),
            Category("Club"),
            Category("Competition")
        )

        categoryAdapter = CategoryAdapter(categories) { categoryName ->
            selectedCategory = categoryName
            applyFilters()
        }

        binding.rvCategories.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvCategories.adapter = categoryAdapter
    }

    // ---------------- EVENTS ----------------
    private fun setupEventRecycler() {
        eventAdapter = EventAdapter { event ->
            val intent = Intent(requireContext(), EventDetailsActivity::class.java)
            intent.putExtra("EVENT_ID", event.id)
            startActivity(intent)
        }

        binding.rvEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEvents.adapter = eventAdapter
    }

    // ---------------- LISTENERS ----------------
    private fun setupListeners() {

        // Search screen
        binding.etSearch.setOnClickListener {
            startActivity(Intent(requireContext(), SearchActivity::class.java))
        }

        // Filter screen
        binding.btnFilter.setOnClickListener {
            filterLauncher.launch(
                Intent(requireContext(), FilterActivity::class.java)
            )
        }

        // Search typing
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // ---------------- FIRESTORE ----------------
    private fun loadEventsFromFirestore() {
        db.collection("events")
            .get()
            .addOnSuccessListener { result ->
                allEvents.clear()

                for (doc in result.documents) {
                    val event = doc.toObject(Event::class.java)
                    if (event != null) {
                        allEvents.add(event.copy(id = doc.id))
                    }
                }

                applyFilters()
            }
            .addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    R.string.error_load_events,
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // ---------------- APPLY FILTERS ----------------
    private fun applyFilters() {
        val query = binding.etSearch.text.toString().trim().lowercase()

        val filtered = allEvents.filter { event ->

            val matchChipCategory =
                selectedCategory == "All" || event.category == selectedCategory

            val matchFilterCategory =
                filterCategories.isEmpty() || filterCategories.contains(event.category)

            val matchSearch =
                query.isEmpty() || event.title.lowercase().contains(query)

            matchChipCategory && matchFilterCategory && matchSearch
        }

        // Sort
        eventAdapter.submitEvents(filtered)
    }
}
