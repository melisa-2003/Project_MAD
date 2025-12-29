package com.example.bookcatalog_asg2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bookcatalog_asg2.databinding.FragmentManageNotificationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ManageNotificationFragment : Fragment() {

    private var _binding: FragmentManageNotificationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.topAppBar.setNavigationOnClickListener {
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.fragment_container, ProfileFragment()) // <-- 确保容器ID正确
                ?.commit()
        }

        loadNotificationSettings()

        setupSwitchListeners()
    }

    private fun loadNotificationSettings() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    binding.phoneAlertSwitch.isChecked = document.getBoolean("alertOnPhone") ?: false
                    binding.emailAlertSwitch.isChecked = document.getBoolean("alertOnEmail") ?: false
                    binding.eventReminderSwitch.isChecked = document.getBoolean("eventReminder") ?: false
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load settings.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupSwitchListeners() {
        binding.phoneAlertSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveNotificationSetting("alertOnPhone", isChecked)
        }

        binding.emailAlertSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveNotificationSetting("alertOnEmail", isChecked)
        }

        binding.eventReminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveNotificationSetting("eventReminder", isChecked)
        }
    }

    private fun saveNotificationSetting(key: String, value: Boolean) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId)
            .update(key, value)
            .addOnSuccessListener {
                Toast.makeText(context, "Setting saved.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
