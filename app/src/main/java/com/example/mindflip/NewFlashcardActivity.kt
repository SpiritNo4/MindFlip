package com.example.mindflip

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class NewFlashcardActivity : AppCompatActivity() {

    private lateinit var subjectInput: MaterialAutoCompleteTextView
    private lateinit var topicInput: TextInputEditText
    private lateinit var questionInput: TextInputEditText
    private lateinit var answerInput: TextInputEditText
    private lateinit var saveButton: MaterialButton

    private val subjects = listOf(
        "CTE308",
        "EDP101",
        "ITM301",
        "ITM302"
    )

    private var saving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            openLogin()
            return
        }

        setContentView(R.layout.activity_new_flashcard)
        // Keep controls clear of the status bar and navigation bar.
        val root = findViewById<android.view.ViewGroup>(
            android.R.id.content
        ).getChildAt(0)

        val originalLeft = root.paddingLeft
        val originalTop = root.paddingTop
        val originalRight = root.paddingRight
        val originalBottom = root.paddingBottom

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(
            root
        ) { view, insets ->

            val safeArea = insets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.systemBars() or
                        androidx.core.view.WindowInsetsCompat.Type.displayCutout()
            )

            view.setPadding(
                originalLeft + safeArea.left,
                originalTop + safeArea.top,
                originalRight + safeArea.right,
                originalBottom + safeArea.bottom
            )

            androidx.core.view.WindowInsetsCompat.CONSUMED
        }

        root.post {
            androidx.core.view.ViewCompat.requestApplyInsets(root)
        }

        subjectInput = findViewById(R.id.dropdownSubject)
        topicInput = findViewById(R.id.editTopic)
        questionInput = findViewById(R.id.editQuestion)
        answerInput = findViewById(R.id.editAnswer)
        saveButton = findViewById(R.id.btnSaveFlashcard)

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            subjects
        )

        subjectInput.setAdapter(adapter)

        if (savedInstanceState == null) {
            subjectInput.setText("", false)
        }

        val returnToLibrary: () -> Unit = {
            if (!isFinishing) {
                val next = Intent(
                    this,
                    LibraryActivity::class.java
                ).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                }

                startActivity(next)
                finish()
            }
        }

        findViewById<View>(R.id.btnBack).setOnClickListener {
            returnToLibrary()
        }

        BottomNavigationHelper.setup(this, R.id.nav_library)

        findViewById<
                com.google.android.material.bottomnavigation.BottomNavigationView
                >(
            R.id.bottomNavigationView
        ).setOnItemReselectedListener { item ->
            if (item.itemId == R.id.nav_library) {
                returnToLibrary()
            }
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    returnToLibrary()
                }
            }
        )
        saveButton.setOnClickListener {
            saveFlashcard()
        }
    }

    private fun saveFlashcard() {
        if (saving) return

        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            openLogin()
            return
        }

        val subjectLayout =
            findViewById<TextInputLayout>(R.id.subjectLayout)
        val topicLayout =
            findViewById<TextInputLayout>(R.id.topicLayout)
        val questionLayout =
            findViewById<TextInputLayout>(R.id.questionLayout)
        val answerLayout =
            findViewById<TextInputLayout>(R.id.answerLayout)

        subjectLayout.error = null
        topicLayout.error = null
        questionLayout.error = null
        answerLayout.error = null

        val subject = subjectInput.text.toString().trim()
        val topic = topicInput.text.toString().trim()
        val question = questionInput.text.toString().trim()
        val answer = answerInput.text.toString().trim()

        var valid = true

        if (subject !in subjects) {
            subjectLayout.error = "Choose a subject"
            valid = false
        }

        if (topic.isBlank()) {
            topicLayout.error = "Enter a topic"
            valid = false
        }

        if (question.isBlank()) {
            questionLayout.error = "Enter a question"
            valid = false
        }

        if (answer.isBlank()) {
            answerLayout.error = "Enter an answer"
            valid = false
        }

        if (!valid) return

        setSaving(true)

        val flashcard = hashMapOf<String, Any>(
            "subject" to subject,
            "topic" to topic,
            "question" to question,
            "answer" to answer,
            "mastered" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .collection("flashcards")
            .add(flashcard)
            .addOnSuccessListener {
                if (!isFinishing && !isDestroyed) {
                    setSaving(false)

                    topicInput.text?.clear()
                    questionInput.text?.clear()
                    answerInput.text?.clear()

                    Toast.makeText(
                        this,
                        "Flashcard saved!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { error ->
                if (!isFinishing && !isDestroyed) {
                    setSaving(false)

                    Toast.makeText(
                        this,
                        error.localizedMessage
                            ?: "Could not save. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun setSaving(value: Boolean) {
        saving = value

        saveButton.isEnabled = !value
        subjectInput.isEnabled = !value
        topicInput.isEnabled = !value
        questionInput.isEnabled = !value
        answerInput.isEnabled = !value

        saveButton.text =
            if (value) "Saving…" else "Save Flashcard"
    }

    private fun openLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }
}