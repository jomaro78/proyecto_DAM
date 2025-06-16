package com.montilivi.esdeveniments.ui.events.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ChatViewModelFactory(private val eventId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(eventId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}