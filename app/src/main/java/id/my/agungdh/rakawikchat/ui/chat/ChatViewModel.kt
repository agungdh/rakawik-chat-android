package id.my.agungdh.rakawikchat.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.my.agungdh.rakawikchat.RakawikChatApp
import id.my.agungdh.rakawikchat.data.remote.dto.MessageResponse
import id.my.agungdh.rakawikchat.data.remote.dto.SendMessageRequest
import id.my.agungdh.rakawikchat.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<MessageResponse> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isConnected: Boolean = false,
    val currentUsername: String = ""
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as RakawikChatApp).container
    private val chatRepository = container.chatRepository

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var conversationId: String = ""

    fun initialize(conversationId: String) {
        this.conversationId = conversationId
        loadMessages()
        loadCurrentUsername()
        connectWebSocket()
    }

    private fun loadCurrentUsername() {
        viewModelScope.launch {
            val user = container.userRepository.getCurrentUser()
            if (user is Resource.Success) {
                _uiState.update { it.copy(currentUsername = user.data.username) }
            }
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = chatRepository.getMessages(conversationId)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, messages = result.data) }
                    chatRepository.markAsRead(conversationId)
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun connectWebSocket() {
        if (_uiState.value.isConnected) return
        chatRepository.connectStomp(
            onConnected = {
                _uiState.update { it.copy(isConnected = true) }
                chatRepository.subscribeToMessages(conversationId) { message ->
                    _uiState.update { state ->
                    val alreadyExists = message.id != null && state.messages.any { it.id == message.id }
                        if (alreadyExists) state
                        else state.copy(messages = state.messages + message)
                    }
                }
            },
            onError = { error ->
                _uiState.update { it.copy(error = error.message, isConnected = false) }
            }
        )
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        val request = SendMessageRequest(
            conversationId = conversationId,
            content = text
        )
        chatRepository.sendMessage(request)
        _uiState.update { it.copy(inputText = "") }
    }

    fun disconnect() {
        chatRepository.disconnectStomp()
        _uiState.update { it.copy(isConnected = false) }
    }

    fun reconnect() {
        connectWebSocket()
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
