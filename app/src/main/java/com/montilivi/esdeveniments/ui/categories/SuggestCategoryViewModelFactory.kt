package com.montilivi.esdeveniments.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.montilivi.esdeveniments.data.repository.CategoryRepository

class SuggestCategoryViewModelFactory(
    private val categoryRepository: CategoryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SuggestCategoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SuggestCategoryViewModel(categoryRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}