package com.examples.learningcompanion.model

import com.examples.learningcompanion.viewstate.ChatUiState.Companion.USER_PREFIX
import java.util.UUID

data class ChatMessageModel(
    val id: String = UUID.randomUUID().toString(),
    val rawMessage: String = "",
    val author: String,
    val isLoading: Boolean = false,
    val isThinking: Boolean = false,
) {
    val isEmpty: Boolean
        get() = rawMessage.trim().isEmpty()
    val isFromUser: Boolean
        get() = author == USER_PREFIX
    val message: String
        get() = rawMessage.trim()
}