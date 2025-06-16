package com.montilivi.esdeveniments.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint


data class Event(
    val eventId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val location: GeoPoint? = GeoPoint(0.0, 0.0),
    val locationName: String = "",
    val imageUrl: String = "",
    val creatorId: String = "",
    val subscribers: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "eventId" to eventId,
            "title" to title,
            "description" to description,
            "category" to category,
            "imageUrl" to imageUrl,
            "location" to location,
            "locationName" to locationName,
            "startDate" to startDate,
            "endDate" to endDate,
            "creatorId" to creatorId,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }
}