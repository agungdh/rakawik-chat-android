package id.my.agungdh.rakawikchat.ui.conversations

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.my.agungdh.rakawikchat.RakawikChatApp
import id.my.agungdh.rakawikchat.data.remote.dto.UserResponse
import id.my.agungdh.rakawikchat.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConversationsUiState(
    val existingChats: List<ChatItem.ExistingChat> = emptyList(),
    val contacts: List<ChatItem.Contact> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentUsername: String = ""
)

class ConversationsViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as RakawikChatApp).container
    private val chatRepository = container.chatRepository
    private val userRepository = container.userRepository

    private val _uiState = MutableStateFlow(ConversationsUiState())
    val uiState: StateFlow<ConversationsUiState> = _uiState.asStateFlow()

    private var allUsers: List<UserResponse> = emptyList()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val currentUser = userRepository.getCurrentUser()
            if (currentUser !is Resource.Success) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load user") }
                return@launch
            }

            val me = currentUser.data
            allUsers = (userRepository.getAllUsers() as? Resource.Success)?.data ?: emptyList()

            val conversations = (chatRepository.getConversations() as? Resource.Success)?.data
                ?: emptyList()

            val existingChats = conversations.mapNotNull { conv ->
                val other = conv.participants.firstOrNull { it != me.username } ?: return@mapNotNull null
                val otherUser = allUsers.firstOrNull { it.username == other }
                ChatItem.ExistingChat(
                    conversationId = conv.id,
                    otherUsername = other,
                    otherFullName = otherUser?.fullName ?: other,
                    lastMessage = conv.lastMessage?.let { "${it.senderUsername}: ${it.content}" }
                )
            }

            val existingUsernames = conversations.flatMap { it.participants }.toSet()

            val contacts = allUsers
                .filter { it.username != me.username && it.username !in existingUsernames }
                .map { ChatItem.Contact(id = it.id, username = it.username, fullName = it.fullName) }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    existingChats = existingChats,
                    contacts = contacts,
                    currentUsername = me.username
                )
            }
        }
    }

    fun startChat(username: String): String? {
        val existing = _uiState.value.existingChats.firstOrNull { it.otherUsername == username }
        return existing?.conversationId
    }

    fun createChat(username: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = chatRepository.createConversation(listOf(username))) {
                is Resource.Success -> {
                    onCreated(result.data.id)
                    loadData()
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            container.authRepository.logout()
        }
    }
}
