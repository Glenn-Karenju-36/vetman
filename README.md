# Vetman

Practice-management app for mobile veterinarians in Kenya. Vets photograph paper patient records; the app reads them with OCR and fills in the patient fields for review before saving.

## Tech stack
- Kotlin, Jetpack Compose, Material 3
- Firebase Authentication and Cloud Firestore
- CameraX and Google ML Kit text recognition (on-device)
- Rule-based field extraction (regex, keyword anchoring, fuzzy matching, veterinary term dictionary)

## Requirements
- Android Studio (latest stable)
- Android 8.0 (API 26) or higher
- A Firebase project

## Setup
1. Clone the repo:  git clone https://github.com/Glenn-Karenju-36/vetman.git
2. Open it in Android Studio and let Gradle sync.
3. Create a Firebase project, register the app with package name com.example.vetman, and download google-services.json.
4. Put google-services.json in the app/ folder. It is git-ignored and must never be committed.
5. Run the app on an emulator or device.

## Project status
Sprint 1: environment setup and project foundation.

## Author
Glenn Karenju, Strathmore University.