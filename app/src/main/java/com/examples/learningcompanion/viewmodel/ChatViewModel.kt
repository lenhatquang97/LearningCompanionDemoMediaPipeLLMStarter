package com.examples.learningcompanion.viewmodel

import android.content.Context
import androidx.compose.ui.focus.FocusState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.examples.learningcompanion.model.ChatMessageModel
import com.examples.learningcompanion.singleton.InferenceSingleton
import com.examples.learningcompanion.viewstate.ChatUiState
import com.examples.learningcompanion.viewstate.ChatUiState.Companion.MODEL_PREFIX
import com.examples.learningcompanion.viewstate.ChatUiState.Companion.THINKING_MARKER_END

import com.examples.learningcompanion.viewstate.ChatUiState.Companion.USER_PREFIX
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

@Suppress("UNCHECKED_CAST")
class ChatViewModel(
    private var inferenceModel: InferenceSingleton
) : ViewModel() {
    private val _uiState: MutableStateFlow<ChatUiState> = MutableStateFlow(inferenceModel.uiState)
    val uiState: StateFlow<ChatUiState> =_uiState.asStateFlow()

    private val _tokensRemaining = MutableStateFlow(-1)
    val tokensRemaining: StateFlow<Int> = _tokensRemaining.asStateFlow()

    private val _textInputEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isTextInputEnabled: StateFlow<Boolean> = _textInputEnabled.asStateFlow()

    fun onEnterChatScreen(context: Context) {
        val inferenceModel = InferenceSingleton.Companion.getInstance(context)
        resetInferenceModel(inferenceModel)
    }

    fun sendMessage(userMessage: String) {
        //TODO: Implement this function
    }

    fun appendMessage(text: String) {
        val index = _uiState.value._messages.indexOfFirst { it.id == _uiState.value._currentMessageId }

        if (text.contains(THINKING_MARKER_END)) { // The model is done thinking, we add a new bubble
            val thinkingEnd = text.indexOf(THINKING_MARKER_END) + THINKING_MARKER_END.length

            // Add text to current "thinking" bubble
            val prefix = text.substring(0, thinkingEnd)
            val suffix = text.substring(thinkingEnd)

            appendToMessage(_uiState.value._currentMessageId, prefix)

            if (_uiState.value._messages[index].isEmpty) {
                // There are no thoughts from the model. We can just re-use the current bubble
                _uiState.value._messages[index] = _uiState.value._messages[index].copy(
                    isThinking = false
                )
                appendToMessage(_uiState.value._currentMessageId, suffix)
            } else {
                // Create a new bubble for the remainder of the model response
                val message = ChatMessageModel(
                    rawMessage = suffix,
                    author = MODEL_PREFIX,
                    isLoading = true,
                    isThinking = false
                )
                _uiState.value._messages.add(message)
                _uiState.value._currentMessageId = message.id
            }
        } else {
            appendToMessage(_uiState.value._currentMessageId, text)
        }
    }

    fun onTokenChange(userMessage: String) {
        if (!userMessage.contains(" ") || userMessage.trim() != userMessage)  {
            recomputeSizeInTokens(userMessage)
        }
    }

    fun onFocusChanged(focusState: FocusState, userMessage: String) {
        if (focusState.isFocused) {
            recomputeSizeInTokens(userMessage)
        }
    }

    fun onClickClearChat(context: Context) {
        InferenceSingleton.Companion.getInstance(context).resetSession()
        clearMessages()
        recomputeSizeInTokens("")
    }

    fun onClickCloseChatIcon(context: Context) {
        InferenceSingleton.Companion.getInstance(context).close()
        clearMessages()
        recomputeSizeInTokens("")
    }

    fun llmModelName(): String = InferenceSingleton.Companion.llmModel.toString()

    private fun recomputeSizeInTokens(message: String) {
        val remainingTokens = inferenceModel.estimateTokensRemaining(message)
        _tokensRemaining.value = remainingTokens
    }

    private fun createLoadingMessage() {
        val chatMessage = ChatMessageModel(author = MODEL_PREFIX, isLoading = true, isThinking = _uiState.value.supportsThinking)
        _uiState.value._messages.add(chatMessage)
        _uiState.value._currentMessageId = chatMessage.id
    }

    private fun clearMessages() {
        _uiState.value._messages.clear()
    }

    /** Creates a new message with the specified text and author. */
    private fun addMessage(text: String, author: String) {
        val chatMessage = ChatMessageModel(
            rawMessage = text,
            author = author
        )
        _uiState.value._messages.add(chatMessage)
        _uiState.value._currentMessageId = chatMessage.id
    }

    private fun appendToMessage(id: String, suffix: String) : Int {
        val index = _uiState.value._messages.indexOfFirst { it.id == id }
        val newText =  suffix.replace(THINKING_MARKER_END, "")
        _uiState.value._messages[index] = _uiState.value._messages[index].copy(
            rawMessage = _uiState.value._messages[index].rawMessage + newText,
            isLoading = false
        )
        return index
    }

    private fun setInputEnabled(isEnabled: Boolean) {
        _textInputEnabled.value = isEnabled
    }

    private fun resetInferenceModel(newModel: InferenceSingleton) {
        inferenceModel = newModel
        _uiState.value = inferenceModel.uiState
    }

    companion object {
        fun getFactory(context: Context) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val inferenceModel = InferenceSingleton.Companion.getInstance(context)
                return ChatViewModel(inferenceModel) as T
            }
        }
    }
}