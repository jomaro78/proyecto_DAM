package com.montilivi.esdeveniments.ui.events.subscribed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.montilivi.esdeveniments.data.model.Event
import com.montilivi.esdeveniments.data.repository.CategoryRepository
import com.montilivi.esdeveniments.data.repository.EventRepository
import com.montilivi.esdeveniments.data.repository.SubscriptionRepository
import com.montilivi.esdeveniments.utils.FirebaseReferences
import kotlinx.coroutines.launch

class SubscribedEventsViewModel(
    private val subscriptionRepo: SubscriptionRepository = SubscriptionRepository(),
    private val eventRepo: EventRepository = EventRepository()
) : ViewModel() {

    private val categoryRepository = CategoryRepository()

    private val _events = MutableLiveData<List<Event>>()
    val events: LiveData<List<Event>> = _events

    private val _categoryColors = MutableLiveData<Map<String, String>>()
    val categoryColors: LiveData<Map<String, String>> = _categoryColors

    private val now = System.currentTimeMillis()

    // carrega esdevs als que l'usuari sigui subscrit
    fun loadSubscribedEvents() {
        val userId = FirebaseReferences.auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val subscribedIds = subscriptionRepo.getSubscribedEvents(userId)
                val allEvents = eventRepo.getAllEvents()

                val filtered = allEvents.filter { event ->
                    event.eventId in subscribedIds && event.endDate >= now
                }

                _events.value = filtered
                _categoryColors.value = categoryRepository.getCategoryColors()
            } catch (e: Exception) {
                _events.value = emptyList()
            }
        }
    }
}
