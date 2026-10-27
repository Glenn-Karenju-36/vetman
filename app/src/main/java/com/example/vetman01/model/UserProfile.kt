package com.example.vetman01.model

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val role: String = "" // "veterinarian" or "pet_owner"
)