package com.examples.learningcompanion.singleton

import android.content.Context
import android.util.Log
import com.examples.learningcompanion.model.LLMModel
import com.examples.learningcompanion.viewstate.ChatUiState
import java.io.File
import kotlin.math.max

//TODO: Implement this class
class InferenceSingleton private constructor(context: Context) {
    private val tag = InferenceSingleton::class.qualifiedName

    val uiState = ChatUiState(llmModel.thinking)

    init {
        if (!modelExists(context)) {
            throw IllegalArgumentException("Model not found at path: ${llmModel.path}")
        }

        createEngine(context)
        createSession()
    }

    fun close() {
    }

    fun resetSession() {
    }

    private fun createEngine(context: Context) {

    }

    private fun createSession() {

    }

    fun generateResponseAsync() {

    }

    fun estimateTokensRemaining(prompt: String): Int {
        return 0
    }

    companion object {
        var llmModel: LLMModel = LLMModel.NONE
        private var instance: InferenceSingleton? = null

        fun getInstance(context: Context): InferenceSingleton {
            return if (instance != null) {
                instance!!
            } else {
                InferenceSingleton(context).also { instance = it }
            }
        }

        fun resetInstance(context: Context): InferenceSingleton {
            return InferenceSingleton(context).also { instance = it }
        }

        fun modelPathFromUrl(context: Context): String {
            if (llmModel.url.isNotEmpty()) {
                val urlFileName = llmModel.fileName
                if (urlFileName.isNotEmpty()) {
                    return File(context.filesDir, urlFileName).absolutePath
                }
            }

            return ""
        }

        fun modelPath(context: Context): String {
            val modelFile = File(llmModel.path)
            if (modelFile.exists()) {
                return llmModel.path
            }

            return modelPathFromUrl(context)
        }

        fun modelExists(context: Context): Boolean {
            return File(modelPath(context)).exists()
        }

        /** The maximum number of tokens the model can process. */
        var MAX_TOKENS = 2048

        /**
         * An offset in tokens that we use to ensure that the model always has the ability to respond when
         * we compute the remaining context length.
         */
        var DECODE_TOKEN_OFFSET = 256
    }
}
