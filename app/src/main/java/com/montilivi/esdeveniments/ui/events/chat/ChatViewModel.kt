package com.montilivi.esdeveniments.ui.events.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.montilivi.esdeveniments.data.model.ChatMessage
import com.montilivi.esdeveniments.data.repository.ChatRepository
import com.montilivi.esdeveniments.utils.FirebaseReferences
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatViewModel(
    private val eventId: String,
    private val chatRepository: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _messages = MutableLiveData<List<ChatMessage>>()
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _lastReadMessageId = MutableLiveData<String?>()
    val lastReadMessageId: LiveData<String?> = _lastReadMessageId

    private val _canSendMessages = MutableLiveData<Boolean>()
    val canSendMessages: LiveData<Boolean> = _canSendMessages

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    var hasJustSentMessage = false

    init {
        checkUserCanSend()
        loadMessages()
    }

    private fun checkUserCanSend() {
        viewModelScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val docId = "${uid}_$eventId"
            val doc = FirebaseReferences.db
                .collection("user_event_subscriptions")
                .document(docId)
                .get()
                .await()

            _canSendMessages.postValue(doc.exists())
        }
    }

    private fun loadMessages() {
        _isLoading.value = true
        chatRepository.listenToMessages(eventId, { messages ->
            _isLoading.postValue(false)
            _messages.postValue(messages)
        }, { _ ->
            _isLoading.postValue(false)
        })
    }

    fun sendMessage(messageText: String) {
        viewModelScope.launch {
            try {
                chatRepository.sendMessage(eventId, messageText)
            } catch (_: Exception) {
            }
        }
    }

    fun markMessageAsRead(messageId: String) {
        viewModelScope.launch {
            chatRepository.markLastReadMessage(eventId, messageId)
        }
    }

    fun fetchLastReadMessageId(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val id = chatRepository.getLastReadMessageId(eventId)
            _lastReadMessageId.postValue(id)
            onResult(id)
        }
    }
}
