# GDGoC Learning Companion App Demo

## Overview

This is a starter codebase project without MediaPipe GenAI. To see the final project, you should go to here: <https://github.com/lenhatquang97/LearningCompanionDemoMediaPipeLLM>

## Prerequisites

- You need to know that when you copy paste the source code, you should import the library to prevent red error.

- Android Studio version must be at least from Android Studio Hedgehog. You can install the latest Android Studio version.

- A physical Android device with a minimum OS version of SDK 24 (Android 7.0 - Nougat) with developer mode enabled. Device should be at least 4GB RAM for better experience.

## Current project architecture

- We will follow the guideline from Google: <https://developer.android.com/topic/architecture>
- In case the data layer is InferenceSingleton
- UI layer will integrate Model-View-ViewModel (MVVM) so that business logic must be in the ViewModel

## How to integrate into demo project

### Step 1: Add dependencies to build.gradle.kts in app folder and apply config in AndroidManifest.xml

In app folder in build.gradle.kts, alter TODO line with this line
Source code

```kotlin
implementation ("com.google.mediapipe:tasks-genai:0.10.22")
```

Next in AndroidManifest.xml, alter TODO line with this code. This code is required after adding genai package. OpenCL is a open-source GPU library, just as the competitor of CUDA.

```xml
<!-- Required to initialize the LlmInference -->
<uses-native-library
    android:name="libOpenCL.so"
    android:required="false"/>
<uses-native-library android:name="libOpenCL-car.so" android:required="false"/>
<uses-native-library android:name="libOpenCL-pixel.so" android:required="false"/>
```

### Step 2: Implement Data Model - InferenceSingleton

Copy paste the source code below to alter

```kotlin
package com.examples.learningcompanion.singleton

import android.content.Context
import android.util.Log
import com.google.common.util.concurrent.ListenableFuture
import com.examples.learningcompanion.model.LLMModel
import com.examples.learningcompanion.viewstate.ChatUiState
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession.LlmInferenceSessionOptions
import com.google.mediapipe.tasks.genai.llminference.ProgressListener
import java.io.File
import kotlin.math.max

class InferenceSingleton private constructor(context: Context) {
    private lateinit var llmInference: LlmInference
    private lateinit var llmInferenceSession: LlmInferenceSession
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
        llmInferenceSession.close()
        llmInference.close()
    }

    fun resetSession() {
        llmInferenceSession.close()
        createSession()
    }

    private fun createEngine(context: Context) {
        val inferenceOptions = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath(context))
            .setMaxTokens(MAX_TOKENS)
            .apply { llmModel.preferredBackend?.let { setPreferredBackend(it) } }
            .build()

        try {
            llmInference = LlmInference.createFromOptions(context, inferenceOptions)
        } catch (e: Exception) {
            Log.e(tag, "Load model error: ${e.message}", e)
            throw e
        }
    }

    private fun createSession() {
        val sessionOptions =  LlmInferenceSessionOptions.builder()
            .setTemperature(llmModel.temperature)
            .setTopK(llmModel.topK)
            .setTopP(llmModel.topP)
            .build()

        try {
            llmInferenceSession =
                LlmInferenceSession.createFromOptions(llmInference, sessionOptions)
        } catch (e: Exception) {
            Log.e(tag, "LlmInferenceSession create error: ${e.message}", e)
            throw e
        }
    }

    fun generateResponseAsync(prompt: String, progressListener: ProgressListener<String>) : ListenableFuture<String> {
        llmInferenceSession.addQueryChunk(prompt)
        return llmInferenceSession.generateResponseAsync(progressListener)
    }

    fun estimateTokensRemaining(prompt: String): Int {
        val context = uiState.messages.joinToString { it.rawMessage } + prompt
        if (context.isEmpty()) return -1 // Special marker if no content has been added

        val sizeOfAllMessages = llmInferenceSession.sizeInTokens(context)
        val approximateControlTokens = uiState.messages.size * 3
        val remainingTokens = MAX_TOKENS - sizeOfAllMessages - approximateControlTokens -  DECODE_TOKEN_OFFSET
        // Token size is approximate so, let's not return anything below 0
        return max(0, remainingTokens)
    }

    companion object {
        var llmModel: LLMModel = LLMModel.GEMMA3_1B_IT_GPU
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
```

### Step 3: Integrate codebase logic inside ViewModel

Copy and paste these lines to alter TODO with each files below:

SelectionViewModel.kt

```kotlin
fun onChooseModel(model: LLMModel) {
    InferenceSingleton.Companion.llmModel = model
    _uiState.value = model
}
```

MediaPipeLLMViewModel.kt

```kotlin
fun loadModel(client: OkHttpClient, onModelLoaded: () -> Unit = { }) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!InferenceSingleton.Companion.modelExists(context)) {
                    if (InferenceSingleton.Companion.llmModel.url.isEmpty()) {
                        throw MissingUrlException("Please manually copy the model to ${InferenceSingleton.Companion.llmModel.path}")
                    }
                    _uiState.value = MediaPipeUiState(isDownloading = true, errorMessage = "")
                    downloadModel(context, InferenceSingleton.Companion.llmModel, client) { newProgress ->
                        _progress.value = newProgress
                    }
                }

                InferenceSingleton.Companion.resetInstance(context)
                // Notify the UI that the model has finished loading
                withContext(Dispatchers.Main) {
                    onModelLoaded()
                }
            } catch (e: MissingAccessTokenException) {
                val msg = e.localizedMessage ?: "Unknown Error"
                _uiState.value = MediaPipeUiState(isDownloading = false, errorMessage = msg)
            } catch (e: MissingUrlException) {
                val msg = e.localizedMessage ?: "Unknown Error"
                _uiState.value = MediaPipeUiState(isDownloading = false, errorMessage = msg)
            } catch (e: UnauthorizedAccessException) {
                val msg = e.localizedMessage ?: "Unknown Error"
                _uiState.value = MediaPipeUiState(isDownloading = false, errorMessage = msg)
            } catch (e: ModelSessionCreateFailException) {
                val msg = e.localizedMessage ?: "Unknown Error"
                _uiState.value = MediaPipeUiState(isDownloading = false, errorMessage = msg)
            } catch (e: ModelLoadFailException) {
                val msg = e.localizedMessage ?: "Unknown Error"
                _uiState.value = MediaPipeUiState(isDownloading = false, errorMessage = msg)
                // Remove invalid model file
                CoroutineScope(Dispatchers.Main).launch {
                    deleteDownloadedFile(context)
                }
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Unknown Error"
                val manualCopyMsg = "${msg}, please manually copy the model to ${InferenceSingleton.Companion.llmModel.path}"
                _uiState.value = MediaPipeUiState(isDownloading = false, errorMessage = manualCopyMsg)
            }
        }
    }
```

ChatViewModel.kt

```kotlin
fun sendMessage(userMessage: String) {
        viewModelScope.launch(Dispatchers.IO) {
            addMessage(userMessage, USER_PREFIX)
            createLoadingMessage()
            setInputEnabled(false)
            try {
                val asyncInference =  inferenceModel.generateResponseAsync(userMessage) { partialResult, done ->
                    appendMessage(partialResult)
                    if (done) {
                        // Re-enable text input
                        setInputEnabled(true)
                    } else {
                        // Reduce current token count (estimate only). sizeInTokens() will be used
                        // when computation is done
                        _tokensRemaining.update { max(0, it - 1) }
                    }
                }
                // Once the inference is done, recompute the remaining size in tokens
                asyncInference.addListener({
                    viewModelScope.launch(Dispatchers.IO) {
                        recomputeSizeInTokens(userMessage)
                    }
                }, Dispatchers.Main.asExecutor())
            } catch (e: Exception) {
                addMessage(e.localizedMessage ?: "Unknown Error", MODEL_PREFIX)
                setInputEnabled(true)
            }
        }
    }
```

### Step 4: Update list enum LLMModel

Note that preferredBackend data type should be changed from Any? to LlmInference.Backend?

```kotlin
enum class LLMModel(
    val path: String,
    val url: String,
    val fileName: String,
    val preferredBackend: LlmInference.Backend?,
    val thinking: Boolean,
    val temperature: Float,
    val topK: Int,
    val topP: Float,
) {
    NONE(path = "", url = "", fileName = "", preferredBackend = LlmInference.Backend.DEFAULT, thinking = false, temperature = 0.0f, topK = 0, topP = 0f),
    GEMMA3_1B_IT_CPU(
        path = "/data/local/tmp/Gemma3-1B-IT_multi-prefill-seq_q8_ekv2048.task",
        url = "https://www.dropbox.com/scl/fi/mky83xnln8lr2ub3a38t3/Gemma3-1B-IT_multi-prefill-seq_q8_ekv2048.task?rlkey=kdoaltng60a4r0gqxjt5cqhvg&st=ztaa63zk&dl=1",
        fileName = "Gemma3-1B-IT_multi-prefill-seq_q8_ekv2048.task",
        preferredBackend = LlmInference.Backend.CPU,
        thinking = false,
        temperature = 1.0f,
        topK = 64,
        topP = 0.95f
    ),
    GEMMA3_1B_IT_GPU(
        path = "/data/local/tmp/Gemma3-1B-IT_multi-prefill-seq_q8_ekv2048.task",
        url = "https://www.dropbox.com/scl/fi/mky83xnln8lr2ub3a38t3/Gemma3-1B-IT_multi-prefill-seq_q8_ekv2048.task?rlkey=kdoaltng60a4r0gqxjt5cqhvg&st=ztaa63zk&dl=1",
        fileName = "Gemma3-1B-IT_multi-prefill-seq_q8_ekv2048.task",
        preferredBackend = LlmInference.Backend.GPU,
        thinking = false,
        temperature = 1.0f,
        topK = 64,
        topP = 0.95f
    ),
    QWEN2_0_5B_INSTRUCT(
        path = "/data/local/tmp/Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
        url = "https://www.dropbox.com/scl/fi/pcc49uxru5rkhi1nmhgb6/Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task?rlkey=jw54doktn43k0rv13le5kh06d&e=1&st=ajy89egf&dl=1",
        fileName = "Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
        preferredBackend = LlmInference.Backend.CPU,
        thinking = false,
        temperature = 0.95f,
        topK = 40,
        topP = 1.0f
    ),
}
```

### Step 5: Run it

1. Select Model: The user first selects a model from the model selection screen.

2. Download Model: If the model has not been downloaded previously, the app will download it from Dropbox link I have uploaded. In fact this model can be retrieved from <https://huggingface.co/litert-community>.

3. Chat with Model: Once the model is downloaded, the user can interact with it by entering prompts and receiving responses.

**In case you cannot download directly because of the Internet connection, you can use the Dropbox URL to download itself and copy to the path in Step 4. You can use option Open in Device Explorer in Device Manager in Android Studio.**
