package com.montilivi.esdeveniments.ui.events.subscribed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.montilivi.esdeveniments.data.repository.EventRepository
import com.montilivi.esdeveniments.data.repository.SubscriptionRepository

class SubscribedEventsViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SubscribedEventsViewModel::class.java)) {
            return SubscribedEventsViewModel(
                SubscriptionRepository(),
                EventRepository()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}