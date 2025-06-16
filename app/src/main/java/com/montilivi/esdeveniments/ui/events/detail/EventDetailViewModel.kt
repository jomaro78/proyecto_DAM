package com.montilivi.esdeveniments.ui.events.detail

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.montilivi.esdeveniments.utils.FirebaseReferences
import com.montilivi.esdeveniments.data.model.Event
import com.montilivi.esdeveniments.data.repository.SubscriptionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EventDetailViewModel() : ViewModel() {

    private val _categoryColor = MutableLiveData<String>()
    val categoryColor: LiveData<String> = _categoryColor
    private val subscriptionRepo = SubscriptionRepository()
    private val _event = MutableLiveData<Event>()
    val event: LiveData<Event> = _event

    private val _isSubscribed = MutableLiveData<Boolean>()
    val isSubscribed: LiveData<Boolean> = _isSubscribed

    private val _subscriberCount = MutableLiveData<Int>()
    val subscriberCount: LiveData<Int> = _subscriberCount

    private lateinit var currentEventId: String

    constructor(eventId: String) : this() {
        this.currentEventId = eventId
        loadEvent()
        checkIfUserIsSubscribed()
        loadSubscriberCount()
    }

    private fun loadEvent() {
        viewModelScope.launch {
            try {
                val eventDeferred = async { FirebaseReferences.eventsCollection.document(currentEventId).get().await() }
                val eventSnap = eventDeferred.await()
                val event = eventSnap.toObject(Event::class.java)?.copy(eventId = eventSnap.id)
                _event.postValue(event ?: return@launch)

                val categoryId = event.category
                val catSnap = FirebaseReferences.categoriesCollection.document(categoryId).get().await()
                val colorHex = catSnap.getString("colorHex") ?: "#FFFFFF"
                _categoryColor.postValue(colorHex)
            } catch (e: Exception) {
                Log.e("EventDetailViewModel", "Error loading event or category color", e)
            }
        }
    }

    fun loadSubscriberCount(onResult: (Int?) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val count = subscriptionRepo.getSubscriberCount(currentEventId)
                _subscriberCount.value = count
                onResult(count)
            } catch (e: Exception) {
                Log.e("EventDetailViewModel", "Error fetching subscriber count", e)
                onResult(null)
            }
        }
    }

    private fun checkIfUserIsSubscribed() {
        val uid = FirebaseReferences.auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val isSub = subscriptionRepo.isUserSubscribed(uid, currentEventId)
            _isSubscribed.postValue(isSub)
        }
    }

    fun subscribe() {
        val uid = FirebaseReferences.auth.currentUser?.uid ?: return
        viewModelScope.launch {
            subscriptionRepo.subscribeToEvent(uid, currentEventId)
            _isSubscribed.postValue(true)
            loadSubscriberCount()
        }
    }

    fun unsubscribe() {
        val uid = FirebaseReferences.auth.currentUser?.uid ?: return
        viewModelScope.launch {
            subscriptionRepo.unsubscribeFromEvent(uid, currentEventId)
            _isSubscribed.postValue(false)
            loadSubscriberCount()
        }
    }
}