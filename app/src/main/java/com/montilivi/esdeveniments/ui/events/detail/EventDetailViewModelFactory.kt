package com.montilivi.esdeveniments.ui.events.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class EventDetailViewModelFactory(private val eventId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EventDetailViewModel::class.java)) {
            return EventDetailViewModel(eventId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}