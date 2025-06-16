package com.montilivi.esdeveniments.data.repository

import com.google.firebase.firestore.ListenerRegistration
import com.montilivi.esdeveniments.data.model.ChatMessage
import com.montilivi.esdeveniments.data.model.ReadStatus
import com.montilivi.esdeveniments.utils.FirebaseReferences
import kotlinx.coroutines.tasks.await

class ChatRepository {

    fun listenToMessages(
        eventId: String,
        onUpdate: (List<ChatMessage>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return FirebaseReferences.db
            .collection("event_chats")
            .document(eventId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val messages = snapshots?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(messageId = doc.id)
                } ?: emptyList()

                onUpdate(messages)
            }
    }

    suspend fun markLastReadMessage(eventId: String, messageId: String) {
        val userId = FirebaseReferences.auth.currentUser?.uid ?: return
        val readStatus = ReadStatus(
            lastReadMessageId = messageId,
            timestamp = System.currentTimeMillis()
        )

        FirebaseReferences.db
            .collection("events")
            .document(eventId)
            .collection("read_statuses")
            .document(userId)
            .set(readStatus)
            .await()
    }

    suspend fun getLastReadMessageId(eventId: String): String? {
        val userId = FirebaseReferences.auth.currentUser?.uid ?: return null
        val snapshot = FirebaseReferences.db
            .collection("events")
            .document(eventId)
            .collection("read_statuses")
            .document(userId)
            .get()
            .await()

        return snapshot.toObject(ReadStatus::class.java)?.lastReadMessageId
    }

    suspend fun sendMessage(eventId: String, messageText: String) {
        val currentUser = FirebaseReferences.auth.currentUser ?: throw Exception("Usuario no autenticado")
        val uid = currentUser.uid

        val userDoc = FirebaseReferences.usersCollection.document(uid).get().await()
        val username = userDoc.getString("username")
            ?: currentUser.displayName
            ?: "Anónimo"

        val message = ChatMessage(
            senderId = uid,
            senderName = username,
            message = messageText,
            timestamp = System.currentTimeMillis()
        )

        FirebaseReferences.db
            .collection("event_chats")
            .document(eventId)
            .collection("messages")
            .add(message)
            .await()
    }

    suspend fun updateUserActivity(eventId: String) {
        val userId = FirebaseReferences.auth.currentUser?.uid ?: return
        val data = mapOf(
            "userId" to userId,
            "eventId" to eventId,
            "lastActive" to System.currentTimeMillis()
        )
        FirebaseReferences.chatActivityCollection
            .document("$eventId-$userId")
            .set(data)
            .await()
    }
}