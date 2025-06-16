package com.montilivi.esdeveniments.ui.events.create

import android.util.Log
import androidx.lifecycle.*
import com.google.firebase.Timestamp
import com.montilivi.esdeveniments.data.model.Event
import com.montilivi.esdeveniments.data.repository.AuthRepository
import com.montilivi.esdeveniments.data.repository.CategoryRepository
import com.montilivi.esdeveniments.data.repository.EventRepository
import com.montilivi.esdeveniments.utils.FirebaseReferences
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CreateEventViewModel(
    private val eventRepository: EventRepository,
    private val authRepository: AuthRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _eventCreationResult = MutableLiveData<EventCreationResult?>()
    val eventCreationResult: LiveData<EventCreationResult?> = _eventCreationResult

    private val _eventData = MutableLiveData<Event>()
    val eventData: LiveData<Event> = _eventData

    val selectedTimestamps = mutableListOf<Long>()
    val selectedCategory = MutableLiveData<String>()
    val startDate = MutableLiveData<Long>()
    val endDate = MutableLiveData<Long>()

    private val _categoriesMap = MutableLiveData<Map<String, String>>()
    val categoriesMap: LiveData<Map<String, String>> = _categoriesMap

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _eventCreated = MutableLiveData<Boolean>()
    val eventCreated: LiveData<Boolean> = _eventCreated

    fun loadCategories() {
        viewModelScope.launch {
            val map = categoryRepository.getAvailableCategoryMap()
            _categoriesMap.value = map
        }
    }

    fun createEvent(event: Event) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val eventId = eventRepository.createEvent(event.toFirestoreMap())
                _eventCreationResult.value = EventCreationResult.Success(eventId)

                val userId = authRepository.getCurrentUserId() ?: return@launch
                val docId = "${userId}_$eventId"

                val subscription = mapOf(
                    "userId" to userId,
                    "eventId" to eventId,
                    "timeStamp" to Timestamp.now()
                )
                FirebaseReferences.userEventSubscriptionsCollection.document(docId).set(subscription).await()

                val placeholderMessage = mapOf(
                    "senderId" to userId,
                    "senderName" to "Sistema",
                    "message" to "Chat creado",
                    "timestamp" to System.currentTimeMillis()
                )
                FirebaseReferences.db.collection("event_chats")
                    .document(eventId).collection("messages")
                    .document("system").set(placeholderMessage).await()
            } catch (e: Exception) {
                _eventCreationResult.value = EventCreationResult.Error(e.message ?: "Error al crear evento")
            } finally {
                _eventCreationResult.value = null
                _isLoading.value = false
            }
        }
    }

    fun loadEventForEdit(eventId: String) {
        viewModelScope.launch {
            try {
                val snapshot = FirebaseReferences.eventsCollection.document(eventId).get().await()
                val event = snapshot.toObject(Event::class.java)?.copy(eventId = snapshot.id)
                _eventData.value = event ?: return@launch
            } catch (e: Exception) {
                Log.e("CreateEventVM", "Error cargando evento para edición", e)
            }
        }
    }

    fun updateEvent(eventId: String, data: Map<String, Any>) {
        viewModelScope.launch {
            try {
                FirebaseReferences.eventsCollection.document(eventId).update(data).await()
                _eventCreated.value = true
            } catch (e: Exception) {
                _eventCreated.value = false
            }
        }
    }

    fun setEventCreatedWithId(eventId: String) {
        _eventCreationResult.value = EventCreationResult.Success(eventId)
    }

    sealed class EventCreationResult {
        data class Success(val eventId: String) : EventCreationResult()
        data class Error(val message: String) : EventCreationResult()
    }
}
