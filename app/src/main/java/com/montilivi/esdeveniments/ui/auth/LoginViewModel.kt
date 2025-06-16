package com.montilivi.esdeveniments.ui.auth

import android.content.Intent
import androidx.lifecycle.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.GoogleAuthProvider
import com.montilivi.esdeveniments.R
import com.montilivi.esdeveniments.data.repository.AuthRepository
import com.montilivi.esdeveniments.utils.FirebaseReferences
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel(private val authRepository: AuthRepository = AuthRepository()) : ViewModel() {

    sealed class NavigationEvent {
        data object NavigateToHome : NavigationEvent()
        data object NavigateToPreferences : NavigationEvent()
        data class ShowError(val messageResId: Int) : NavigationEvent()
    }

    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> get() = _navigationEvent

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.loginWithEmail(email, password)
            if (result.isSuccess) {
                val userId = FirebaseReferences.auth.currentUser?.uid
                val doc = userId?.let {
                    FirebaseReferences.usersCollection.document(it).get().await()
                }
                _navigationEvent.value = if (doc?.exists() == true) {
                    NavigationEvent.NavigateToHome
                } else {
                    NavigationEvent.NavigateToPreferences
                }
            } else {
                _navigationEvent.value = NavigationEvent.ShowError(R.string.login_error_credentials)
            }
            _isLoading.value = false
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _navigationEvent.value = NavigationEvent.ShowError(R.string.invalid_email)
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.sendPasswordReset(email)
            _navigationEvent.value = if (result.isSuccess) {
                NavigationEvent.ShowError(R.string.reset_email_sent)
            } else {
                NavigationEvent.ShowError(R.string.reset_email_failed)
            }
            _isLoading.value = false
        }
    }

    fun checkUserDataAfterGoogleLogin(data: Intent?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(Exception::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = FirebaseReferences.auth.signInWithCredential(credential).await()
                    val isNewUser = authResult.additionalUserInfo?.isNewUser ?: false
                    val userId = authResult.user?.uid ?: return@launch

                    if (isNewUser) {
                        _navigationEvent.value = NavigationEvent.NavigateToPreferences
                    } else {
                        val doc = FirebaseReferences.usersCollection.document(userId).get().await()
                        _navigationEvent.value = if (doc.exists()) {
                            NavigationEvent.NavigateToHome
                        } else {
                            NavigationEvent.NavigateToPreferences
                        }
                    }
                } else {
                    _navigationEvent.value = NavigationEvent.ShowError(R.string.login_error_google_token_missing)
                }
            } catch (e: Exception) {
                _navigationEvent.value = NavigationEvent.ShowError(R.string.login_error_google)
            }
            _isLoading.value = false
        }
    }
}