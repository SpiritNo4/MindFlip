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

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout

    private lateinit var loginButton: MaterialButton
    private lateinit var signUpLink: TextView
    private lateinit var forgotPasswordLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        emailInput = findViewById(R.id.editLoginEmail)
        passwordInput = findViewById(R.id.editLoginPassword)

        emailLayout = findViewById(R.id.loginEmailLayout)
        passwordLayout = findViewById(R.id.loginPasswordLayout)

        loginButton = findViewById(R.id.btnLogin)
        signUpLink = findViewById(R.id.textSignUp)
        forgotPasswordLink = findViewById(R.id.textForgotPassword)

        loginButton.text = "Log In"

        if (savedInstanceState == null) {
            emailInput.setText(intent.getStringExtra("email").orEmpty())
        }

        signUpLink.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        loginButton.setOnClickListener {
            login()
        }

        forgotPasswordLink.setOnClickListener {
            resetPassword()
        }
    }

    private fun login() {
        emailLayout.error = null
        passwordLayout.error = null

        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()

        var valid = true

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = "Enter a valid email address"
            valid = false
        }

        if (password.isEmpty()) {
            passwordLayout.error = "Enter your password"
            valid = false
        }

        if (!valid) return

        setLoading(true, "Logging in…")

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                setLoading(false)

                if (task.isSuccessful) {
                    passwordInput.text?.clear()

                    val intent = Intent(
                        this,
                        HomeActivity::class.java
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }

                    startActivity(intent)
                    finish()
                } else {
                    showMessage(
                        task.exception?.localizedMessage
                            ?: "Login failed. Check your details and try again."
                    )
                }
            }
    }

    private fun resetPassword() {
        emailLayout.error = null

        val email = emailInput.text.toString().trim()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = "Enter your account email first"
            emailInput.requestFocus()
            return
        }

        setLoading(true, "Sending reset request…")

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                setLoading(false)

                if (task.isSuccessful) {
                    showMessage(
                        "If an account exists for this email, " +
                                "you will receive a reset link. Check your spam folder too."
                    )
                } else {
                    showMessage(
                        task.exception?.localizedMessage
                            ?: "Could not request a reset. Try again."
                    )
                }
            }
    }

    private fun setLoading(
        loading: Boolean,
        message: String = "Log In"
    ) {
        loginButton.isEnabled = !loading
        signUpLink.isEnabled = !loading
        forgotPasswordLink.isEnabled = !loading

        loginButton.text = if (loading) message else "Log In"
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}