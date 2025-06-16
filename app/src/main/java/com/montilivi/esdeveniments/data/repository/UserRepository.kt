package com.montilivi.esdeveniments.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Source
import com.montilivi.esdeveniments.data.model.AuthProvider
import com.montilivi.esdeveniments.data.model.User
import com.montilivi.esdeveniments.utils.FirebaseReferences
import com.montilivi.esdeveniments.utils.ImageUtils
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val usersRef = FirebaseReferences.usersCollection

    suspend fun getUser(userId: String, source: Source = Source.DEFAULT): User? {
        return try {
            val snapshot = usersRef.document(userId).get(source).await()
            val raw = snapshot.data
            if (raw == null) return null

            User(
                userId = raw["userId"] as? String ?: "",
                username = raw["username"] as? String ?: "",
                email = raw["email"] as? String ?: "",
                profileImageUri = raw["profileImageUri"] as? String,
                selectedCategories = raw["selectedCategories"] as? List<String> ?: emptyList(),
                maxDistance = (raw["maxDistance"] as? Long)?.toInt() ?: 50,
                location = raw["location"] as? GeoPoint,
                authProvider = AuthProvider.EMAIL,
                isOrganizer = raw["isOrganizer"] as? Boolean ?: false,
                updatedAt = raw["updatedAt"] as? Timestamp
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun uploadUserProfileImage(context: Context, userId: String, imageUri: Uri, oldImageUrl: String?): Result<String> {
        return try {
            oldImageUrl?.takeIf { it.isNotBlank() }?.let {
                try {
                    FirebaseReferences.storage.getReferenceFromUrl(it).delete().await()
                } catch (_: Exception) {}
            }

            val compressedData = ImageUtils.compressImageFromUri(context, imageUri)
            val storageRef = FirebaseReferences.profileImagesRef.child("$userId.jpg")
            storageRef.putBytes(compressedData).await()
            val downloadUri = storageRef.downloadUrl.await()
            Result.success(downloadUri.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
