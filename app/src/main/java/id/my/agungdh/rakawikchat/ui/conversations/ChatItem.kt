package id.my.agungdh.rakawikchat.ui.conversations

sealed class ChatItem {
    data class ExistingChat(
        val conversationId: String,
        val otherUsername: String,
        val otherFullName: String,
        val lastMessage: String?,
        val online: Boolean = false
    ) : ChatItem()

    data class Contact(
        val id: Long,
        val username: String,
        val fullName: String,
        val online: Boolean = false
    ) : ChatItem()
}
