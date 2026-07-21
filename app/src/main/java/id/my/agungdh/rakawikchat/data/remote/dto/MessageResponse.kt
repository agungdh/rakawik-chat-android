package id.my.agungdh.rakawikchat.data.remote.dto

data class MessageResponse(
    val id: String?,
    val conversationId: String,
    val senderUsername: String,
    val content: String,
    val contentType: String?,
    val timestamp: String?,
    val readBy: List<String>?
)
