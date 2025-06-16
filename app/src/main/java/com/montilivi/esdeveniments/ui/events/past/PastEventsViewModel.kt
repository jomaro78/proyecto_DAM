package com.montilivi.esdeveniments.ui.events.past

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.montilivi.esdeveniments.data.model.Event
import com.montilivi.esdeveniments.data.repository.CategoryRepository
import com.montilivi.esdeveniments.data.repository.EventRepository
import com.montilivi.esdeveniments.utils.FirebaseReferences
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PastEventsViewModel : ViewModel() {

    private val repository = EventRepository()
    private val categoryRepository = CategoryRepository()

    private val _pastEvents = MutableLiveData<List<Event>>()
    val pastEvents: LiveData<List<Event>> = _pastEvents

    private val _categoryColors = MutableLiveData<Map<String, String>>()
    val categoryColors: LiveData<Map<String, String>> = _categoryColors

    init {
        loadPastEvents()
    }

    //carrega esdevs finalitzats als que l'usuari sigués subscrit
    private fun loadPastEvents() {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val allEvents = repository.getAllEvents()

                val userId = FirebaseReferences.auth.currentUser?.uid
                if (userId == null) {
                    _pastEvents.value = emptyList()
                    return@launch
                }

                val subscriptions = FirebaseReferences.db
                    .collection("user_event_subscriptions")
                    .whereEqualTo("userId", userId)
                    .get().await()

                val subscribedEventIds = subscriptions.documents.mapNotNull { it.getString("eventId") }

                val filtered = allEvents.filter { event ->
                    event.endDate < now && event.eventId in subscribedEventIds
                }

                _pastEvents.value = filtered
                _categoryColors.value = categoryRepository.getCategoryColors()

            } catch (e: Exception) {
                _pastEvents.value = emptyList()
            }
        }
    }
}
