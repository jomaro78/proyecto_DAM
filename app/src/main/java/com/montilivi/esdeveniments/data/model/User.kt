package com.montilivi.esdeveniments.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint


data class User(
    val userId: String = "",
    val username: String = "",
    val email: String? = null,
    var profileImageUri: String? = null,
    val selectedCategories: List<String> = emptyList(),
    val maxDistance: Int = 10,
    val location: GeoPoint? = null,
    val authProvider: AuthProvider = AuthProvider.EMAIL,
    val isOrganizer: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {

    fun toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "username" to username,
            "email" to email,
            "profileImageUri" to profileImageUri,
            "selectedCategories" to selectedCategories,
            "maxDistance" to maxDistance,
            "location" to location,
            "authProvider" to authProvider.name,
            "isOrganizer" to isOrganizer,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }
}