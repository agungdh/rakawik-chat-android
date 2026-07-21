package id.my.agungdh.rakawikchat.data.remote.dto

data class SendMessageRequest(
    val conversationId: String,
    val content: String,
    val contentType: String = "TEXT"
)
