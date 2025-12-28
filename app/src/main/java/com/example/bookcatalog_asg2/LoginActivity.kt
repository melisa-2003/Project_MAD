package com.example.bookcatalog_asg2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bookcatalog_asg2.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // LOGIN
        binding.loginBtn.setOnClickListener {
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill out all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ALLOW ONLY ADMIN OR STUDENT EMAIL DOMAINS
            if (!isAllowedEmail(email)) {
                Toast.makeText(
                    this,
                    "Invalid email",
                    Toast.LENGTH_LONG
                ).show()

                binding.email.text?.clear()
                binding.email.requestFocus()
                return@setOnClickListener
            }

            binding.loginBtn.isEnabled = false

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    binding.loginBtn.isEnabled = true

                    if (task.isSuccessful) {
                        checkUserRole()
                    } else {
                        val exception = task.exception
                        if (exception is FirebaseAuthInvalidUserException) {
                            Toast.makeText(
                                this,
                                "Account not found. Please sign up.",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                this,
                                "Invalid email or password.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
        }

        // SIGN UP
        binding.btnSignUpNow.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        // RESET PASSWORD
        binding.forgotPass.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    // EMAIL DOMAIN VALIDATION
    private fun isAllowedEmail(email: String): Boolean {
        return email.endsWith("@admin.uni.my") || email.endsWith("@siswa.uni.my")
    }

    // ROLE CHECK
    private fun checkUserRole() {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val email = user.email ?: ""

        val role = when {
            email.endsWith("@admin.uni.my") -> "admin"
            email.endsWith("@siswa.uni.my") -> "student"
            else -> {
                auth.signOut()
                Toast.makeText(this, "Invalid email domain.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    val userData = mapOf(
                        "email" to email,
                        "role" to role
                    )
                    db.collection("users").document(uid).set(userData)
                }

                val intent = if (role == "admin") {
                    Intent(this, AdminActivity::class.java)
                } else {
                    Intent(this, MainActivity::class.java)
                }

                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user account.", Toast.LENGTH_SHORT).show()
                auth.signOut()
            }
    }
}