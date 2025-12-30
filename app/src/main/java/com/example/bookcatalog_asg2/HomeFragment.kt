package com.example.bookcatalog_asg2

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookcatalog_asg2.databinding.FragmentHomeBinding
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
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

    private var sharedPreferences: SharedPreferences? = null
    // 2. 添加一个监听器，用于在后台计数值变化时刷新UI
    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "badge_count") {
                activity?.runOnUiThread {
                    setupNotificationBadge()
                }
            }
        }

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
        setupNotificationBadge()

        // 3. 注册监听器
        sharedPreferences = activity?.getSharedPreferences("app_notifications", Context.MODE_PRIVATE)
        sharedPreferences?.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 4. 注销监听器，防止内存泄漏
        sharedPreferences?.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
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
    @OptIn(ExperimentalBadgeUtils::class)
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

        // Notification
        binding.notificationContainer.setOnClickListener {
            FirestoreListenerService.resetBadgeCount(requireContext())
            BadgeUtils.detachBadgeDrawable(null, binding.notificationContainer)
            startActivity(Intent(requireContext(), NotificationActivity::class.java))
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

    // ---------------- Notification Badge ----------------
    @OptIn(ExperimentalBadgeUtils::class)
    private fun setupNotificationBadge() {
        val sharedPref = activity?.getSharedPreferences("app_notifications", Context.MODE_PRIVATE) ?: return

        // 1. 直接从 SharedPreferences 读取计数值
        val badgeCount = sharedPref.getInt("badge_count", 0)
        Log.d("HomeFragment", "setupNotificationBadge called. Current badge count from Prefs: $badgeCount")

        val badgeContainer = binding.notificationContainer
        // 无论如何，先移除旧的角标，防止重叠
        BadgeUtils.detachBadgeDrawable(null, badgeContainer)

        // 2. 如果计数值大于0，就创建并显示新的角标
        if (badgeCount > 0) {
            val badge = BadgeDrawable.create(requireContext())
            badge.number = badgeCount
            badge.backgroundColor = ContextCompat.getColor(requireContext(), R.color.red)

            BadgeUtils.attachBadgeDrawable(badge, badgeContainer)

            // 使用 post 来确保在布局完成后再调整位置
            badgeContainer.post {
                badge.badgeGravity = BadgeDrawable.TOP_START

                // 您的位置调整代码
                val horizontalOffset = (badgeContainer.width / 2) + (binding.ivNotifications.width / 2) - (badge.intrinsicWidth / 2) - 4
                val verticalOffset = (badgeContainer.height / 2) - (binding.ivNotifications.height / 2) + 16

                badge.horizontalOffset = horizontalOffset
                badge.verticalOffset = verticalOffset
            }
        } else {
            Log.d("HomeFragment", "Badge count is 0, no badge will be shown.")
        }
    }
}
