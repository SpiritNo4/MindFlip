package com.example.mindflip

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlin.math.roundToInt

class ProgressActivity : AppCompatActivity() {

    private var cardListener: ListenerRegistration? = null
    private var quizListener: ListenerRegistration? = null

    private lateinit var totalText: TextView
    private lateinit var masteredText: TextView
    private lateinit var quizCountText: TextView
    private lateinit var averageText: TextView

    private val subjectViews = mapOf(
        "CTE308" to Pair(
            R.id.textProgressCTE308,
            R.id.progressCTE308
        ),
        "EDP101" to Pair(
            R.id.textProgressEDP101,
            R.id.progressEDP101
        ),
        "ITM301" to Pair(
            R.id.textProgressITM301,
            R.id.progressITM301
        ),
        "ITM302" to Pair(
            R.id.textProgressITM302,
            R.id.progressITM302
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)

        totalText = findViewById(R.id.textTotalCards)
        masteredText = findViewById(R.id.textCardsMastered)
        quizCountText = findViewById(R.id.textQuizzesCompleted)
        averageText = findViewById(R.id.textAverageScore)

        quizCountText.visibility = View.VISIBLE
        averageText.visibility = View.VISIBLE

        BottomNavigationHelper.setup(this, R.id.nav_progress)

        findViewById<MaterialButton>(
            R.id.btnContinueLearning
        ).setOnClickListener {
            val next = Intent(this, HomeActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }

            startActivity(next)
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        loadProgress()
    }

    override fun onStop() {
        cardListener?.remove()
        quizListener?.remove()
        cardListener = null
        quizListener = null
        super.onStop()
    }

    private fun loadProgress() {
        showCardStatus("Loading flashcard progress…")
        quizCountText.text = "Loading quiz results…"
        averageText.text = ""

        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            val next = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            startActivity(next)
            finish()
            return
        }

        val userDocument = FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)

        cardListener?.remove()
        quizListener?.remove()

        cardListener = userDocument.collection("flashcards")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    showCardStatus("Could not load flashcard progress.")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    displayCards(snapshot.documents)
                }
            }

        quizListener = userDocument.collection("quizResults")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    quizCountText.text = "Could not load quiz results."
                    averageText.text = "Reopen Progress to retry."
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val percentages = snapshot.documents.mapNotNull { doc ->
                        val score = doc.getLong("score")
                        val total = doc.getLong("total")

                        if (
                            score != null &&
                            total != null &&
                            total > 0 &&
                            score >= 0 &&
                            score <= total
                        ) {
                            score.toDouble() / total * 100
                        } else {
                            null
                        }
                    }

                    quizCountText.text =
                        "Quizzes completed: ${percentages.size}"

                    averageText.text = if (percentages.isEmpty()) {
                        "Average quiz score: No results yet"
                    } else {
                        val average = percentages.average().roundToInt()
                        "Average quiz score: $average%"
                    }
                }
            }
    }

    private fun displayCards(cards: List<DocumentSnapshot>) {
        val mastered = cards.count {
            it.getBoolean("mastered") == true
        }

        totalText.text = "Flashcards saved: ${cards.size}"
        masteredText.text = "Cards marked as mastered: $mastered"

        subjectViews.forEach { (subject, ids) ->
            val subjectCards = cards.filter {
                it.getString("subject") == subject
            }

            val total = subjectCards.size
            val masteredCount = subjectCards.count {
                it.getBoolean("mastered") == true
            }

            val percentage = if (total == 0) {
                0
            } else {
                (masteredCount.toDouble() / total * 100).roundToInt()
            }

            findViewById<TextView>(ids.first).text =
                if (total == 0) {
                    "$subject — No flashcards yet"
                } else {
                    "$subject — $percentage% " +
                            "($masteredCount of $total mastered)"
                }

            findViewById<ProgressBar>(ids.second).apply {
                visibility = View.VISIBLE
                max = 100
                progress = percentage
            }
        }
    }

    private fun showCardStatus(message: String) {
        totalText.text = message
        masteredText.text = ""

        subjectViews.forEach { (subject, ids) ->
            findViewById<TextView>(ids.first).text = subject
            findViewById<ProgressBar>(ids.second).visibility =
                View.INVISIBLE
        }
    }
}