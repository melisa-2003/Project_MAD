package com.example.bookcatalog_asg2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class FilterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filter)

        // Buttons
        val btnApply = findViewById<Button>(R.id.btn_apply)
        val btnClear = findViewById<Button>(R.id.btn_clear)

        // Containers
        val llCategories = findViewById<LinearLayout>(R.id.ll_categories)
        val rgSort = findViewById<RadioGroup>(R.id.rg_sort)
        val rgDate = findViewById<RadioGroup>(R.id.rg_date)

        // APPLY
        btnApply.setOnClickListener {

            // -------- CATEGORIES --------
            val selectedCategories = mutableListOf<String>()

            for (i in 0 until llCategories.childCount) {
                val view = llCategories.getChildAt(i)
                if (view is CheckBox && view.isChecked) {
                    selectedCategories.add(view.text.toString())
                }
            }

            // -------- SORT --------
            val sortBy = when (rgSort.checkedRadioButtonId) {
                -1 -> "Relevance"
                else -> {
                    val rb = findViewById<RadioButton>(rgSort.checkedRadioButtonId)
                    rb.text.toString()
                }
            }

            // -------- DATE --------
            val dateFilter = when (rgDate.checkedRadioButtonId) {
                -1 -> "Any"
                else -> {
                    val rb = findViewById<RadioButton>(rgDate.checkedRadioButtonId)
                    rb.text.toString()
                }
            }

            // -------- RESULT --------
            val resultIntent = Intent().apply {
                putStringArrayListExtra(
                    "FILTER_CATEGORIES",
                    ArrayList(selectedCategories)
                )
                putExtra("FILTER_SORT", sortBy)
                putExtra("FILTER_DATE", dateFilter)
            }

            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        // CLEAR
        btnClear.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }
}



