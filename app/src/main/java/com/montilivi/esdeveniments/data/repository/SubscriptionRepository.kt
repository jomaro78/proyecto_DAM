package com.montilivi.esdeveniments.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.montilivi.esdeveniments.utils.FirebaseReferences
import kotlinx.coroutines.tasks.await

class SubscriptionRepository {
    private val collection = FirebaseReferences.userEventSubscriptionsCollection

    suspend fun subscribeToEvent(userId: String, eventId: String): Boolean {
        return try {
            val docId = "${userId}_$eventId"
            val subscription = hashMapOf(
                "userId" to userId,
                "eventId" to eventId,
                "timeStamp" to Timestamp.now()
            )
            collection.document(docId).set(subscription).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun unsubscribeFromEvent(userId: String, eventId: String) {
        val snapshot = collection
            .whereEqualTo("userId", userId)
            .whereEqualTo("eventId", eventId)
            .get()
            .await()
        for (doc in snapshot.documents) {
            doc.reference.delete()
        }
    }

    suspend fun isUserSubscribed(userId: String, eventId: String): Boolean {
        val snapshot = collection
            .whereEqualTo("userId", userId)
            .whereEqualTo("eventId", eventId)
            .get()
            .await()
        return !snapshot.isEmpty
    }

    suspend fun getSubscribedEvents(userId: String): List<String> {
        return try {
            val result = collection.whereEqualTo("userId", userId).get().await()
            result.documents.mapNotNull { it.getString("eventId") }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSubscriberCount(eventId: String): Int {
        return try {
            val snapshot = collection
                .whereEqualTo("eventId", eventId)
                .get()
                .await()
            Log.d("PruebaSubs", "Found ${snapshot.size()} subscribers for $eventId")
            snapshot.size()
        } catch (e: Exception) {
            Log.e("PruebaSubs", "Error getting subscriber count: ${e.message}", e)
            0
        }
    }

    suspend fun getOnlineUserCount(eventId: String): Int {
        val threshold = System.currentTimeMillis() - 2 * 60 * 1000
        val snapshot = FirebaseReferences.chatActivityCollection
            .whereEqualTo("eventId", eventId)
            .whereGreaterThan("lastActive", threshold)
            .get()
            .await()
        return snapshot.size()
    }
}