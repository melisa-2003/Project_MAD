package com.example.bookcatalog_asg2

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.widget.TextView
import com.google.android.material.appbar.MaterialToolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class DiscoverFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_discover, container, false)

        val rvFeatured = view.findViewById<RecyclerView>(R.id.rv_featured)
        val rvTrending = view.findViewById<RecyclerView>(R.id.rv_trending)
        val rvRecommend = view.findViewById<RecyclerView>(R.id.rv_recommend)

        rvFeatured.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvTrending.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvRecommend.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val featuredAdapter = EventAdapter { openDetails(it) }
        val trendingAdapter = EventAdapter { openDetails(it) }
        val recommendAdapter = EventAdapter { openDetails(it) }

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar_discover)

        toolbar.title = "Discover"
        // 处理状态栏，保持 toolbar 内容垂直居中
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val statusBarHeight = insets.getInsets(
                WindowInsetsCompat.Type.statusBars()
            ).top

            // 只调整顶部 padding 来避开状态栏
            v.setPadding(
                v.paddingLeft,
                statusBarHeight,
                v.paddingRight,
                v.paddingBottom
            )
            insets
        }

        rvFeatured.adapter = featuredAdapter
        rvTrending.adapter = trendingAdapter
        rvRecommend.adapter = recommendAdapter

        loadFeatured(featuredAdapter)
        loadTrending(trendingAdapter)
        loadRecommend(recommendAdapter)

        // View More
        view.findViewById<TextView>(R.id.btn_view_more_featured)
            .setOnClickListener { openList("FEATURED") }

        view.findViewById<TextView>(R.id.btn_view_more_trending)
            .setOnClickListener { openList("TRENDING") }

        view.findViewById<TextView>(R.id.btn_view_more_recommend)
            .setOnClickListener { openList("RECOMMEND") }

        return view
    }

    private fun loadFeatured(adapter: EventAdapter) {
        db.collection("events")
            .whereEqualTo("featured", true)
            .limit(5)
            .get()
            .addOnSuccessListener {
                val list = it.documents.mapNotNull { d ->
                    d.toObject(Event::class.java)?.copy(id = d.id)
                }
                adapter.submitEvents(list)
            }
    }

    private fun loadTrending(adapter: EventAdapter) {
        db.collection("events")
            .orderBy("viewCount", Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener {
                val list = it.documents.mapNotNull { d ->
                    d.toObject(Event::class.java)?.copy(id = d.id)
                }
                adapter.submitEvents(list)
            }
    }

    private fun loadRecommend(adapter: EventAdapter) {
        db.collection("events")
            .limit(5)
            .get()
            .addOnSuccessListener {
                val list = it.documents.mapNotNull { d ->
                    d.toObject(Event::class.java)?.copy(id = d.id)
                }
                adapter.submitEvents(list)
            }
    }

    private fun openDetails(event: Event) {
        val intent = Intent(requireContext(), EventDetailsActivity::class.java)
        intent.putExtra("EVENT_ID", event.id)
        startActivity(intent)
    }

    private fun openList(type: String) {
        val intent = Intent(requireContext(), EventListActivity::class.java)
        intent.putExtra("TYPE", type)
        startActivity(intent)
    }
}
