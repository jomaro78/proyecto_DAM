package com.montilivi.esdeveniments.ui.preferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.montilivi.esdeveniments.data.repository.CategoryRepository
import com.montilivi.esdeveniments.data.repository.UserRepository

class PreferencesViewModelFactory(
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PreferencesViewModel(userRepository, categoryRepository) as T
    }
}
