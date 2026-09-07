package com.example.mindflip

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class QuizInfoActivity : AppCompatActivity() {
    private var questions: List<QuizQuestion> = emptyList()
    private var loading = false
    private var launching = false
    private var subject = ""
    private lateinit var startButton: MaterialButton
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_info)
        subject = intent.getStringExtra("subject").orEmpty()
        findViewById<TextView>(R.id.textQuizSubject).text = subject
        findViewById<TextView>(R.id.textQuizTitle).text = "$subject • Your Flashcards"
        status = findViewById(R.id.textQuestionCount)
        startButton = findViewById(R.id.btnStartQuiz)
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        startButton.setOnClickListener {
            if (loading || launching) return@setOnClickListener
            if (questions.isEmpty()) {
                loadQuestions()
            } else {
                launching = true
                startButton.isEnabled = false
                runCatching { QuizData.saveSession(this, subject, questions) }
                    .onSuccess { sessionId ->
                        startActivity(Intent(this, QuizQuestionActivity::class.java).apply {
                            putExtra("subject", subject)
                            putExtra("sessionId", sessionId)
                        })
                        finish()
                    }.onFailure {
                        launching = false
                        startButton.isEnabled = true
                        status.text = "Could not prepare the quiz. Check device storage and try again."
                    }
            }
        }
        loadQuestions()
    }

    private fun loadQuestions() {
        loading = true
        startButton.isEnabled = false
        status.text = "Loading your flashcards…"
        QuizData.load(subject) { loaded, message ->
            if (isFinishing || isDestroyed) return@load
            loading = false
            questions = loaded.orEmpty()
            status.text = message ?: "${questions.size} questions • Shuffled from your cards (up to 20)"
            startButton.text = if (questions.isEmpty()) "Try Again" else "Start Quiz"
            startButton.isEnabled = true
        }
    }
}
