package com.example.mindflip

import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class LibraryActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var loading: ProgressBar
    private lateinit var filterButton: MaterialButton

    private var listener: ListenerRegistration? = null
    private var cards: List<DocumentSnapshot> = emptyList()

    private val subjects = arrayOf(
        "All",
        "CTE308",
        "EDP101",
        "ITM301",
        "ITM302"
    )

    private var selectedSubject = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)

        container = findViewById(R.id.libraryCardsContainer)
        statusText = findViewById(R.id.textLibraryStatus)
        loading = findViewById(R.id.libraryLoading)
        filterButton = findViewById(R.id.btnFilter)

        selectedSubject =
            savedInstanceState?.getString("subjectFilter") ?: "All"

        filterButton.text = "Subject: $selectedSubject"
        filterButton.isEnabled = false

        BottomNavigationHelper.setup(this, R.id.nav_library)

        findViewById<MaterialButton>(
            R.id.btnAddFlashcard
        ).setOnClickListener {
            startActivity(
                Intent(this, NewFlashcardActivity::class.java)
            )
        }

        filterButton.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Choose a subject")
                .setSingleChoiceItems(
                    subjects,
                    subjects.indexOf(selectedSubject)
                ) { dialog, position ->
                    selectedSubject = subjects[position]
                    filterButton.text = "Subject: $selectedSubject"
                    renderCards()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onStart() {
        super.onStart()
        loadFlashcards()
    }

    override fun onStop() {
        listener?.remove()
        listener = null
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("subjectFilter", selectedSubject)
        super.onSaveInstanceState(outState)
    }

    private fun loadFlashcards() {
        val user = FirebaseAuth.getInstance().currentUser

        cards = emptyList()
        container.removeAllViews()
        filterButton.isEnabled = false

        if (user == null) {
            val intent = Intent(
                this,
                LoginActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            startActivity(intent)
            finish()
            return
        }

        loading.visibility = View.VISIBLE
        statusText.text = "Loading your flashcards…"

        listener?.remove()

        listener = FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .collection("flashcards")
            .addSnapshotListener { snapshot, error ->
                loading.visibility = View.GONE

                if (error != null) {
                    cards = emptyList()
                    container.removeAllViews()
                    filterButton.isEnabled = false
                    statusText.text =
                        "Could not load flashcards: ${error.localizedMessage}"
                    return@addSnapshotListener
                }

                cards = snapshot?.documents.orEmpty()
                    .sortedByDescending {
                        it.getTimestamp("createdAt")?.seconds ?: 0L
                    }

                filterButton.isEnabled = true
                renderCards()
            }
    }

    private fun renderCards() {
        container.removeAllViews()

        val visibleCards = cards.filter { card ->
            selectedSubject == "All" ||
                    card.getString("subject") == selectedSubject
        }

        statusText.text = when {
            cards.isEmpty() ->
                "No flashcards yet. Tap New Flashcard to create one."

            visibleCards.isEmpty() ->
                "No flashcards for $selectedSubject."

            else ->
                "${visibleCards.size} flashcards • Tap a card to see its answer"
        }

        visibleCards.forEach { document ->
            val subject = document.getString("subject").orEmpty()
            val topic = document.getString("topic").orEmpty()
            val question = document.getString("question").orEmpty()
            val answer = document.getString("answer").orEmpty()

            val cardView = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(14)
                }

                radius = dp(16).toFloat()
                cardElevation = dp(2).toFloat()

                setCardBackgroundColor(
                    ContextCompat.getColor(
                        this@LibraryActivity,
                        R.color.mindflip_white
                    )
                )

                isClickable = true
                isFocusable = true
            }

            val content = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(20), dp(20), dp(20), dp(20))
            }

            val subjectText = TextView(this).apply {
                text = "$subject • $topic"
                textSize = 13f
                setTextColor(
                    ContextCompat.getColor(
                        this@LibraryActivity,
                        R.color.mindflip_primary
                    )
                )
            }

            val questionText = TextView(this).apply {
                text = question
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setPadding(0, dp(12), 0, 0)

                setTextColor(
                    ContextCompat.getColor(
                        this@LibraryActivity,
                        R.color.mindflip_text_primary
                    )
                )
            }

            content.addView(subjectText)
            content.addView(questionText)
            cardView.addView(content)
            container.addView(cardView)

            cardView.setOnClickListener {
                MaterialAlertDialogBuilder(this)
                    .setTitle(question)
                    .setMessage(answer)
                    .setPositiveButton("Close", null)
                    .show()
                }
            cardView.setOnLongClickListener {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Flashcard options")
                    .setItems(arrayOf("Edit", "Delete")) { _, position ->
                        when (position) {
                            0 -> editFlashcard(document)
                            1 -> deleteFlashcard(document)
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }
        }
    }
    private fun editFlashcard(document: DocumentSnapshot) {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), dp(8))
        }

        val subjectLabel = TextView(this).apply {
            text = "Subject"
        }

        val subjectChoices = subjects.drop(1)

        val subjectSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@LibraryActivity,
                android.R.layout.simple_spinner_dropdown_item,
                subjectChoices
            )

            val currentIndex = subjectChoices.indexOf(
                document.getString("subject")
            )

            setSelection(currentIndex.coerceAtLeast(0))
        }

        fun createInput(label: String, value: String): EditText {
            return EditText(this).apply {
                hint = label
                setText(value)
                inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or
                            android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                            android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                minHeight = dp(56)
            }
        }

        val topicInput = createInput(
            "Topic",
            document.getString("topic").orEmpty()
        )

        val questionInput = createInput(
            "Question",
            document.getString("question").orEmpty()
        )

        val answerInput = createInput(
            "Answer",
            document.getString("answer").orEmpty()
        )

        form.addView(subjectLabel)
        form.addView(subjectSpinner)
        form.addView(topicInput)
        form.addView(questionInput)
        form.addView(answerInput)

        val scroll = android.widget.ScrollView(this).apply {
            addView(form)
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Edit flashcard")
            .setView(scroll)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val save = dialog.getButton(
                android.content.DialogInterface.BUTTON_POSITIVE
            )

            val cancel = dialog.getButton(
                android.content.DialogInterface.BUTTON_NEGATIVE
            )

            save.setOnClickListener {
                topicInput.error = null
                questionInput.error = null
                answerInput.error = null

                val topic = topicInput.text.toString().trim()
                val question = questionInput.text.toString().trim()
                val answer = answerInput.text.toString().trim()
                val subject = subjectSpinner.selectedItem.toString()

                var valid = true

                if (topic.isBlank()) {
                    topicInput.error = "Enter a topic"
                    valid = false
                }

                if (question.isBlank()) {
                    questionInput.error = "Enter a question"
                    valid = false
                }

                if (answer.isBlank()) {
                    answerInput.error = "Enter an answer"
                    valid = false
                }

                if (valid) {
                    save.isEnabled = false
                    cancel.isEnabled = false
                    save.text = "Saving…"
                    dialog.setCancelable(false)

                    val changes = hashMapOf<String, Any>(
                        "subject" to subject,
                        "topic" to topic,
                        "question" to question,
                        "answer" to answer
                    )

                    // Changed study content needs to be reviewed again.
                    if (
                        question != document.getString("question") ||
                        answer != document.getString("answer")
                    ) {
                        changes["mastered"] = false
                    }

                    document.reference.update(changes)
                        .addOnSuccessListener {
                            dialog.dismiss()

                            Toast.makeText(
                                this,
                                "Flashcard updated",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener { error ->
                            save.isEnabled = true
                            cancel.isEnabled = true
                            save.text = "Save"
                            dialog.setCancelable(true)

                            Toast.makeText(
                                this,
                                error.localizedMessage
                                    ?: "Could not update the flashcard.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
            }
        }

        dialog.show()
    }

    private fun deleteFlashcard(document: DocumentSnapshot) {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Delete flashcard?")
            .setMessage(
                "This will permanently delete:\n\n" +
                        document.getString("question").orEmpty()
            )
            .setPositiveButton("Delete", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val delete = dialog.getButton(
                android.content.DialogInterface.BUTTON_POSITIVE
            )

            val cancel = dialog.getButton(
                android.content.DialogInterface.BUTTON_NEGATIVE
            )

            delete.setOnClickListener {
                delete.isEnabled = false
                cancel.isEnabled = false
                delete.text = "Deleting…"
                dialog.setCancelable(false)

                document.reference.delete()
                    .addOnSuccessListener {
                        dialog.dismiss()

                        Toast.makeText(
                            this,
                            "Flashcard deleted",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { error ->
                        delete.isEnabled = true
                        cancel.isEnabled = true
                        delete.text = "Delete"
                        dialog.setCancelable(true)

                        Toast.makeText(
                            this,
                            error.localizedMessage
                                ?: "Could not delete the flashcard.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        }

        dialog.show()
    }
    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}