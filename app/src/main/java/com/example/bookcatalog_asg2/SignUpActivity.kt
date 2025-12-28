package com.example.bookcatalog_asg2

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bookcatalog_asg2.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.signUpBtn.setOnClickListener {

            val username = binding.username.text.toString().trim()
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()
            val confirmPassword = binding.confirmPassword.text.toString().trim()

            // ---------------- VALIDATION ----------------
            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isAllowedEmail(email)) {
                Toast.makeText(
                    this,
                    "Sign up allowed only with @admin.uni.my (admin) or @siswa.uni.my (student)",
                    Toast.LENGTH_LONG
                ).show()
                binding.email.text?.clear()
                binding.email.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ADMIN PASSWORD RULE
            if (email.endsWith("@admin.uni.my") && !password.startsWith("Uni!")) {
                Toast.makeText(
                    this,
                    "Admin password must start with \"Uni!\"",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            binding.signUpBtn.isEnabled = false

            // ---------------- FIREBASE AUTH ----------------
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->

                    binding.signUpBtn.isEnabled = true

                    if (task.isSuccessful) {

                        val uid = auth.currentUser!!.uid

                        // ROLE FROM EMAIL DOMAIN
                        val role = when {
                            email.endsWith("@admin.uni.my") -> "admin"
                            email.endsWith("@siswa.uni.my") -> "student"
                            else -> "user"
                        }

                        val userData = hashMapOf(
                            "username" to username,
                            "email" to email,
                            "role" to role
                        )

                        db.collection("users")
                            .document(uid)
                            .set(userData)
                            .addOnSuccessListener {

                                auth.signOut()

                                Toast.makeText(
                                    this,
                                    "Sign up successful! Please log in.",
                                    Toast.LENGTH_SHORT
                                ).show()

                                Handler(Looper.getMainLooper()).postDelayed({
                                    val intent = Intent(this, LoginActivity::class.java)
                                    intent.flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                }, 1500)
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    this,
                                    "Failed to save user data",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                    } else {
                        val message = when (val exception = task.exception) {
                            is FirebaseAuthUserCollisionException ->
                                "This email is already registered"
                            else ->
                                exception?.message ?: "Sign up failed"
                        }

                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                }
        }

        binding.btnLoginHere.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // DOMAIN RULE
    private fun isAllowedEmail(email: String): Boolean {
        return email.endsWith("@admin.uni.my") || email.endsWith("@siswa.uni.my")
    }
}

