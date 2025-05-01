package com.examples.learningcompanion.viewstate

import androidx.compose.runtime.toMutableStateList
import com.examples.learningcompanion.model.ChatMessageModel

/** Management of the message queue. */
@Suppress("PropertyName")
class ChatUiState(
    val supportsThinking: Boolean = false,
    messages: List<ChatMessageModel> = emptyList()
)  {
    val _messages: MutableList<ChatMessageModel> = messages.toMutableStateList()
    val messages: List<ChatMessageModel> = _messages.asReversed()
    var _currentMessageId = ""

    companion object {
        const val USER_PREFIX = "user"
        const val MODEL_PREFIX = "model"
        const val THINKING_MARKER_END = "</think>"
    }
}
