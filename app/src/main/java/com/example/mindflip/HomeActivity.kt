package com.example.mindflip

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlin.math.roundToInt

class HomeActivity : AppCompatActivity() {

    private var listener: ListenerRegistration? = null

    private val subjectViews = mapOf(
        "CTE308" to Triple(
            R.id.textHomeCountCTE308,
            R.id.progressHomeCTE308,
            R.id.textHomeMasteryCTE308
        ),
        "EDP101" to Triple(
            R.id.textHomeCountEDP101,
            R.id.progressHomeEDP101,
            R.id.textHomeMasteryEDP101
        ),
        "ITM301" to Triple(
            R.id.textHomeCountITM301,
            R.id.progressHomeITM301,
            R.id.textHomeMasteryITM301
        ),
        "ITM302" to Triple(
            R.id.textHomeCountITM302,
            R.id.progressHomeITM302,
            R.id.textHomeMasteryITM302
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        BottomNavigationHelper.setup(this, R.id.nav_home)

        findViewById<View>(R.id.btnLogout).setOnClickListener {
            listener?.remove()
            listener = null

            FirebaseAuth.getInstance().signOut()

            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            startActivity(intent)
            finish()
        }
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

        showStatus("Loading…")

        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            openLogin()
            return
        }

        listener?.remove()

        listener = FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .collection("flashcards")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    showStatus("Unavailable")

                    Toast.makeText(
                        this,
                        "Could not load Home statistics. " +
                                "Reopen Home to retry.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    return@addSnapshotListener
                }

                val cards = snapshot.documents

                subjectViews.forEach { (subject, ids) ->
                    val subjectCards = cards.filter {
                        it.getString("subject") == subject
                    }

                    val total = subjectCards.size

                    val mastered = subjectCards.count {
                        it.getBoolean("mastered") == true
                    }

                    val percentage = if (total == 0) {
                        0
                    } else {
                        (
                                mastered.toDouble() / total * 100
                                ).roundToInt()
                    }

                    findViewById<TextView>(ids.first).text =
                        if (total == 1) "1 card" else "$total cards"

                    findViewById<ProgressBar>(ids.second).apply {
                        visibility = View.VISIBLE
                        max = 100
                        progress = percentage
                    }

                    findViewById<TextView>(ids.third).text =
                        if (total == 0) {
                            "No cards yet"
                        } else {
                            "$percentage% Mastered"
                        }
                }
            }
    }

    override fun onStop() {
        listener?.remove()
        listener = null
        super.onStop()
    }

    private fun showStatus(message: String) {
        subjectViews.values.forEach { ids ->
            findViewById<TextView>(ids.first).text = message

            findViewById<ProgressBar>(ids.second).visibility =
                View.INVISIBLE

            findViewById<TextView>(ids.third).text = ""
        }
    }

    private fun openLogin() {
        val intent = Intent(
            this,
            LoginActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }
}