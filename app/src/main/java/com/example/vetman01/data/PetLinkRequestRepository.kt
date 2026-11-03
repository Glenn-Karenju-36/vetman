
package com.example.vetman01.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

/**
 * Creates pet-owner linking invitations.
 *
 * One request is stored at:
 * petLinkRequests/{patientId}
 *
 * The owner must explicitly accept before the pet is linked.
 */
class PetLinkRequestRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        const val MAX_PETS_PER_REQUEST = 5
    }

    /**
     * Sends one invitation per selected pet.
     *
     * The callback returns how many invitations were saved.
     * A successful send does NOT mean the owner has accepted.
     */
    fun createLinkRequest(
        patientIds: List<String>,
        ownerEmail: String,
        onResult: (Result<Int>) -> Unit
    ) {
        val vetId = auth.currentUser?.uid

        if (vetId == null) {
            onResult(
                Result.failure(
                    IllegalStateException(
                        "Please sign in as a veterinarian."
                    )
                )
            )
            return
        }

        val email = ownerEmail.trim().lowercase()
        val selectedIds = patientIds
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        if (selectedIds.isEmpty()) {
            onResult(
                Result.failure(
                    IllegalArgumentException(
                        "Select at least one pet."
                    )
                )
            )
            return
        }

        if (selectedIds.size > MAX_PETS_PER_REQUEST) {
            onResult(
                Result.failure(
                    IllegalArgumentException(
                        "Select up to $MAX_PETS_PER_REQUEST pets."
                    )
                )
            )
            return
        }

        if (
            !android.util.Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()
        ) {
            onResult(
                Result.failure(
                    IllegalArgumentException(
                        "Enter a valid owner email address."
                    )
                )
            )
            return
        }

        // Read each selected pet from the server.
        // Preview fields must come from the actual patient,
        // never from manually entered form text.
        loadSelectedPets(
            patientIds = selectedIds,
            vetId = vetId,
            index = 0,
            loadedPets = mutableListOf(),
            onComplete = { result ->
                result.fold(
                    onSuccess = { pets ->
                        createPendingBatch(
                            pets = pets,
                            vetId = vetId,
                            email = email,
                            onResult = onResult
                        )
                    },
                    onFailure = { error ->
                        onResult(Result.failure(error))
                    }
                )
            }
        )
    }

    private fun loadSelectedPets(
        patientIds: List<String>,
        vetId: String,
        index: Int,
        loadedPets: MutableList<DocumentSnapshot>,
        onComplete: (Result<List<DocumentSnapshot>>) -> Unit
    ) {
        if (index >= patientIds.size) {
            onComplete(Result.success(loadedPets.toList()))
            return
        }

        val patientId = patientIds[index]

        db.collection("pets")
            .document(patientId)
            .get(Source.SERVER)
            .addOnSuccessListener { snapshot ->

                if (!snapshot.exists()) {
                    onComplete(
                        Result.failure(
                            IllegalStateException(
                                "A selected pet no longer exists."
                            )
                        )
                    )
                    return@addOnSuccessListener
                }

                if (snapshot.getString("vetId") != vetId) {
                    onComplete(
                        Result.failure(
                            SecurityException(
                                "You can only link your own patients."
                            )
                        )
                    )
                    return@addOnSuccessListener
                }

                val ownerId =
                    snapshot.getString("ownerId").orEmpty()

                if (ownerId.isNotBlank()) {
                    onComplete(
                        Result.failure(
                            IllegalStateException(
                                "${snapshot.getString("petName") ?: "This pet"} " +
                                        "is already linked to an owner."
                            )
                        )
                    )
                    return@addOnSuccessListener
                }

                val petName = snapshot.getString("petName")
                val species = snapshot.getString("species")
                val breed = snapshot.getString("breed")

                if (
                    petName == null ||
                    species == null ||
                    breed == null
                ) {
                    onComplete(
                        Result.failure(
                            IllegalStateException(
                                "This pet is missing name, species, " +
                                        "or breed information."
                            )
                        )
                    )
                    return@addOnSuccessListener
                }

                loadedPets.add(snapshot)

                loadSelectedPets(
                    patientIds = patientIds,
                    vetId = vetId,
                    index = index + 1,
                    loadedPets = loadedPets,
                    onComplete = onComplete
                )
            }
            .addOnFailureListener { error ->
                onComplete(Result.failure(error))
            }
    }

    private fun createPendingBatch(
        pets: List<DocumentSnapshot>,
        vetId: String,
        email: String,
        onResult: (Result<Int>) -> Unit
    ) {
        val batch = db.batch()

        for (pet in pets) {
            val patientId = pet.id

            val requestRef = db.collection("petLinkRequests")
                .document(patientId)

            // Exactly the nine fields required by our
            // tested Firestore security rules.
            val requestData = hashMapOf<String, Any>(
                "id" to patientId,
                "patientId" to patientId,
                "vetId" to vetId,
                "ownerEmail" to email,
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp(),
                "petName" to pet.getString("petName").orEmpty(),
                "species" to pet.getString("species").orEmpty(),
                "breed" to pet.getString("breed").orEmpty()
            )

            // An existing request cannot be overwritten
            // by the veterinarian under the current rules.
            // This safely prevents duplicate invitations.
            batch.set(requestRef, requestData)
        }

        // All selected invitations succeed or fail together.
        batch.commit()
            .addOnSuccessListener {
                onResult(Result.success(pets.size))
            }
            .addOnFailureListener { error ->
                onResult(
                    Result.failure(
                        IllegalStateException(
                            "Could not send the linking request. " +
                                    "Check that the pets are unlinked " +
                                    "and do not have existing requests. " +
                                    error.message,
                            error
                        )
                    )
                )
            }
    }
}
