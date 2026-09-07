package com.example.mindflip

import org.junit.Assert.*
import org.junit.Test

class QuizDataTest {
    @Test fun emptyAndSingleAnswerCannotCreateMultipleChoiceQuiz() {
        assertTrue(QuizData.buildQuestions(emptyList()).isEmpty())
        assertTrue(QuizData.buildQuestions(listOf(QuizCard("Q", "A"))).isEmpty())
        assertTrue(QuizData.buildQuestions(listOf(
            QuizCard("Q1", "Yes"), QuizCard("Q2", " yes "))).isEmpty())
    }

    @Test fun twoCardsHaveTwoChoicesAndCorrectAnswerSurvivesShuffling() {
        val cards = listOf(QuizCard("France capital?", "Paris"), QuizCard("Japan capital?", "Tokyo"))
        repeat(30) {
            val questions = QuizData.buildQuestions(cards)
            assertEquals(2, questions.size)
            questions.forEach { q ->
                assertEquals(2, q.options.size)
                assertEquals(cards.first { it.question == q.question }.answer, q.options[q.correctIndex])
            }
        }
    }

    @Test fun alternateAnswersToIdenticalQuestionAreNotWrongChoices() {
        val cards = listOf(QuizCard("Name a fruit", "Apple"),
            QuizCard(" name A fruit ", "Banana"), QuizCard("Name a metal", "Iron"))
        repeat(30) {
            val question = QuizData.buildQuestions(cards).first { it.question == "Name a fruit" }
            assertEquals(setOf("Apple", "Iron"), question.options.toSet())
        }
    }

    @Test fun ignoresBlanksLimitsSessionAndUsesOnlySavedAnswers() {
        val cards = (1..30).map { QuizCard("Question $it", "Answer $it") } + QuizCard("", "Invalid")
        val questions = QuizData.buildQuestions(cards)
        assertEquals(20, questions.size)
        assertEquals(20, questions.map { it.question }.distinct().size)
        questions.forEach { q ->
            assertEquals(4, q.options.size)
            assertEquals(4, q.options.distinct().size)
            assertFalse(q.options.contains("Invalid"))
            assertEquals(cards.first { it.question == q.question }.answer, q.options[q.correctIndex])
        }
    }
}
