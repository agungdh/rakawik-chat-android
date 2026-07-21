package id.my.agungdh.rakawikchat.ui.conversations

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.my.agungdh.rakawikchat.RakawikChatApp
import id.my.agungdh.rakawikchat.data.remote.dto.ConversationResponse
import id.my.agungdh.rakawikchat.data.remote.dto.UserResponse
import id.my.agungdh.rakawikchat.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConversationsUiState(
    val conversations: List<ConversationResponse> = emptyList(),
    val users: List<UserResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentUser: UserResponse? = null,
    val showCreateDialog: Boolean = false,
    val selectedUserIds: Set<Long> = emptySet()
)

class ConversationsViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as RakawikChatApp).container
    private val chatRepository = container.chatRepository
    private val userRepository = container.userRepository

    private val _uiState = MutableStateFlow(ConversationsUiState())
    val uiState: StateFlow<ConversationsUiState> = _uiState.asStateFlow()

    init {
        loadConversations()
        loadCurrentUser()
    }

    fun loadConversations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = chatRepository.getConversations()) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, conversations = result.data) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            when (val result = userRepository.getCurrentUser()) {
                is Resource.Success -> {
                    _uiState.update { it.copy(currentUser = result.data) }
                }
                else -> {}
            }
        }
    }

    fun loadUsers() {
        viewModelScope.launch {
            when (val result = userRepository.getAllUsers()) {
                is Resource.Success -> {
                    val currentUsername = _uiState.value.currentUser?.username
                    _uiState.update {
                        it.copy(
                            users = result.data.filter { u -> u.username != currentUsername },
                            showCreateDialog = true
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun toggleUser(userId: Long) {
        _uiState.update {
            val newSet = it.selectedUserIds.toMutableSet()
            if (newSet.contains(userId)) newSet.remove(userId) else newSet.add(userId)
            it.copy(selectedUserIds = newSet)
        }
    }

    fun hideCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false, selectedUserIds = emptySet()) }
    }

    fun createConversation() {
        val selectedUsernames = _uiState.value.users
            .filter { _uiState.value.selectedUserIds.contains(it.id) }
            .map { it.username }

        if (selectedUsernames.isEmpty()) return

        viewModelScope.launch {
            when (val result = chatRepository.createConversation(selectedUsernames)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(showCreateDialog = false, selectedUserIds = emptySet()) }
                    loadConversations()
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            container.authRepository.logout()
            _uiState.update { it.copy(conversations = emptyList()) }
        }
    }
}
