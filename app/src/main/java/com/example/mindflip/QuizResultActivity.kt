package com.example.mindflip

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.LinearLayout
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date
import java.util.UUID
import kotlin.math.roundToInt

class QuizResultActivity : AppCompatActivity() {

    private var resultId = ""
    private var completedAtMillis = 0L
    private var saving = false
    private var saved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_result)

        val subject = intent.getStringExtra("subject").orEmpty()
        val total = intent.getIntExtra("total", 0).coerceAtLeast(0)
        val score = intent.getIntExtra("score", 0).coerceIn(0, total)

        resultId = savedInstanceState?.getString("resultId")
            ?: intent.getStringExtra("resultId")
                    ?: UUID.randomUUID().toString()

        completedAtMillis =
            savedInstanceState?.getLong("completedAtMillis")
                ?: intent.getLongExtra(
                    "completedAtMillis",
                    System.currentTimeMillis()
                )

        saved = savedInstanceState?.getBoolean("resultSaved") ?: false

        intent.putExtra("resultId", resultId)
        intent.putExtra("completedAtMillis", completedAtMillis)

        val percentage = if (total == 0) {
            0
        } else {
            (score.toDouble() / total * 100).roundToInt()
        }

        findViewById<TextView>(
            R.id.textResultSubject
        ).text = "$subject Practice Quiz"

        findViewById<TextView>(
            R.id.textScorePercentage
        ).text = "$percentage%"

        findViewById<TextView>(
            R.id.textScoreSummary
        ).text = "$score out of $total correct"

        findViewById<TextView>(
            R.id.textCorrectCount
        ).text = score.toString()

        findViewById<TextView>(
            R.id.textIncorrectCount
        ).text = (total - score).toString()

        findViewById<TextView>(
            R.id.textResultMessage
        ).text = when {
            total == 0 -> "No questions were completed."
            score == total ->
                "Well done! You answered every question correctly."
            score > 0 ->
                "Good effort. Review the explanations and try again."
            else ->
                "Keep practising. Use the explanations to guide your review."
        }

        findViewById<MaterialButton>(
            R.id.btnRetryQuiz
        ).setOnClickListener {
            val next = Intent(this, QuizInfoActivity::class.java)
            next.putExtra("subject", subject)
            startActivity(next)
            finish()
        }

        findViewById<MaterialButton>(
            R.id.btnChooseAnotherQuiz
        ).setOnClickListener {
            openScreen(PracticeQuizActivity::class.java)
        }

        findViewById<MaterialButton>(
            R.id.btnBackHome
        ).setOnClickListener {
            openScreen(HomeActivity::class.java)
        }

        showAnswerReview()

        if (total > 0 && !saved) {
            saveResult(subject, score, total)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("resultId", resultId)
        outState.putLong("completedAtMillis", completedAtMillis)
        outState.putBoolean("resultSaved", saved)
        super.onSaveInstanceState(outState)
    }

    private fun saveResult(subject: String, score: Int, total: Int) {
        if (saving || saved) return

        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            Snackbar.make(
                findViewById(android.R.id.content),
                "Result not saved. Please sign in again.",
                Snackbar.LENGTH_LONG
            ).show()
            return
        }

        saving = true

        val result = hashMapOf<String, Any>(
            "subject" to subject,
            "score" to score,
            "total" to total,
            "percentage" to (score.toDouble() / total * 100),
            "completedAt" to Timestamp(Date(completedAtMillis))
        )

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .collection("quizResults")
            .document(resultId)
            .set(result)
            .addOnSuccessListener {
                saving = false
                saved = true

                if (!isFinishing && !isDestroyed) {
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Quiz result saved",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener {
                saving = false

                if (!isFinishing && !isDestroyed) {
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Could not save quiz result.",
                        Snackbar.LENGTH_INDEFINITE
                    ).setAction("Retry") {
                        saveResult(subject, score, total)
                    }.show()
                }
            }
    }

    private fun showAnswerReview() {
        val sessionId = intent.getStringExtra("sessionId").orEmpty()
        val questions = QuizData.readSession(this, sessionId)
        val responses = intent.getIntArrayExtra("responses") ?: return
        if (questions.isEmpty() || responses.size != questions.size) return
        val container = findViewById<LinearLayout>(R.id.quizReviewContainer)
        questions.forEachIndexed { index, question ->
            val selected = responses[index]
            if (selected != question.correctIndex) {
                container.addView(TextView(this).apply {
                    text = "Question ${index + 1}: ${question.question}\n\n" +
                        "Your answer: ${question.options.getOrNull(selected) ?: "Not answered"}\n\n" +
                        "Correct answer: ${question.options[question.correctIndex]}"
                    textSize = 16f
                    setTextColor(androidx.core.content.ContextCompat.getColor(
                        this@QuizResultActivity, R.color.mindflip_text_primary))
                    val spacing = (16 * resources.displayMetrics.density).toInt()
                    setPadding(0, spacing, 0, spacing)
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                })
            }
        }
        if (container.childCount == 0) {
            container.addView(TextView(this).apply {
                text = "No missed questions. Well done!"
                setTextColor(androidx.core.content.ContextCompat.getColor(
                    this@QuizResultActivity, R.color.mindflip_text_primary))
            })
        }
    }

    private fun openScreen(destination: Class<*>) {
        val next = Intent(this, destination).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }

        startActivity(next)
        finish()
    }
}