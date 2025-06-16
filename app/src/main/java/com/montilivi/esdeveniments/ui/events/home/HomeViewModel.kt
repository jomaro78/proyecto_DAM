package com.montilivi.esdeveniments.ui.events.home

import androidx.lifecycle.*
import com.montilivi.esdeveniments.utils.FirebaseReferences
import com.montilivi.esdeveniments.data.repository.EventRepository
import com.montilivi.esdeveniments.data.model.Event
import com.montilivi.esdeveniments.data.repository.CategoryRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeViewModel : ViewModel() {

    private val eventRepository = EventRepository()
    private val categoryRepository = CategoryRepository()

    private val _events = MutableLiveData<List<Event>>()
    val events: LiveData<List<Event>> = _events

    private val _categoryColors = MutableLiveData<Map<String, String>>()
    val categoryColors: LiveData<Map<String, String>> = _categoryColors

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // carrega events disponibles segons les preferencies de l'usuari
    fun loadEvents() {
        val userId = FirebaseReferences.auth.currentUser?.uid ?: return
        val now = System.currentTimeMillis()

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userDoc = FirebaseReferences.db.collection("users").document(userId).get().await()
                val categories = userDoc.get("selectedCategories") as? List<String> ?: emptyList()
                val distance = (userDoc.get("maxDistance") as? Long)?.toInt() ?: 50
                val location = userDoc.getGeoPoint("location") ?: return@launch

                val eventos = eventRepository.getNearbyEvents(location, categories, distance)

                val subscriptionsSnapshot = FirebaseReferences.db
                    .collection("user_event_subscriptions")
                    .whereEqualTo("userId", userId)
                    .get().await()

                val subscribedEventIds = subscriptionsSnapshot.documents.mapNotNull { it.getString("eventId") }

                _events.value = eventos.filter {
                    it.endDate > now && it.eventId !in subscribedEventIds
                }

                _categoryColors.value = categoryRepository.getCategoryColors()

            } catch (e: Exception) {
                _events.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}