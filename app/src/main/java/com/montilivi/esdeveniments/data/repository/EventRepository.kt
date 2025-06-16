package com.montilivi.esdeveniments.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.GeoPoint
import com.montilivi.esdeveniments.utils.FirebaseReferences
import com.montilivi.esdeveniments.data.model.Event
import com.montilivi.esdeveniments.utils.ImageUtils
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.math.*

class EventRepository {

    private val db = FirebaseReferences.db

    suspend fun getNearbyEvents(userLocation: GeoPoint, categories: List<String>, maxDistance: Int): List<Event> {
        return try {
            val snapshot = db.collection("events").get().await()
            snapshot.documents.mapNotNull { doc ->
                val category = doc.getString("category") ?: return@mapNotNull null
                val location = doc.getGeoPoint("location") ?: return@mapNotNull null

                if (categories.isNotEmpty() && category !in categories) return@mapNotNull null

                val distance = haversineDistance(
                    userLocation.latitude, userLocation.longitude,
                    location.latitude, location.longitude
                )

                if (distance <= maxDistance) {
                    doc.toObject(Event::class.java)?.copy(eventId = doc.id)
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllEvents(): List<Event> {
        return try {
            val snapshot = db.collection("events").get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Event::class.java)?.copy(eventId = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getEventCategories(): List<String> {
        return try {
            val snapshot = db.collection("eventCategories").get().await()
            snapshot.documents.mapNotNull { it.getString("name") }

        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createEvent(eventData: Map<String, Any?>): String {
        return try {
            val docRef = FirebaseReferences.db.collection("events").add(eventData).await()
            // 🟢 Actualizamos el campo eventId con el ID real
            FirebaseReferences.db.collection("events").document(docRef.id)
                .update("eventId", docRef.id)
            docRef.id
        } catch (e: Exception) {
            throw e
        }
    }

    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    suspend fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            FirebaseReferences.eventsCollection.document(eventId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadEventImage(
        context: Context,
        organizerId: String,
        eventId: String,
        imageUri: Uri,
        oldImageUrl: String? = null
    ): Result<String> {
        return try {
            oldImageUrl?.takeIf { it.isNotBlank() }?.let {
                try {
                    FirebaseReferences.storage.getReferenceFromUrl(it).delete().await()
                } catch (_: Exception) {}
            }

            val compressedData = ImageUtils.compressImageFromUri(context, imageUri)
            val storageRef = FirebaseReferences.eventImagesRef
                .child(organizerId)
                .child("$eventId.jpg")

            storageRef.putBytes(compressedData).await()
            val downloadUri = storageRef.downloadUrl.await()
            Result.success(downloadUri.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}