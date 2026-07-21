package id.my.agungdh.rakawikchat.data.repository

import id.my.agungdh.rakawikchat.AppContainer
import id.my.agungdh.rakawikchat.data.remote.ApiService
import id.my.agungdh.rakawikchat.data.remote.StompClient
import id.my.agungdh.rakawikchat.data.remote.dto.*
import id.my.agungdh.rakawikchat.util.Resource

class ChatRepository(
    private val apiService: ApiService,
    private val stompClient: StompClient,
    private val container: AppContainer
) {

    suspend fun getConversations(): Resource<List<ConversationResponse>> {
        return try {
            val response = apiService.getConversations()
            if (response.isSuccessful && response.body()?.success == true) {
                Resource.Success(response.body()!!.data!!)
            } else {
                Resource.Error(response.body()?.message ?: "Failed to load conversations")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun createConversation(participants: List<String>): Resource<ConversationResponse> {
        return try {
            val response = apiService.createConversation(CreateConversationRequest(participants))
            if (response.isSuccessful && response.body()?.success == true) {
                Resource.Success(response.body()!!.data!!)
            } else {
                Resource.Error(response.body()?.message ?: "Failed to create conversation")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun getMessages(conversationId: String): Resource<List<MessageResponse>> {
        return try {
            val response = apiService.getMessages(conversationId)
            if (response.isSuccessful && response.body()?.success == true) {
                Resource.Success(response.body()!!.data!!)
            } else {
                Resource.Error(response.body()?.message ?: "Failed to load messages")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun markAsRead(conversationId: String) {
        try {
            apiService.markAsRead(conversationId)
        } catch (_: Exception) {}
    }

    fun connectStomp(onConnected: () -> Unit, onError: (Throwable) -> Unit) {
        val token = container.currentToken
        if (token != null) {
            stompClient.connect(token, onConnected, onError)
        } else {
            onError(RuntimeException("No auth token"))
        }
    }

    fun subscribeToMessages(
        conversationId: String,
        onMessage: (MessageResponse) -> Unit
    ) {
        val destination = "/topic/conversation/$conversationId"
        stompClient.subscribe(destination, onMessage)
    }

    fun unsubscribeFromMessages(conversationId: String) {
        val destination = "/topic/conversation/$conversationId"
        stompClient.unsubscribe(destination)
    }

    fun sendMessage(message: SendMessageRequest) {
        stompClient.send("/app/chat.send", message)
    }

    fun disconnectStomp() {
        stompClient.disconnect()
    }
}
