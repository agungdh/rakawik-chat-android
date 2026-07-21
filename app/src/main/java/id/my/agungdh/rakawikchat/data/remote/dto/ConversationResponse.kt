package id.my.agungdh.rakawikchat.data.remote.dto

data class ConversationResponse(
    val id: String,
    val participants: List<String>,
    val createdAt: String?,
    val updatedAt: String?,
    val lastMessage: MessageResponse?
)
