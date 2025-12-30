package com.example.bookcatalog_asg2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bookcatalog_asg2.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.content.edit

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

            if (!isAllowedEmail(email)) {
                Toast.makeText(this, "Invalid email", Toast.LENGTH_LONG).show()
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
                            Toast.makeText(this, "Account not found. Please sign up.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Invalid email or password.", Toast.LENGTH_SHORT).show()
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

    private fun isAllowedEmail(email: String): Boolean {
        return email.endsWith("@admin.uni.my") || email.endsWith("@siswa.uni.my")
    }

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
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        android.util.Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                        proceedToNextActivity(role, uid, email, doc)
                        return@addOnCompleteListener
                    }

                    val token = task.result

                    if (token != null) {
                        val userDataWithToken = mapOf("fcmToken" to token)
                        db.collection("users").document(uid)
                            .update(userDataWithToken)
                            .addOnSuccessListener {
                                android.util.Log.d("FCM", "Token updated on login.")
                                proceedToNextActivity(role, uid, email, doc)
                            }
                            .addOnFailureListener {
                                android.util.Log.e("FCM", "Failed to update token.", it)
                                proceedToNextActivity(role, uid, email, doc)
                            }
                    } else {
                        proceedToNextActivity(role, uid, email, doc)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user account.", Toast.LENGTH_SHORT).show()
                auth.signOut()
            }
    }

    private fun proceedToNextActivity(
        role: String,
        uid: String,
        email: String,
        doc: com.google.firebase.firestore.DocumentSnapshot
    ) {
        if (!doc.exists()) {
            val userData = mapOf(
                "email" to email,
                "role" to role
            )
            db.collection("users").document(uid).set(userData)
        }

        // Set the notification badge count to 1 on every successful login for non-admins
        if (role != "admin") {
            val sharedPref = getSharedPreferences("app_notifications", MODE_PRIVATE)
            sharedPref.edit { putInt("badge_count", 1) }
        }

        val intent = if (role == "admin") {
            Intent(this, AdminActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }

        startActivity(intent)
        finish()
    }
}