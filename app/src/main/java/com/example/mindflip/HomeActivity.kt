package com.example.mindflip

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            openLogin()
            return
        }

        setContentView(R.layout.activity_home)

        BottomNavigationHelper.setup(this, R.id.nav_home)

        findViewById<View>(
            R.id.cardNewFlashcard
        ).setOnClickListener {
            startActivity(
                Intent(this, NewFlashcardActivity::class.java)
            )
        }

        findViewById<View>(
            R.id.cardPracticeQuiz
        ).setOnClickListener {
            startActivity(
                Intent(this, PracticeQuizActivity::class.java)
            )
        }

        findViewById<View>(
            R.id.cardMyProgress
        ).setOnClickListener {
            startActivity(
                Intent(this, ProgressActivity::class.java)
            )
        }

        val subjectCards = mapOf(
            R.id.cardCTE308 to "CTE308",
            R.id.cardEDP101 to "EDP101",
            R.id.cardITM301 to "ITM301",
            R.id.cardITM302 to "ITM302"
        )

        subjectCards.forEach { (cardId, subject) ->
            findViewById<View>(cardId).setOnClickListener {
                val intent = Intent(
                    this,
                    FlashcardStudyActivity::class.java
                )

                intent.putExtra("subject", subject)
                startActivity(intent)
            }
        }
    }

    override fun onStart() {
        super.onStart()

        if (FirebaseAuth.getInstance().currentUser == null) {
            openLogin()
        }
    }

    private fun openLogin() {
        if (isFinishing) return

        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }
}