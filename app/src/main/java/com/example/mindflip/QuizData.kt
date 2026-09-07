package com.example.mindflip

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

object QuizData {

    fun questions(subject: String): List<QuizQuestion> {
        return when (subject) {
            "EDP101" -> listOf(
                QuizQuestion(
                    "Which statement best describes entrepreneurship?",
                    listOf(
                        "Pursuing an opportunity by creating and managing a venture",
                        "Avoiding every possible business risk",
                        "Ignoring customer needs",
                        "Guaranteeing a profit"
                    ),
                    0,
                    "Entrepreneurship involves pursuing opportunities, " +
                            "creating value, and managing uncertainty."
                ),
                QuizQuestion(
                    "Why should a new business research its customers?",
                    listOf(
                        "To eliminate every competitor",
                        "To guarantee immediate profit",
                        "To understand customer needs",
                        "To avoid testing its product"
                    ),
                    2,
                    "Customer research helps a business understand needs " +
                            "and make better product decisions."
                )
            )

            "CTE308" -> listOf(
                QuizQuestion(
                    "What does supervised learning use during training?",
                    listOf(
                        "Only random numbers",
                        "Examples paired with target labels or values",
                        "No training data",
                        "Only unlabelled examples"
                    ),
                    1,
                    "Supervised learning learns from inputs paired " +
                            "with known target outputs."
                ),
                QuizQuestion(
                    "Which task is an example of classification?",
                    listOf(
                        "Predicting tomorrow's temperature",
                        "Estimating a house price",
                        "Predicting a person's height",
                        "Labelling an email as spam or not spam"
                    ),
                    3,
                    "Classification assigns an input to a category, " +
                            "such as spam or not spam."
                )
            )

            "ITM301" -> listOf(
                QuizQuestion(
                    "Which action better protects personal information?",
                    listOf(
                        "Publishing private records",
                        "Sharing passwords with everyone",
                        "Restricting access to authorised people",
                        "Collecting unnecessary sensitive data"
                    ),
                    2,
                    "Restricting access helps protect personal " +
                            "information from unauthorised use."
                ),
                QuizQuestion(
                    "Why should an AI system be checked for unfair bias?",
                    listOf(
                        "It may treat groups unfairly",
                        "AI systems can never make mistakes",
                        "Bias always improves accuracy",
                        "Testing removes the need for human judgment"
                    ),
                    0,
                    "Bias can lead to unfair outcomes, so systems " +
                            "should be evaluated across relevant groups."
                )
            )

            "ITM302" -> listOf(
                QuizQuestion(
                    "What is the purpose of file permissions?",
                    listOf(
                        "To increase screen brightness",
                        "To control access to files",
                        "To change the computer's clock",
                        "To guarantee internet access"
                    ),
                    1,
                    "File permissions control which users can " +
                            "perform actions such as reading or writing."
                ),
                QuizQuestion(
                    "What does the principle of least privilege mean?",
                    listOf(
                        "Give every user administrator access",
                        "Remove all passwords",
                        "Make all files public",
                        "Grant only the access needed for a task"
                    ),
                    3,
                    "Least privilege limits access to what a user " +
                            "or process needs to do its work."
                )
            )

            else -> emptyList()
        }
    }
}