package com.example.mindflip

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.UUID

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class QuizCard(val question: String, val answer: String)

object QuizData {
    const val MAX_QUESTIONS = 20
    private fun key(value: String) = value.trim().lowercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ")

    // All choices come from this user's cards in the selected subject.
    // Other answers to the SAME question must never become distractors.
    fun buildQuestions(cards: List<QuizCard>): List<QuizQuestion> {
        val valid = cards.filter { it.question.isNotBlank() && it.answer.isNotBlank() }
        val answerPool = valid.map { it.answer.trim() }.distinctBy { key(it) }
        return valid.distinctBy { key(it.question) }.mapNotNull { card ->
            val accepted = valid.filter { key(it.question) == key(card.question) }
                .map { key(it.answer) }.toSet()
            val distractors = answerPool.filter { key(it) !in accepted }.shuffled().take(3)
            if (distractors.isEmpty()) return@mapNotNull null
            val answer = card.answer.trim()
            val choices = (distractors + answer).shuffled()
            QuizQuestion(card.question.trim(), choices, choices.indexOf(answer),
                "From your saved flashcard. Review any equivalent answers with your notes.")
        }.shuffled().take(MAX_QUESTIONS)
    }

    fun load(subject: String, callback: (List<QuizQuestion>?, String?) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            callback(null, "Please sign in to use your flashcards.")
            return
        }
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("flashcards").whereEqualTo("subject", subject).get()
            .addOnSuccessListener { snapshot ->
                if (FirebaseAuth.getInstance().currentUser?.uid != uid) {
                    callback(null, "Your account changed. Please reopen the quiz.")
                    return@addOnSuccessListener
                }
                val cards = snapshot.documents.map { doc ->
                    QuizCard(doc.getString("question").orEmpty(), doc.getString("answer").orEmpty())
                }
                val questions = buildQuestions(cards)
                val message = if (questions.isEmpty()) {
                    "Add at least two different questions with different answers in $subject, then try again. " +
                        if (snapshot.metadata.isFromCache) "Only downloaded cards are available offline." else ""
                } else null
                callback(questions, message)
            }
            .addOnFailureListener {
                callback(null, "Could not load flashcards. Check your connection and try again.")
            }
    }

    // Store the exact shuffled session privately on-device; only the ID crosses activities.
    // This avoids large Intent/Bundle payloads and preserves order after recreation.
    private fun sessionFile(context: Context, id: String): File {
        require(id.matches(Regex("[a-f0-9-]{36}")))
        val uid = requireNotNull(FirebaseAuth.getInstance().currentUser?.uid)
        val userFolder = uid.toByteArray().joinToString("") { "%02x".format(it) }
        val directory = File(context.noBackupFilesDir, "quiz_sessions/$userFolder")
        check(directory.exists() || directory.mkdirs())
        return File(directory, "$id.json")
    }

    fun saveSession(context: Context, subject: String, questions: List<QuizQuestion>): String {
        val id = UUID.randomUUID().toString()
        val rows = JSONArray()
        questions.forEach { q -> rows.put(JSONObject().put("question", q.question)
            .put("options", JSONArray(q.options)).put("correctIndex", q.correctIndex)
            .put("explanation", q.explanation)) }
        val json = JSONObject().put("subject", subject).put("questions", rows)
        sessionFile(context, id).writeText(json.toString())
        return id
    }

    fun readSession(context: Context, id: String): List<QuizQuestion> = runCatching {
        val rows = JSONObject(sessionFile(context, id).readText()).getJSONArray("questions")
        (0 until rows.length()).map { index ->
            val q = rows.getJSONObject(index)
            val options = q.getJSONArray("options")
            QuizQuestion(q.getString("question"), (0 until options.length()).map { options.getString(it) },
                q.getInt("correctIndex"), q.getString("explanation"))
        }
    }.getOrDefault(emptyList())
}
