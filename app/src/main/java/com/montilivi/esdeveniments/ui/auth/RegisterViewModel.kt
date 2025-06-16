package com.montilivi.esdeveniments.ui.auth

import androidx.lifecycle.*
import com.montilivi.esdeveniments.data.repository.AuthRepository
import kotlinx.coroutines.launch

class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {

    sealed class NavigationEvent {
        data object NavigateToPreferences : NavigationEvent()
        data class ShowError(val message: String) : NavigationEvent()
    }

    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> get() = _navigationEvent

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun registerWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.registerWithEmail(email, password)
            if (result.isSuccess) {
                _navigationEvent.value = NavigationEvent.NavigateToPreferences
            } else {
                _navigationEvent.value = NavigationEvent.ShowError(result.exceptionOrNull()?.localizedMessage ?: "Error desconocido")
            }
            _isLoading.value = false
        }
    }
}
