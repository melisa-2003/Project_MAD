package com.example.bookcatalog_asg2

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class EventListActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EventAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_list)

        // 1️⃣ 绑定 Toolbar
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(
            R.id.toolbar_event_list
        )
        setSupportActionBar(toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val statusBarHeight = insets.getInsets(
                WindowInsetsCompat.Type.statusBars()
            ).top

            v.setPadding(
                v.paddingLeft,
                statusBarHeight, // 👈 这里 16px，不是 dp
                v.paddingRight,
                v.paddingBottom
            )
            insets
        }

        // 显示返回键
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerView = findViewById(R.id.rv_event_list)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = EventAdapter { openDetails(it) }
        recyclerView.adapter = adapter

        // 获取类型
        val type = intent.getStringExtra("TYPE") ?: "ALL"

        // 设置标题
        title = when (type) {
            "FEATURED" -> "Featured Events"
            "TRENDING" -> "Trending Events"
            "RECOMMEND" -> "Recommended Events"
            else -> "All Events"
        }

        // 加载 Firestore 数据
        loadEvents(type)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish() // 👈 返回 DiscoverFragment
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadEvents(type: String) {
        val query = when (type) {
            "FEATURED" -> db.collection("events").whereEqualTo("featured", true)
            "TRENDING" -> db.collection("events").orderBy("viewCount", Query.Direction.DESCENDING)
            "RECOMMEND" -> db.collection("events").limit(10)
            else -> db.collection("events")
        }

        query.get()
            .addOnSuccessListener { snapshot ->
                val events = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Event::class.java)?.copy(id = doc.id)
                }

                if (events.isEmpty()) {
                    Toast.makeText(this, "No events found", Toast.LENGTH_SHORT).show()
                }

                adapter.submitEvents(events)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load events", Toast.LENGTH_SHORT).show()
                it.printStackTrace()
            }
    }

    private fun openDetails(event: Event) {
        startActivity(
            android.content.Intent(this, EventDetailsActivity::class.java)
                .putExtra("EVENT_ID", event.id)
        )
    }
}
