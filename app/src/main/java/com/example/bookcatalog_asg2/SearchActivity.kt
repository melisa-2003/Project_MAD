package com.example.bookcatalog_asg2

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class SearchActivity : AppCompatActivity() {

    private lateinit var eventAdapter: EventAdapter
    private val db = FirebaseFirestore.getInstance()

    private lateinit var etSearch: EditText
    private lateinit var suggestionsLayout: LinearLayout
    private lateinit var rvResults: RecyclerView
    
    private var isProgrammaticSearch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        // Views
        etSearch = findViewById(R.id.et_search)
        val btnCancel = findViewById<TextView>(R.id.tv_cancel)

        suggestionsLayout = findViewById(R.id.ll_suggestions)
        rvResults = findViewById(R.id.rv_search_results)

        val recentWorkshop = findViewById<TextView>(R.id.tv_recent_item)
        val trendingTalk = findViewById<TextView>(R.id.tv_trending_item_1)
        val trendingCompetition = findViewById<TextView>(R.id.tv_trending_item_2)

        // RecyclerView
        eventAdapter = EventAdapter { event ->
            val intent = Intent(this, EventDetailsActivity::class.java)
            intent.putExtra("EVENT_ID", event.id)
            startActivity(intent)
        }

        rvResults.layoutManager = LinearLayoutManager(this)
        rvResults.adapter = eventAdapter
        rvResults.visibility = View.GONE

        // Focus search automatically
        etSearch.requestFocus()

        // Cancel -> close SearchActivity
        btnCancel.setOnClickListener {
            finish()
        }

        // USER TYPES SEARCH
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) {
                if (isProgrammaticSearch) return

                val query = s.toString().trim()

                if (query.isNotEmpty()) {
                    suggestionsLayout.visibility = View.GONE
                    rvResults.visibility = View.VISIBLE
                    performSearch(query)
                } else {
                    suggestionsLayout.visibility = View.VISIBLE
                    rvResults.visibility = View.GONE
                    eventAdapter.submitEvents(emptyList())
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // RECENT / TRENDING CLICK
        recentWorkshop.setOnClickListener { triggerSearch("workshop") }
        trendingTalk.setOnClickListener { triggerSearch("talk") }
        trendingCompetition.setOnClickListener { triggerSearch("competition") }
    }

    private fun triggerSearch(keyword: String) {
        isProgrammaticSearch = true

        etSearch.setText(keyword)
        etSearch.setSelection(keyword.length)

        suggestionsLayout.visibility = View.GONE
        rvResults.visibility = View.VISIBLE

        performSearch(keyword)

        isProgrammaticSearch = false
    }

    // Firestore search
    private fun performSearch(query: String) {
        db.collection("events")
            .get()
            .addOnSuccessListener { result ->
                val filteredEvents = result.documents.mapNotNull { doc ->
                    val event = doc.toObject(Event::class.java)
                    if (
                        event != null &&
                        (event.title.contains(query, true) ||
                                event.category.contains(query, true))
                    ) {
                        event.copy(id = doc.id)
                    } else null
                }

                eventAdapter.submitEvents(filteredEvents)
            }
    }
}
