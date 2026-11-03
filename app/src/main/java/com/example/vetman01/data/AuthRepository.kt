
package com.example.vetman01.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

/**
 * Handles account registration, login, user roles and owner lookup.
 *
 * Firebase Authentication manages passwords and sessions.
 * Firestore stores user profiles under users/{uid}.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Signs in an existing user using email and password.
     */
    fun signIn(
        email: String,
        password: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(Result.success(Unit))
                } else {
                    onResult(
                        Result.failure(
                            task.exception ?: Exception("Sign-in failed")
                        )
                    )
                }
            }
    }

    /**
     * Registers a new account and saves its role in Firestore.
     */
    fun register(
        email: String,
        password: String,
        role: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->

                if (!task.isSuccessful) {
                    onResult(
                        Result.failure(
                            task.exception ?: Exception("Registration failed")
                        )
                    )
                    return@addOnCompleteListener
                }

                // Use Firebase's UID as the user's document ID.
                val uid = auth.currentUser?.uid

                if (uid == null) {
                    onResult(Result.failure(Exception("User ID missing")))
                    return@addOnCompleteListener
                }

                // Normalize email addresses for consistent lookup.
                val normalizedEmail = email.trim()
                    .lowercase(Locale.ROOT)

                val profile = mapOf(
                    "uid" to uid,
                    "email" to normalizedEmail,
                    "role" to role
                )

                // Save the user's role and email under users/{uid}.
                db.collection("users").document(uid)
                    .set(profile)
                    .addOnSuccessListener {
                        onResult(Result.success(Unit))
                    }
                    .addOnFailureListener {
                        onResult(Result.failure(it))
                    }
            }
    }

    /**
     * Retrieves the logged-in user's role from Firestore.
     */
    fun getUserRole(
        onResult: (Result<String>) -> Unit
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onResult(Result.failure(Exception("Not signed in")))
            return
        }

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role")

                if (role != null) {
                    onResult(Result.success(role))
                } else {
                    onResult(
                        Result.failure(
                            Exception("No role found for this account")
                        )
                    )
                }
            }
            .addOnFailureListener {
                onResult(Result.failure(it))
            }
    }

    /**
     * Finds a registered pet owner using their email address.
     *
     * Returns the owner's Firebase UID, not their password.
     *
     * IMPORTANT: Firestore security rules must restrict this
     * lookup to authorized veterinarians.
     */
    fun findPetOwnerByEmail(
        email: String,
        onResult: (Result<String>) -> Unit
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onResult(Result.failure(Exception("Not signed in")))
            return
        }

        val normalizedEmail = email.trim()
            .lowercase(Locale.ROOT)

        if (normalizedEmail.isBlank()) {
            onResult(Result.failure(Exception("Enter an email address")))
            return
        }

        // Check the current user's role before searching.
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { userDoc ->

                if (userDoc.getString("role") != "veterinarian") {
                    onResult(
                        Result.failure(Exception("Veterinarian access required"))
                    )
                    return@addOnSuccessListener
                }

                // Find the registered pet-owner profile.
                db.collection("users")
                    .whereEqualTo("email", normalizedEmail)
                    .whereEqualTo("role", "pet_owner")
                    .limit(1)
                    .get()
                    .addOnSuccessListener { snapshot ->

                        val owner = snapshot.documents.firstOrNull()

                        if (owner == null) {
                            onResult(
                                Result.failure(
                                    Exception("No registered pet owner found")
                                )
                            )
                        } else {
                            // The document ID is the owner's Firebase UID.
                            onResult(Result.success(owner.id))
                        }
                    }
                    .addOnFailureListener {
                        onResult(Result.failure(it))
                    }
            }
            .addOnFailureListener {
                onResult(Result.failure(it))
            }
    }

    /**
     * Ends the current Firebase authentication session.
     */
    fun signOut() = auth.signOut()
}
