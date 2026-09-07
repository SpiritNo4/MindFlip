package com.example.mindflip

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class QuizQuestionActivity : AppCompatActivity() {

    private lateinit var answersGroup: RadioGroup
    private lateinit var options: List<RadioButton>
    private lateinit var checkButton: MaterialButton
    private lateinit var nextButton: MaterialButton
    private lateinit var feedbackCard: MaterialCardView

    private var subject = ""
    private var questions: List<QuizQuestion> = emptyList()

    private var index = 0
    private var score = 0
    private var checked = false
    private var selectedOption = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_question)

        subject = intent.getStringExtra("subject").orEmpty()
        questions = QuizData.questions(subject)

        if (questions.isEmpty()) {
            Toast.makeText(
                this,
                "No quiz questions available.",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        index = savedInstanceState?.getInt("index") ?: 0
        score = savedInstanceState?.getInt("score") ?: 0
        checked = savedInstanceState?.getBoolean("checked") ?: false
        selectedOption =
            savedInstanceState?.getInt("selectedOption", -1) ?: -1

        answersGroup = findViewById(R.id.radioGroupAnswers)

        options = listOf(
            findViewById<RadioButton>(R.id.optionA),
            findViewById<RadioButton>(R.id.optionB),
            findViewById<RadioButton>(R.id.optionC),
            findViewById<RadioButton>(R.id.optionD)
        )

        checkButton = findViewById(R.id.btnCheckAnswer)
        nextButton = findViewById(R.id.btnNextQuestion)
        feedbackCard = findViewById(R.id.cardAnswerFeedback)

        findViewById<TextView>(
            R.id.textQuizSubject
        ).text = "$subject Quiz"

        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        checkButton.setOnClickListener {
            checkAnswer()
        }

        nextButton.setOnClickListener {
            if (checked) {
                if (index == questions.lastIndex) {
                    openResult()
                } else {
                    index++
                    checked = false
                    selectedOption = -1
                    displayQuestion()
                }
            }
        }

        displayQuestion()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::answersGroup.isInitialized) {
            selectedOption = options.indexOfFirst {
                it.id == answersGroup.checkedRadioButtonId
            }
        }

        outState.putInt("index", index)
        outState.putInt("score", score)
        outState.putBoolean("checked", checked)
        outState.putInt("selectedOption", selectedOption)

        super.onSaveInstanceState(outState)
    }

    private fun displayQuestion() {
        val question = questions[index]

        findViewById<TextView>(
            R.id.textQuestionNumber
        ).text = "Question ${index + 1} of ${questions.size}"

        findViewById<TextView>(
            R.id.textQuestion
        ).text = question.question

        findViewById<ProgressBar>(R.id.progressQuiz).apply {
            max = questions.size
            progress = index + 1
        }

        answersGroup.clearCheck()

        val letters = listOf("A", "B", "C", "D")

        options.forEachIndexed { position, button ->
            button.text =
                "${letters[position]}. ${question.options[position]}"
            button.isEnabled = !checked
        }

        if (selectedOption in options.indices) {
            answersGroup.check(options[selectedOption].id)
        }

        feedbackCard.visibility = View.GONE
        checkButton.visibility = View.VISIBLE
        nextButton.visibility = View.GONE

        if (checked) {
            displayFeedback()
        }
    }

    private fun checkAnswer() {
        if (checked) return

        selectedOption = options.indexOfFirst {
            it.id == answersGroup.checkedRadioButtonId
        }

        if (selectedOption == -1) {
            Toast.makeText(
                this,
                "Select an answer first.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        checked = true

        if (selectedOption == questions[index].correctIndex) {
            score++
        }

        displayFeedback()
    }

    private fun displayFeedback() {
        val question = questions[index]
        val correct = selectedOption == question.correctIndex

        options.forEach {
            it.isEnabled = false
        }

        findViewById<TextView>(
            R.id.textAnswerFeedback
        ).text = if (correct) {
            "Correct!"
        } else {
            "Not quite."
        }

        findViewById<TextView>(
            R.id.textAnswerExplanation
        ).text =
            "Correct answer: " +
                    question.options[question.correctIndex] +
                    "\n\n" +
                    question.explanation

        val background = if (correct) {
            R.color.mindflip_green_light
        } else {
            R.color.mindflip_pink_light
        }

        feedbackCard.setCardBackgroundColor(
            ContextCompat.getColor(this, background)
        )

        feedbackCard.visibility = View.VISIBLE
        checkButton.visibility = View.GONE
        nextButton.visibility = View.VISIBLE

        nextButton.text = if (index == questions.lastIndex) {
            "See Results"
        } else {
            "Next Question"
        }
    }

    private fun openResult() {
        val intent = Intent(
            this,
            QuizResultActivity::class.java
        )

        intent.putExtra("subject", subject)
        intent.putExtra("score", score)
        intent.putExtra("total", questions.size)

        startActivity(intent)
        finish()
    }
}