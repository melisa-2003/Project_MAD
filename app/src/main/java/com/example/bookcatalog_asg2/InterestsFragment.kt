package com.example.bookcatalog_asg2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bookcatalog_asg2.databinding.FragmentInterestsBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class InterestsFragment : Fragment() {

    private var _binding: FragmentInterestsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInterestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.topAppBar.setNavigationOnClickListener {
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.fragment_container, ProfileFragment()) // <-- 确保容器ID正确
                ?.commit()
        }

        binding.clearButton.setOnClickListener {
            binding.interestChipGroup.clearCheck()
            Toast.makeText(context, "Selection cleared", Toast.LENGTH_SHORT).show()
        }

        binding.saveButton.setOnClickListener {
            val checkedChipIds = binding.interestChipGroup.checkedChipIds

            if (checkedChipIds.isEmpty()) {
                Toast.makeText(context, "Please select at least one interest.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedInterests = checkedChipIds.map { chipId ->
                view.findViewById<com.google.android.material.chip.Chip>(chipId).text.toString()
            }

            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                val db = FirebaseFirestore.getInstance()
                db.collection("users").document(userId)
                    .update("interests", selectedInterests)
                    .addOnSuccessListener {

                        val firstInterest = selectedInterests.first()
                        val message = "Great! We will recommend events related to '$firstInterest' and more."
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()

                        view.postDelayed({
                            activity?.supportFragmentManager?.popBackStack()
                        }, 1500)

                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Failed to save interests: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(context, "Error: User not logged in.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

