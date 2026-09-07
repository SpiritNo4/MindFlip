package com.example.mindflip

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var signUpButton: MaterialButton
    private lateinit var loginLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val nameLayout =
            findViewById<TextInputLayout>(R.id.fullNameLayout)
        val emailLayout =
            findViewById<TextInputLayout>(R.id.emailLayout)
        val passwordLayout =
            findViewById<TextInputLayout>(R.id.passwordLayout)

        val nameInput =
            findViewById<TextInputEditText>(R.id.editFullName)
        val emailInput =
            findViewById<TextInputEditText>(R.id.editEmail)
        val passwordInput =
            findViewById<TextInputEditText>(R.id.editPassword)

        signUpButton = findViewById(R.id.btnSignUp)
        loginLink = findViewById(R.id.textLogin)

        signUpButton.text = "Sign Up"

        loginLink.setOnClickListener {
            openLogin(emailInput.text.toString().trim())
        }

        signUpButton.setOnClickListener {
            nameLayout.error = null
            emailLayout.error = null
            passwordLayout.error = null

            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            var valid = true

            if (name.isBlank()) {
                nameLayout.error = "Enter your full name"
                valid = false
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.error = "Enter a valid email address"
                valid = false
            }

            if (password.length < 8) {
                passwordLayout.error = "Use at least 8 characters"
                valid = false
            }

            if (valid) {
                setLoading(true)

                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            setLoading(false)
                            showMessage(
                                task.exception?.localizedMessage
                                    ?: "Could not create account. Try again."
                            )
                        } else {
                            passwordInput.text?.clear()

                            val user = task.result.user

                            if (user == null) {
                                finishRegistration(email, false)
                            } else {
                                val profile =
                                    UserProfileChangeRequest.Builder()
                                        .setDisplayName(name)
                                        .build()

                                user.updateProfile(profile)
                                    .addOnCompleteListener { profileTask ->
                                        finishRegistration(
                                            email,
                                            profileTask.isSuccessful
                                        )
                                    }
                            }
                        }
                    }
            }
        }
    }

    private fun finishRegistration(email: String, nameSaved: Boolean) {
        // Firebase signs in a newly created user automatically.
        // Sign out here so the user can test the Login screen.
        auth.signOut()
        setLoading(false)

        if (nameSaved) {
            showMessage("Account created. Please log in.")
        } else {
            showMessage(
                "Account created, but your name could not be saved. " +
                        "You can still log in."
            )
        }

        openLogin(email)
    }

    private fun openLogin(email: String) {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra("email", email)
        startActivity(intent)
        finish()
    }

    private fun setLoading(loading: Boolean) {
        signUpButton.isEnabled = !loading
        loginLink.isEnabled = !loading
        signUpButton.text = if (loading) "Creating account…" else "Sign Up"
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}