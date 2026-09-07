package com.example.mindflip

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class FlashcardStudyActivity : AppCompatActivity() {

    private lateinit var contentText: TextView
    private lateinit var sideText: TextView
    private lateinit var topicText: TextView
    private lateinit var countText: TextView
    private lateinit var hintText: TextView
    private lateinit var progress: ProgressBar

    private lateinit var flipButton: MaterialButton
    private lateinit var againButton: MaterialButton
    private lateinit var gotItButton: MaterialButton

    private var cards: List<DocumentSnapshot> = emptyList()
    private val reviewedIds = arrayListOf<String>()
    private val masteredIds = arrayListOf<String>()

    private var subject = ""
    private var showingAnswer = false
    private var answerViewed = false
    private var busy = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flashcard_study)

        applyScreenInsets()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    leaveStudy()
                }
            }
        )

        subject = intent.getStringExtra("subject").orEmpty()

        reviewedIds.addAll(
            savedInstanceState?.getStringArrayList("reviewedIds")
                ?: arrayListOf()
        )

        masteredIds.addAll(
            savedInstanceState?.getStringArrayList("masteredIds")
                ?: arrayListOf()
        )

        contentText = findViewById(R.id.textFlashcardContent)
        sideText = findViewById(R.id.textCardSide)
        topicText = findViewById(R.id.textStudyTopic)
        countText = findViewById(R.id.textCardCount)
        hintText = findViewById(R.id.textFlipHint)
        progress = findViewById(R.id.progressStudy)

        flipButton = findViewById(R.id.btnFlipCard)
        againButton = findViewById(R.id.btnStudyAgain)
        gotItButton = findViewById(R.id.btnGotIt)

        findViewById<TextView>(
            R.id.textStudySubject
        ).text = subject.ifBlank { "Flashcards" }

        findViewById<View>(R.id.btnBack).setOnClickListener {
            leaveStudy()
        }

        findViewById<View>(R.id.cardFlashcard).setOnClickListener {
            flipCard()
        }

        flipButton.setOnClickListener {
            flipCard()
        }

        againButton.setOnClickListener {
            rateCard(false)
        }

        gotItButton.setOnClickListener {
            rateCard(true)
        }

        loadCards()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList(
            "reviewedIds",
            ArrayList(reviewedIds)
        )

        outState.putStringArrayList(
            "masteredIds",
            ArrayList(masteredIds)
        )

        super.onSaveInstanceState(outState)
    }

    private fun leaveStudy() {
        if (isFinishing) return

        // If there is no previous screen, open Home.
        if (isTaskRoot) {
            val next = Intent(
                this,
                HomeActivity::class.java
            ).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }

            startActivity(next)
        }

        finish()
    }

    private fun applyScreenInsets() {
        val content = findViewById<ViewGroup>(android.R.id.content)
        val root = content.getChildAt(0)

        val originalLeft = root.paddingLeft
        val originalTop = root.paddingTop
        val originalRight = root.paddingRight
        val originalBottom = root.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val safeArea = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout()
            )

            view.setPadding(
                originalLeft + safeArea.left,
                originalTop + safeArea.top,
                originalRight + safeArea.right,
                originalBottom + safeArea.bottom
            )

            WindowInsetsCompat.CONSUMED
        }

        root.post {
            ViewCompat.requestApplyInsets(root)
        }
    }

    private fun loadCards() {
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            val next = Intent(
                this,
                LoginActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            startActivity(next)
            finish()
            return
        }

        busy = true
        updateButtons()

        contentText.text = "Loading flashcards…"
        sideText.text = ""
        topicText.text = ""
        countText.text = ""
        hintText.text = ""
        progress.progress = 0

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .collection("flashcards")
            .whereEqualTo("subject", subject)
            .get()
            .addOnSuccessListener { snapshot ->
                if (isFinishing || isDestroyed) {
                    return@addOnSuccessListener
                }

                cards = snapshot.documents
                    .sortedBy { it.id }
                    .filter { it.id !in reviewedIds }

                busy = false

                if (cards.isEmpty()) {
                    if (reviewedIds.isNotEmpty()) {
                        openSummary()
                    } else {
                        contentText.text =
                            "No flashcards for this subject yet."

                        hintText.text =
                            "Go back and create a card for $subject."

                        countText.text = "0 cards"

                        updateButtons()
                    }
                } else {
                    showQuestion()
                }
            }
            .addOnFailureListener { error ->
                if (isFinishing || isDestroyed) {
                    return@addOnFailureListener
                }

                busy = false
                contentText.text = "Could not load flashcards."
                hintText.text = "Go back and try again."

                updateButtons()

                showMessage(
                    error.localizedMessage ?: "Please try again."
                )
            }
    }

    private fun showQuestion() {
        val card = cards.firstOrNull() ?: return

        showingAnswer = false
        answerViewed = false

        val total = reviewedIds.size + cards.size

        topicText.text = card.getString("topic").orEmpty()
        sideText.text = "QUESTION"
        contentText.text = card.getString("question").orEmpty()
        hintText.text = "Tap the card to reveal the answer"
        countText.text = "${reviewedIds.size + 1} / $total"

        progress.max = total
        progress.progress = reviewedIds.size

        flipButton.text = "Show Answer"

        updateButtons()
    }

    private fun flipCard() {
        if (busy) return

        val card = cards.firstOrNull() ?: return

        showingAnswer = !showingAnswer

        if (showingAnswer) {
            answerViewed = true

            sideText.text = "ANSWER"
            contentText.text = card.getString("answer").orEmpty()
            hintText.text = "How well did you remember?"
            flipButton.text = "Show Question"
        } else {
            sideText.text = "QUESTION"
            contentText.text = card.getString("question").orEmpty()
            hintText.text = "Tap the card to reveal the answer"
            flipButton.text = "Show Answer"
        }

        updateButtons()
    }

    private fun rateCard(mastered: Boolean) {
        if (busy || !answerViewed) return

        val card = cards.firstOrNull() ?: return

        busy = true
        hintText.text = "Saving your progress…"

        updateButtons()

        card.reference.update("mastered", mastered)
            .addOnSuccessListener {
                if (isFinishing || isDestroyed) {
                    return@addOnSuccessListener
                }

                reviewedIds.add(card.id)

                if (mastered) {
                    masteredIds.add(card.id)
                }

                cards = cards.drop(1)
                busy = false

                if (cards.isEmpty()) {
                    openSummary()
                } else {
                    showQuestion()
                }
            }
            .addOnFailureListener { error ->
                if (isFinishing || isDestroyed) {
                    return@addOnFailureListener
                }

                busy = false
                hintText.text =
                    "Progress could not be saved. Tap your rating to retry."

                updateButtons()

                showMessage(
                    error.localizedMessage ?: "Could not save progress."
                )
            }
    }

    private fun updateButtons() {
        val ready = !busy && cards.isNotEmpty()

        flipButton.isEnabled = ready
        againButton.isEnabled = ready && answerViewed
        gotItButton.isEnabled = ready && answerViewed
    }

    private fun openSummary() {
        val next = Intent(
            this,
            FlashcardCompleteActivity::class.java
        )

        next.putExtra("subject", subject)
        next.putExtra("reviewed", reviewedIds.size)
        next.putExtra("mastered", masteredIds.size)

        startActivity(next)
        finish()
    }

    private fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}