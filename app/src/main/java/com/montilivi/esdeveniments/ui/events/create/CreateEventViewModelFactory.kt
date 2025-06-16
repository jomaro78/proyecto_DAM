package com.montilivi.esdeveniments.ui.events.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.montilivi.esdeveniments.data.repository.AuthRepository
import com.montilivi.esdeveniments.data.repository.CategoryRepository
import com.montilivi.esdeveniments.data.repository.EventRepository

class CreateEventViewModelFactory(
    private val eventRepository: EventRepository,
    private val authRepository: AuthRepository,
    private val categoryRepository: CategoryRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CreateEventViewModel(eventRepository, authRepository, categoryRepository) as T
    }
}