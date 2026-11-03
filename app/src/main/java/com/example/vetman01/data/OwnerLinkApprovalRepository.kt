
package com.example.vetman01.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

/**
 * A pending or completed pet-owner linking request.
 *
 * Pet preview details are stored on the invitation so
 * the owner doesn't need access to an unlinked patient.
 */
data class OwnerLinkRequest(
    val id: String = "",
    val patientId: String = "",
    val vetId: String = "",
    val ownerEmail: String = "",
    val status: String = "pending",
    val petName: String = "",
    val species: String = "",
    val breed: String = ""
)

class OwnerLinkApprovalRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Return the signed-in owner's verified email.
     * Only a verified account can receive invitations.
     */
    private fun ownerEmail(): Result<String> {
        val user = auth.currentUser
            ?: return Result.failure(
                IllegalStateException(
                    "Please sign in to continue."
                )
            )

        if (!user.isEmailVerified) {
            return Result.failure(
                IllegalStateException(
                    "Verify your email before viewing requests."
                )
            )
        }

        val email = user.email
            ?.trim()
            ?.lowercase()
            .orEmpty()

        if (email.isBlank()) {
            return Result.failure(
                IllegalStateException(
                    "This account has no email address."
                )
            )
        }

        return Result.success(email)
    }

    /**
     * Load invitations addressed to this verified email.
     *
     * Only the pet preview saved in petLinkRequests is read.
     * We deliberately do NOT read pets/{patientId} here,
     * because the pet isn't linked to this user yet.
     */
    fun getPendingRequests(
        onResult: (Result<List<OwnerLinkRequest>>) -> Unit
    ) {
        val emailResult = ownerEmail()

        if (emailResult.isFailure) {
            onResult(
                Result.failure(
                    emailResult.exceptionOrNull()
                        ?: IllegalStateException(
                            "Email verification required."
                        )
                )
            )
            return
        }

        val email = emailResult.getOrThrow()

        db.collection("petLinkRequests")
            .whereEqualTo("ownerEmail", email)
            .get(Source.SERVER)
            .addOnSuccessListener { snapshot ->

                val requests = snapshot.documents
                    .map { document ->
                        OwnerLinkRequest(
                            id = document.id,
                            patientId = document.getString(
                                "patientId"
                            ).orEmpty(),
                            vetId = document.getString(
                                "vetId"
                            ).orEmpty(),
                            ownerEmail = document.getString(
                                "ownerEmail"
                            ).orEmpty(),
                            status = document.getString(
                                "status"
                            ).orEmpty(),
                            petName = document.getString(
                                "petName"
                            ).orEmpty(),
                            species = document.getString(
                                "species"
                            ).orEmpty(),
                            breed = document.getString(
                                "breed"
                            ).orEmpty()
                        )
                    }
                    .filter { request ->
                        request.status == "pending" &&
                                request.patientId == request.id
                    }

                onResult(Result.success(requests))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    /**
     * Accept an invitation.
     *
     * Firestore security rules require BOTH changes
     * to happen in the same atomic batch:
     *
     * 1. Mark the request accepted.
     * 2. Set the pet's ownerId to this owner's UID.
     */
    fun acceptRequest(
        request: OwnerLinkRequest,
        onResult: (Result<Unit>) -> Unit
    ) {
        val user = auth.currentUser

        if (user == null) {
            onResult(
                Result.failure(
                    IllegalStateException(
                        "Please sign in to continue."
                    )
                )
            )
            return
        }

        val emailResult = ownerEmail()

        if (emailResult.isFailure) {
            onResult(
                Result.failure(
                    emailResult.exceptionOrNull()
                        ?: IllegalStateException(
                            "Email verification required."
                        )
                )
            )
            return
        }

        if (
            request.id.isBlank() ||
            request.patientId != request.id ||
            request.ownerEmail.lowercase() !=
            emailResult.getOrThrow()
        ) {
            onResult(
                Result.failure(
                    IllegalArgumentException(
                        "This invitation does not match your account."
                    )
                )
            )
            return
        }

        val requestRef = db.collection("petLinkRequests")
            .document(request.patientId)

        val petRef = db.collection("pets")
            .document(request.patientId)

        val batch = db.batch()

        batch.update(
            requestRef,
            mapOf(
                "status" to "accepted",
                "ownerUid" to user.uid,
                "acceptedAt" to FieldValue.serverTimestamp()
            )
        )

        batch.update(
            petRef,
            "ownerId",
            user.uid
        )

        batch.commit()
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    /**
     * Declining an invitation does not change ownerId.
     */
    fun declineRequest(
        request: OwnerLinkRequest,
        onResult: (Result<Unit>) -> Unit
    ) {
        val emailResult = ownerEmail()

        if (emailResult.isFailure) {
            onResult(
                Result.failure(
                    emailResult.exceptionOrNull()
                        ?: IllegalStateException(
                            "Email verification required."
                        )
                )
            )
            return
        }

        if (
            request.id.isBlank() ||
            request.patientId != request.id ||
            request.ownerEmail.lowercase() !=
            emailResult.getOrThrow()
        ) {
            onResult(
                Result.failure(
                    IllegalArgumentException(
                        "This invitation does not match your account."
                    )
                )
            )
            return
        }

        db.collection("petLinkRequests")
            .document(request.patientId)
            .update(
                mapOf(
                    "status" to "declined",
                    "declinedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    /**
     * Send a Firebase Authentication verification email.
     */
    fun sendVerificationEmail(
        onResult: (Result<Unit>) -> Unit
    ) {
        val user = auth.currentUser

        if (user == null) {
            onResult(
                Result.failure(
                    IllegalStateException(
                        "Please sign in first."
                    )
                )
            )
            return
        }

        if (user.isEmailVerified) {
            onResult(Result.success(Unit))
            return
        }

        user.sendEmailVerification()
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    /**
     * Refresh both the Firebase user and ID token.
     * The security rules depend on email_verified
     * being present in the authentication token.
     */
    fun refreshVerification(
        onResult: (Result<Boolean>) -> Unit
    ) {
        val user = auth.currentUser

        if (user == null) {
            onResult(
                Result.failure(
                    IllegalStateException(
                        "Please sign in first."
                    )
                )
            )
            return
        }

        user.reload()
            .addOnSuccessListener {

                val refreshedUser = auth.currentUser

                if (refreshedUser == null) {
                    onResult(
                        Result.failure(
                            IllegalStateException(
                                "Authentication session expired."
                            )
                        )
                    )
                    return@addOnSuccessListener
                }

                refreshedUser.getIdToken(true)
                    .addOnSuccessListener {
                        onResult(
                            Result.success(
                                refreshedUser.isEmailVerified
                            )
                        )
                    }
                    .addOnFailureListener { error ->
                        onResult(Result.failure(error))
                    }
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }
}
