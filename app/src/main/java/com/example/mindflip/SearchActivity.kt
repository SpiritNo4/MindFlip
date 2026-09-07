package com.example.mindflip

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class SearchActivity : AppCompatActivity() {

    private lateinit var searchInput: TextInputEditText
    private lateinit var chipGroup: ChipGroup
    private lateinit var resultsContainer: LinearLayout
    private lateinit var resultCount: TextView
    private lateinit var emptyText: TextView

    private var cards: List<DocumentSnapshot> = emptyList()
    private var listener: ListenerRegistration? = null
    private var dataLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        searchInput = findViewById(R.id.editSearch)
        chipGroup = findViewById(R.id.chipGroupSubjects)
        resultsContainer = findViewById(R.id.searchResultsContainer)
        resultCount = findViewById(R.id.textSearchResultCount)
        emptyText = findViewById(R.id.textNoResults)

        // Remove the sample result included in the XML.
        resultsContainer.removeAllViews()

        BottomNavigationHelper.setup(this, R.id.nav_search)

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                text: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                text: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                if (dataLoaded) {
                    displayResults()
                }
            }

            override fun afterTextChanged(text: Editable?) {
            }
        })

        chipGroup.setOnCheckedStateChangeListener { _, _ ->
            if (dataLoaded) {
                displayResults()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        loadCards()
    }

    override fun onStop() {
        listener?.remove()
        listener = null
        super.onStop()
    }

    private fun loadCards() {
        dataLoaded = false
        cards = emptyList()
        resultsContainer.removeAllViews()
        emptyText.visibility = View.GONE
        resultCount.text = "Loading your flashcards…"

        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            val intent = Intent(
                this,
                LoginActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            startActivity(intent)
            finish()
            return
        }

        listener?.remove()

        listener = FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .collection("flashcards")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    dataLoaded = false
                    cards = emptyList()
                    resultsContainer.removeAllViews()

                    resultCount.text = "Could not load flashcards"

                    emptyText.text =
                        "Check your connection, then reopen Search to retry."

                    emptyText.visibility = View.VISIBLE
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    cards = snapshot.documents
                    dataLoaded = true
                    displayResults()
                }
            }
    }

    private fun displayResults() {
        resultsContainer.removeAllViews()

        val query = searchInput.text.toString().trim()

        val selectedSubject = when (chipGroup.checkedChipId) {
            R.id.chipCTE308 -> "CTE308"
            R.id.chipEDP101 -> "EDP101"
            R.id.chipITM301 -> "ITM301"
            R.id.chipITM302 -> "ITM302"
            else -> null
        }

        val matches = cards.filter { card ->
            val subject = card.getString("subject").orEmpty()
            val topic = card.getString("topic").orEmpty()
            val question = card.getString("question").orEmpty()

            val subjectMatches =
                selectedSubject == null || subject == selectedSubject

            val textMatches =
                query.isBlank() ||
                        subject.contains(query, ignoreCase = true) ||
                        topic.contains(query, ignoreCase = true) ||
                        question.contains(query, ignoreCase = true)

            subjectMatches && textMatches
        }.sortedBy {
            it.getString("question").orEmpty().lowercase()
        }

        resultCount.text =
            if (matches.size == 1) {
                "1 flashcard found"
            } else {
                "${matches.size} flashcards found"
            }

        emptyText.visibility =
            if (matches.isEmpty()) View.VISIBLE else View.GONE

        emptyText.text =
            if (cards.isEmpty()) {
                "No flashcards yet. Create one from Home."
            } else {
                "No matches. Try another keyword or choose All."
            }

        matches.forEach { card ->
            addResult(card)
        }
    }

    private fun addResult(document: DocumentSnapshot) {
        val subject = document.getString("subject").orEmpty()
        val topic = document.getString("topic").orEmpty()
        val question = document.getString("question").orEmpty()
        val answer = document.getString("answer").orEmpty()

        val cardView = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }

            radius = dp(16).toFloat()
            cardElevation = dp(2).toFloat()

            setCardBackgroundColor(
                ContextCompat.getColor(
                    this@SearchActivity,
                    R.color.mindflip_white
                )
            )

            isClickable = true
            isFocusable = true
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        val subjectText = TextView(this).apply {
            text = "$subject • $topic"
            textSize = 13f

            setTextColor(
                ContextCompat.getColor(
                    this@SearchActivity,
                    R.color.mindflip_primary
                )
            )
        }

        val questionText = TextView(this).apply {
            text = question
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, dp(12), 0, 0)

            setTextColor(
                ContextCompat.getColor(
                    this@SearchActivity,
                    R.color.mindflip_text_primary
                )
            )
        }

        val hintText = TextView(this).apply {
            text = "Tap to see the answer"
            textSize = 13f
            setPadding(0, dp(12), 0, 0)

            setTextColor(
                ContextCompat.getColor(
                    this@SearchActivity,
                    R.color.mindflip_text_secondary
                )
            )
        }

        content.addView(subjectText)
        content.addView(questionText)
        content.addView(hintText)

        cardView.addView(content)
        resultsContainer.addView(cardView)

        cardView.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(question)
                .setMessage(answer)
                .setPositiveButton("Close", null)
                .show()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}