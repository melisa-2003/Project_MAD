package com.example.bookcatalog_asg2

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddEditEventActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    private lateinit var imagePreview: ImageView
    private lateinit var titleInput: EditText
    private lateinit var dateInput: EditText
    private lateinit var timeInput: EditText
    private lateinit var venueInput: EditText
    private lateinit var organizerInput: EditText
    private lateinit var overviewInput: EditText
    private lateinit var highlightsInput: EditText
    private lateinit var imageRefInput: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var saveButton: Button

    private var eventId: String? = null
    private var selectedCategory = "Event"
    private val selectedDateTime = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_event)

        db = FirebaseFirestore.getInstance()

        imagePreview = findViewById(R.id.imagePreview)
        titleInput = findViewById(R.id.inputTitle)
        dateInput = findViewById(R.id.inputDate)
        timeInput = findViewById(R.id.inputTime)
        venueInput = findViewById(R.id.inputVenue)
        organizerInput = findViewById(R.id.inputOrganizer)
        overviewInput = findViewById(R.id.inputOverview)
        highlightsInput = findViewById(R.id.inputHighlights)
        imageRefInput = findViewById(R.id.inputImageUrl)
        categorySpinner = findViewById(R.id.spinnerCategory)
        saveButton = findViewById(R.id.btnSave)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        imageRefInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) previewImageFromUrl()
        }

        setupCategorySpinner()

        dateInput.setOnClickListener { showDatePicker() }
        timeInput.setOnClickListener { showTimePicker() }

        eventId = intent.getStringExtra("EVENT_ID")
        if (eventId != null) {
            saveButton.text = getString(R.string.update_event)
            loadEventForEdit(eventId!!)
        }

        saveButton.setOnClickListener { saveEvent() }
        findViewById<Button>(R.id.btnCancel).setOnClickListener { finish() }
    }

    // ---------------- CATEGORY ----------------
    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.event_categories,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                selectedCategory = parent.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    // ---------------- DATE PICKER ----------------
    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                // 3. 更新 Calendar 实例，而不是直接格式化字符串
                selectedDateTime.set(Calendar.YEAR, year)
                selectedDateTime.set(Calendar.MONTH, month)
                selectedDateTime.set(Calendar.DAY_OF_MONTH, day)

                // 更新UI让用户看到 (这里的格式仅用于显示，不影响最终保存)
                val displayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                dateInput.setText(displayFormat.format(selectedDateTime.time))
            },
            selectedDateTime.get(Calendar.YEAR),
            selectedDateTime.get(Calendar.MONTH),
            selectedDateTime.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // ---------------- TIME PICKER ----------------
    private fun showTimePicker() {
        TimePickerDialog(
            this,
            { _, hour, minute ->
                // 4. 更新 Calendar 实例
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hour)
                selectedDateTime.set(Calendar.MINUTE, minute)

                // 更新UI让用户看到 (这里的格式仅用于显示，不影响最终保存)
                val displayFormat = SimpleDateFormat("hh:mm a", Locale.US) // 12小时制 + AM/PM
                timeInput.setText(displayFormat.format(selectedDateTime.time))
            },
            selectedDateTime.get(Calendar.HOUR_OF_DAY),
            selectedDateTime.get(Calendar.MINUTE),
            false // 改为 false 来使用支持 AM/PM 的12小时制选择器
        ).show()
    }

    // ---------------- LOAD EVENT ----------------
    private fun loadEventForEdit(id: String) {
        db.collection("events").document(id)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                titleInput.setText(doc.getString("title"))
                venueInput.setText(doc.getString("venue"))
                organizerInput.setText(doc.getString("organizerName"))
                overviewInput.setText(doc.getString("overview"))
                highlightsInput.setText(doc.getString("highlights"))
                imageRefInput.setText(doc.getString("imageRef"))

                val dateTime = doc.getString("dateTime") ?: ""
                if (dateTime.contains(" ")) {
                    val parts = dateTime.split(" ")
                    dateInput.setText(parts[0])
                    timeInput.setText(parts.getOrNull(1) ?: "")
                }

                doc.getString("imageRef")?.let {
                    Glide.with(this)
                        .load(it)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(imagePreview)
                }

                val category = doc.getString("category")
                val categories = resources.getStringArray(R.array.event_categories)
                val index = categories.indexOf(category)
                if (index >= 0) {
                    categorySpinner.setSelection(index)
                    selectedCategory = category ?: selectedCategory
                }
            }
    }

    // ---------------- IMAGE PREVIEW ----------------
    private fun previewImageFromUrl() {
        val url = imageRefInput.text.toString().trim()
        if (url.startsWith("http")) {
            Glide.with(this)
                .load(url)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(imagePreview)
        }
    }

    // ---------------- SAVE EVENT ----------------
    private fun saveEvent() {
        val title = titleInput.text.toString().trim()
        val date = dateInput.text.toString().trim()
        val time = timeInput.text.toString().trim()
        val venue = venueInput.text.toString().trim()
        val organizer = organizerInput.text.toString().trim()
        val overview = overviewInput.text.toString().trim()
        val highlights = highlightsInput.text.toString().trim()
        val imageUrl = imageRefInput.text.toString().trim()
        val adminUid = intent.getStringExtra("ADMIN_UID")

        if (title.isEmpty() || date.isEmpty() || time.isEmpty()) {
            Toast.makeText(this, "Title, date and time are required", Toast.LENGTH_SHORT).show()
            return
        }

        if (adminUid.isNullOrBlank()) {
            Toast.makeText(this, "Error: Admin User ID is missing. Cannot save.", Toast.LENGTH_LONG)
                .show()
            return
        }

        // 5. 关键修复：在这里创建与后台服务完全一致的格式化工具
        val firestoreFormat = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US)
        // 从我们一直维护的 Calendar 实例中获取 Date 对象，然后格式化为正确的字符串
        val finalDateTimeString = firestoreFormat.format(selectedDateTime.time)

        val eventData = hashMapOf(
            "title" to title,
            "dateTime" to finalDateTimeString,
            "venue" to venue,
            "organizerName" to organizer,
            "overview" to overview,
            "highlights" to highlights,
            "category" to selectedCategory,
            "imageRef" to imageUrl,

            "featured" to false,
            "viewCount" to 0L,
            "createdBy" to adminUid
        )

        if (eventId != null) {
            eventData.remove("viewCount")
            eventData.remove("featured")
        }

        val collection = db.collection("events")

        if (eventId == null) {
            // 创建新活动
            collection.add(eventData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Event added successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error adding event: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
        } else {
            // 更新现有活动
            collection.document(eventId!!)
                .update(eventData as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(this, "Event updated successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating event: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
        }
    }
}
