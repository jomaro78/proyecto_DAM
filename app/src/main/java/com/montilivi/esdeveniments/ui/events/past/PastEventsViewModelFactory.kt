package com.montilivi.esdeveniments.ui.events.past

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PastEventsViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PastEventsViewModel::class.java)) {
            return PastEventsViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}