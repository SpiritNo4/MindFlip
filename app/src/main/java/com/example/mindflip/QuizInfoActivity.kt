package com.example.mindflip

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class QuizInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_info)

        val subject = intent.getStringExtra("subject").orEmpty()
        val count = QuizData.questions(subject).size

        findViewById<TextView>(
            R.id.textQuizSubject
        ).text = subject

        findViewById<TextView>(
            R.id.textQuizTitle
        ).text = "$subject Practice Quiz"

        findViewById<TextView>(
            R.id.textQuestionCount
        ).text = "Questions: $count"

        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.btnStartQuiz).apply {
            isEnabled = count > 0

            setOnClickListener {
                val intent = Intent(
                    this@QuizInfoActivity,
                    QuizQuestionActivity::class.java
                )
                intent.putExtra("subject", subject)
                startActivity(intent)
                finish()
            }
        }
    }
}