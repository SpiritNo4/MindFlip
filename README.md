# MindFlip

MindFlip is an Android learning app built with Kotlin, XML layouts,
Firebase Authentication, and Cloud Firestore.

## Features

- Email and password registration and login
- Password reset, persistent sign-in, and logout
- Create, edit, delete, and search personal flashcards
- Filter flashcards by subject
- Study cards and record mastery
- Practice multiple-choice quizzes
- Save quiz results and display learning progress

## Subjects

- CTE308
- EDP101
- ITM301
- ITM302

The current quiz bank contains two built-in questions per subject.
Quiz questions are separate from user-created flashcards.

## Requirements

- Android Studio compatible with the project's Gradle configuration
- Android SDK 37
- Android device or emulator running Android 7.0 (API 24) or later
- A Firebase project for authentication and database storage

## Setup

1. Clone this repository and open it in Android Studio.
2. Register an Android app in your Firebase project using:
   com.example.mindflip
3. Place your Firebase google-services.json file inside the app folder,
   replacing the existing configuration when using your own project.
4. Enable Email/Password in Firebase Authentication.
5. Create the default Cloud Firestore database.
6. Publish the rules from firestore.rules in the Firebase Console.
7. Sync Gradle and run the app.

## Data structure

Each user's data is stored under their Firebase Authentication UID:

- users/{uid}/flashcards/{cardId}
- users/{uid}/quizResults/{resultId}

Flashcards contain a subject, topic, question, answer, mastery status,
and creation timestamp.

Quiz results contain the subject, score, total questions, percentage,
and completion timestamp.

## Progress calculations

- Subject mastery: mastered cards divided by saved cards for that subject.
- Average quiz score: the mean percentage across saved quiz attempts.
- Flashcard mastery and quiz scores are tracked separately.

## Security

Firestore rules restrict client access to the signed-in user's own data.

Quiz scoring happens in the app. Scores are intended for personal
learning and are not verified exam results.

Never commit service-account private keys, signing keystores,
or passwords.

## Current limitations

- The quiz bank is small and defined in QuizData.kt.
- Completed quiz results are saved; unfinished quizzes are not stored
  as resumable sessions in Firestore.
- Internet access is needed for authentication and confirming cloud saves.