package com.example.bookcatalog_asg2

import android.content.Context
import android.content.Intent
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
import androidx.core.content.edit

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
        // Call setupNotificationBadge() here to ensure it runs when the view is first created
        setupNotificationBadge()
    }

    override fun onResume() {
        super.onResume()
        // Also call it here to refresh the badge when the user returns to the screen
        setupNotificationBadge()
    }

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

    private fun setupEventRecycler() {
        eventAdapter = EventAdapter { event ->
            val intent = Intent(requireContext(), EventDetailsActivity::class.java)
            intent.putExtra("EVENT_ID", event.id)
            startActivity(intent)
        }

        binding.rvEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEvents.adapter = eventAdapter
    }

    @OptIn(ExperimentalBadgeUtils::class)
    private fun setupListeners() {

        binding.etSearch.setOnClickListener {
            startActivity(Intent(requireContext(), SearchActivity::class.java))
        }

        binding.btnFilter.setOnClickListener {
            filterLauncher.launch(
                Intent(requireContext(), FilterActivity::class.java)
            )
        }

        binding.notificationContainer.setOnClickListener {
            // 1. 获取 SharedPreferences
            val sharedPref = requireActivity().getSharedPreferences("app_notifications", Context.MODE_PRIVATE)

            // 2. 将后台的角标计数值清零
            sharedPref.edit { // 使用已有的 anko 扩展函数
                putInt("badge_count", 0)
            }

            // 3. 立即调用我们的函数来更新UI（它会发现计数值为0并移除红点）
            setupNotificationBadge()

            // 4. 跳转到通知页面
            startActivity(Intent(requireContext(), NotificationActivity::class.java))
        }


        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

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

        eventAdapter.submitEvents(filtered)
    }

    @OptIn(ExperimentalBadgeUtils::class)
    private fun setupNotificationBadge() {
        // i & ii. 获取 SharedPreferences 和 badge_count 值
        val sharedPref = activity?.getSharedPreferences("app_notifications", Context.MODE_PRIVATE) ?: return
        val badgeCount = sharedPref.getInt("badge_count", 0)

        Log.d("HomeFragment", "setupNotificationBadge called. Badge count: $badgeCount")

        val anchor = binding.ivNotifications

        // 每次更新前，都先移除旧的角标，防止重复绘制
        BadgeUtils.detachBadgeDrawable(null, anchor)

        // iii. 判断 badge_count 是否大于 0
        if (badgeCount > 0) {
            // iv. 如果大于0，则创建并显示红点
            Log.d("HomeFragment", "Badge count is > 0, showing badge.")

            // 创建一个新的 BadgeDrawable
            val badge = BadgeDrawable.create(requireContext())

            // 设置颜色为红色
            badge.backgroundColor = ContextCompat.getColor(requireContext(), R.color.red)
            // 设置位置在右上角
            badge.badgeGravity = BadgeDrawable.TOP_END

            // 微调位置以获得更好的视觉效果 (可以根据你的图标调整这些值)
            badge.verticalOffset = 5    // 向下移动 5dp
            badge.horizontalOffset = 5  // 向右移动 5dp

            // 最后，将角标附加到你的 ImageView 上
            BadgeUtils.attachBadgeDrawable(badge, anchor)
        } else {
            // 如果 badge_count 为 0，detach 已经执行，无需额外操作
            Log.d("HomeFragment", "Badge count is 0, no badge will be shown.")
        }
    }
}
