package com.example.mindflip

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PracticeQuizActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_practice_quiz)

        BottomNavigationHelper.setup(this, R.id.nav_home)

        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        val subjects = mapOf(
            R.id.cardQuizEDP101 to "EDP101",
            R.id.cardQuizCTE308 to "CTE308",
            R.id.cardQuizITM301 to "ITM301",
            R.id.cardQuizITM302 to "ITM302"
        )

        subjects.forEach { (id, subject) ->
            val card = findViewById<View>(id)

            updateQuestionCount(
                card,
                QuizData.questions(subject).size
            )

            card.setOnClickListener {
                val intent = Intent(
                    this,
                    QuizInfoActivity::class.java
                )

                intent.putExtra("subject", subject)
                startActivity(intent)
            }
        }
    }

    private fun updateQuestionCount(view: View, count: Int) {
        if (
            view is TextView &&
            view.text.toString().contains("Multiple choice")
        ) {
            view.text = "$count questions • Multiple choice"
        }

        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                updateQuestionCount(
                    view.getChildAt(index),
                    count
                )
            }
        }
    }
}