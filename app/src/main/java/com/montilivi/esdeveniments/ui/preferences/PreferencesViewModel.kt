package com.montilivi.esdeveniments.ui.preferences

import android.content.Context
import android.location.Location
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.GeoPoint
import com.montilivi.esdeveniments.data.model.User
import com.montilivi.esdeveniments.data.repository.CategoryRepository
import com.montilivi.esdeveniments.data.repository.UserRepository
import com.montilivi.esdeveniments.utils.FirebaseReferences
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PreferencesViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val categoryRepository: CategoryRepository = CategoryRepository()
) : ViewModel() {

    val userId = FirebaseReferences.auth.currentUser?.uid ?: ""
    val userData = MutableLiveData<User?>()
    private val maxDistance = MutableLiveData<Int>()
    private val isLoading = MutableLiveData<Boolean>()

    private val _location = MutableLiveData<GeoPoint?>()
    val location: LiveData<GeoPoint?> = _location

    private val _photoUri = MutableLiveData<String?>()

    private val _categoriesMap = MutableLiveData<Map<String, String>>()
    val categoriesMap: LiveData<Map<String, String>> = _categoriesMap

    sealed class SaveResult {
        data object Success : SaveResult()
        data class Error(val message: String) : SaveResult()
    }

    val saveResult = MutableLiveData<SaveResult>()

    // obtenir dades
    fun loadUserPreferences(userId: String) {
        viewModelScope.launch {
            isLoading.value = true
            userData.value = userRepository.getUser(userId)
            userData.value?.let {
                maxDistance.value = it.maxDistance
                _location.value = it.location
                _photoUri.value = it.profileImageUri
            }
            isLoading.value = false
        }
    }

    // obtenir categoríes
    fun loadCategories() {
        viewModelScope.launch {
            val map = categoryRepository.getAvailableCategoryMap()
            _categoriesMap.value = map
        }
    }

    //obtenir ubicació
    fun fetchCurrentLocation(context: Context) {
        viewModelScope.launch {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                val location: Location? = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    _location.value = GeoPoint(location.latitude, location.longitude)
                } else {
                    Log.e("PreferencesVM", "No se pudo obtener ubicación actual (null)")
                }
            } catch (e: SecurityException) {
                Log.e("PreferencesVM", "Permiso de ubicación no concedido", e)
            } catch (e: Exception) {
                Log.e("PreferencesVM", "Error al obtener la ubicación", e)
            }
        }
    }

    fun uploadImageAndSave(user: User, context: Context, imageUri: Uri) {
        viewModelScope.launch {
            isLoading.value = true
            val oldImage = user.profileImageUri
            val result = userRepository.uploadUserProfileImage(context, user.userId, imageUri, oldImage)
            result.onSuccess { uri ->
                user.profileImageUri = uri
                saveUserPreferences(user.toFirestoreMap())
            }.onFailure {
                saveResult.value = SaveResult.Error("Error al subir imagen: ${it.message}")
                isLoading.value = false
            }
        }
    }

    fun saveUserPreferences(userMap: Map<String, Any?>) {
        viewModelScope.launch {
            try {
                FirebaseReferences.usersCollection.document(userId).set(userMap).await()
                saveResult.value = SaveResult.Success
            } catch (e: Exception) {
                saveResult.value = SaveResult.Error(e.message ?: "")
            } finally {
                isLoading.value = false
            }
        }
    }
}
