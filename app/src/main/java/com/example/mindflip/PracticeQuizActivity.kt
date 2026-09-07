package com.example.mindflip

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class PracticeQuizActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_practice_quiz)

        applyScreenInsets()

        BottomNavigationHelper.setup(this, R.id.nav_home)

        findViewById<View>(R.id.btnBack).setOnClickListener {
            returnHome()
        }

        // Also handle the device Back button or back gesture.
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    returnHome()
                }
            }
        )

        val subjects = mapOf(
            R.id.cardQuizEDP101 to "EDP101",
            R.id.cardQuizCTE308 to "CTE308",
            R.id.cardQuizITM301 to "ITM301",
            R.id.cardQuizITM302 to "ITM302"
        )

        subjects.forEach { (id, subject) ->
            val card = findViewById<View>(id)

            updateQuestionCount(card)

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

    private fun returnHome() {
        if (isFinishing) return

        val intent = Intent(this, HomeActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }

        startActivity(intent)
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

    private fun updateQuestionCount(view: View) {
        if (
            view is TextView &&
            view.text.toString().contains("Multiple choice")
        ) {
            view.text = "Your flashcards • Multiple choice"
        }

        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                updateQuestionCount(
                    view.getChildAt(index)
                )
            }
        }
    }
}