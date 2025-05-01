package com.examples.learningcompanion.model

//TODO: Add multiple values here
enum class LLMModel(
    val path: String,
    val url: String,
    val fileName: String,
    val preferredBackend: Any?,
    val thinking: Boolean,
    val temperature: Float,
    val topK: Int,
    val topP: Float,
) {
    NONE(path = "", url = "", fileName = "", preferredBackend = null, thinking = false, temperature = 0.0f, topK = 0, topP = 0f),
}