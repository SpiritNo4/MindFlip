package com.example.mindflip

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class FlashcardCompleteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flashcard_complete)

        val subject = intent.getStringExtra("subject").orEmpty()
        val reviewed = intent.getIntExtra("reviewed", 0)
        val mastered = intent.getIntExtra("mastered", 0)
        val needsPractice = reviewed - mastered

        findViewById<TextView>(
            R.id.textCompletionMessage
        ).text = "You finished reviewing your $subject flashcards."

        findViewById<TextView>(
            R.id.textReviewedCount
        ).text = reviewed.toString()

        findViewById<TextView>(
            R.id.textGotItCount
        ).text = mastered.toString()

        findViewById<TextView>(
            R.id.textNeedsPracticeCount
        ).text = needsPractice.toString()

        findViewById<MaterialButton>(
            R.id.btnReviewAgain
        ).setOnClickListener {
            val intent = Intent(
                this,
                FlashcardStudyActivity::class.java
            )

            intent.putExtra("subject", subject)
            startActivity(intent)
            finish()
        }

        findViewById<MaterialButton>(
            R.id.btnBackHome
        ).setOnClickListener {
            val intent = Intent(
                this,
                HomeActivity::class.java
            ).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }

            startActivity(intent)
            finish()
        }
    }
}