package com.montilivi.esdeveniments.ui.categories

import android.util.Log
import androidx.lifecycle.*
import com.montilivi.esdeveniments.R
import com.montilivi.esdeveniments.data.repository.CategoryRepository
import com.montilivi.esdeveniments.utils.StringUtils
import kotlinx.coroutines.launch

class SuggestCategoryViewModel(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _existingSuggestions = MutableLiveData<List<String>>()
    val existingSuggestions: LiveData<List<String>> = _existingSuggestions

    private val _submissionSuccess = MutableLiveData<Boolean>()
    val submissionSuccess: LiveData<Boolean> = _submissionSuccess

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadSuggestions() {
        viewModelScope.launch {
            Log.d("SuggestCategoryVM", "Cargando sugerencias")
            try {
                val suggestions = categoryRepository.getAllCategorySuggestions()
                val names = suggestions.map { it.name }
                _existingSuggestions.value = names
                Log.d("SuggestCategoryVM", "Sugerencias cargadas: $names")
            } catch (e: Exception) {
                _error.value = R.string.suggest_category_generic_error.toString()
            }
        }
    }

    fun submitSuggestion(name: String, userId: String, userEmail: String) {
        val normalized = StringUtils.normalizeCategoryId(name)

        // valida que el mateix usuari no demani dos cops lo mateix
        if (_existingSuggestions.value?.contains(normalized) == true) {
            _error.value = R.string.suggest_category_already_exists.toString()
            return
        }

        viewModelScope.launch {
            try {
                categoryRepository.submitCategorySuggestion(name, userId, userEmail)
                _submissionSuccess.value = true
            } catch (e: Exception) {
                _error.value = R.string.suggest_category_submit_error.toString()
            }
        }
    }

    fun resetStatus() {
        _submissionSuccess.value = false
        _error.value = null
    }
}
