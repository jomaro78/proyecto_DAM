package com.montilivi.esdeveniments.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.montilivi.esdeveniments.data.model.CategoryRequest
import com.montilivi.esdeveniments.utils.FirebaseReferences
import com.montilivi.esdeveniments.utils.FirebaseReferences.db
import com.montilivi.esdeveniments.utils.StringUtils
import kotlinx.coroutines.tasks.await
import java.util.Locale

class CategoryRepository {

    // manega les traduccions
    suspend fun getAvailableCategoryMap(): Map<String, String> {
        val snapshot = db.collection("event_categories").get().await()
        val currentLang = Locale.getDefault().language
        return snapshot.documents.mapNotNull { doc ->
            val translations = doc.get("translations") as? Map<String, String>
            val name = translations?.get(currentLang) ?: translations?.values?.firstOrNull()
            name?.let { it to doc.id }
        }.toMap()
    }

    suspend fun submitCategorySuggestion(name: String, userId: String, userEmail: String) {
        val suggestionId = StringUtils.normalizeCategoryId(name)
        val docRef = db.collection("category_requests").document(suggestionId)
            .collection("votes").document(userId)

        val data = mapOf(
            "name" to name,
            "userId" to userId,
            "userEmail" to userEmail,
            "timestamp" to FieldValue.serverTimestamp()
        )

        docRef.set(data, SetOptions.merge()).await()
    }

    //obté les suggerencies i les ordena per quantitat
    suspend fun getAllCategorySuggestions(): List<CategoryRequest> {
        val snapshot = db.collectionGroup("votes").get().await()

        val grouped = snapshot.documents
            .mapNotNull { it.getString("name") }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }

        return grouped.map { (name, count) ->
            CategoryRequest(name = "$name ($count)")
        }
    }

    // obté el color de la categoría
    suspend fun getCategoryColors(): Map<String, String> {
        return try {
            val snapshot = FirebaseReferences.categoriesCollection.get().await()
            snapshot.documents.associate { doc ->
                val colorHex = doc.getString("colorHex") ?: "#FFFFFF"
                doc.id to colorHex
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
