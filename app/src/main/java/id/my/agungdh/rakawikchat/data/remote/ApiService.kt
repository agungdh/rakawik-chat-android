package id.my.agungdh.rakawikchat.data.remote

import id.my.agungdh.rakawikchat.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginResponse>>

    @POST("/api/auth/logout")
    suspend fun logout(): Response<ApiResponse<Any>>

    @GET("/api/users/me")
    suspend fun getCurrentUser(): Response<ApiResponse<UserResponse>>

    @GET("/api/users")
    suspend fun getAllUsers(): Response<ApiResponse<List<UserResponse>>>

    @GET("/api/conversations")
    suspend fun getConversations(): Response<ApiResponse<List<ConversationResponse>>>

    @POST("/api/conversations")
    suspend fun createConversation(@Body request: CreateConversationRequest): Response<ApiResponse<ConversationResponse>>

    @GET("/api/conversations/{id}/messages")
    suspend fun getMessages(@Path("id") conversationId: String): Response<ApiResponse<List<MessageResponse>>>

    @PUT("/api/conversations/{id}/read")
    suspend fun markAsRead(@Path("id") conversationId: String): Response<ApiResponse<Any>>
}
