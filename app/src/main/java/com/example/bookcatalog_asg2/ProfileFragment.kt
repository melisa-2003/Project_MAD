package com.example.bookcatalog_asg2 // 确保这是你正确的包名

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bookcatalog_asg2.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            updateUI(currentUser)
        } else {
            binding.usernameText.text = getString(R.string.profile_username)
            binding.emailAddressText.text = "Please log in"
        }


        setupClickListeners()

        binding.signOutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(activity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            Toast.makeText(context, "You have been signed out.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        binding.interestsButton.setOnClickListener {

            navigateTo(InterestsFragment())
        }

        binding.manageNotificationButton.setOnClickListener {
            navigateTo(ManageNotificationFragment())
        }

        binding.themeButton.setOnClickListener {
            // 你需要先创建 ThemeFragment
            Toast.makeText(context, "Theme Clicked", Toast.LENGTH_SHORT).show()
            // navigateTo(ThemeFragment())
        }

        binding.aboutUsButton.setOnClickListener {
            navigateTo(AboutUsFragment())
        }

        binding.contactUsButton.setOnClickListener {
           navigateTo(ContactUsFragment())
        }
    }

    private fun navigateTo(fragment: Fragment) {
        activity?.supportFragmentManager?.beginTransaction()
            ?.replace(R.id.fragment_container, fragment)
            ?.commit()
    }


    @SuppressLint("SetTextI18n")
    private fun updateUI(user: FirebaseUser) {
        binding.emailAddressText.text = user.email
        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val username = document.getString("username")
                    binding.usernameText.text = if (!username.isNullOrBlank()) {
                        username
                    } else {
                        "No Name"
                    }
                } else {
                    binding.usernameText.text = "Unknown"
                }
            }
            .addOnFailureListener {
                binding.usernameText.text = "Unknown"
                Toast.makeText(context, "Failed to load username", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}



