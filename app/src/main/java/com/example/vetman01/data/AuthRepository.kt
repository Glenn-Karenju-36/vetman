package com.example.vetman01.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * AuthRepository handles everything to do with user accounts.
 * The screens never talk to Firebase directly. They call this class.
 * This keeps the UI separate from the data layer (see the architecture diagram).
 */
class AuthRepository(
    // Firebase Authentication: handles login, passwords and sessions
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    // Cloud Firestore: the database where we store each user's role
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Logs an existing user in with email and password.
     * Firebase works in the background, so the result comes back through onResult:
     * success if the login worked, failure (with the reason) if it didn't.
     */
    fun signIn(email: String, password: String, onResult: (Result<Unit>) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) onResult(Result.success(Unit))
                else onResult(Result.failure(task.exception ?: Exception("Sign-in failed")))
            }
    }

    /**
     * Creates a new account, then saves the user's role in Firestore.
     * role is either "veterinarian" or "pet_owner". The app uses it later
     * to decide which screens the user can see.
     */
    fun register(email: String, password: String, role: String, onResult: (Result<Unit>) -> Unit) {
        // Step 1: create the login in Firebase Authentication
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    // Registration failed (e.g. email already used, weak password)
                    onResult(Result.failure(task.exception ?: Exception("Registration failed")))
                    return@addOnCompleteListener
                }

                // Step 2: build the user's profile
                // uid is the unique ID Firebase gave this user
                val uid = auth.currentUser?.uid ?: ""
                val profile = mapOf("uid" to uid, "email" to email, "role" to role)

                // Step 3: save it in Firestore under users/{uid}
                db.collection("users").document(uid).set(profile)
                    .addOnSuccessListener { onResult(Result.success(Unit)) }
                    .addOnFailureListener { onResult(Result.failure(it)) }
            }
    }

    /**
     * Reads the logged-in user's role ("veterinarian" or "pet_owner")
     * from Firestore, where register() saved it under users/{uid}.
     */
    fun getUserRole(onResult: (Result<String>) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onResult(Result.failure(Exception("Not signed in")))
            return
        }
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role")
                if (role != null) onResult(Result.success(role))
                else onResult(Result.failure(Exception("No role found for this account")))
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    /** Logs the current user out. */
    fun signOut() = auth.signOut()
}