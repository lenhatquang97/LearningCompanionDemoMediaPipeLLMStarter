package com.examples.learningcompanion.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.examples.learningcompanion.singleton.InferenceSingleton
import com.examples.learningcompanion.viewstate.MediaPipeUiState
import com.examples.learningcompanion.model.LLMModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

private class MissingAccessTokenException :
    Exception("Please try again after sign in")

private class UnauthorizedAccessException :
    Exception("Access denied. Please try again and grant the necessary permissions.")

private class MissingUrlException(message: String) :
    Exception(message)

private class ModelLoadFailException :
    Exception("Failed to load model, please try again")

private class ModelSessionCreateFailException :
    Exception("Failed to create model session, please try again")

@Suppress("UNCHECKED_CAST")
@SuppressLint("StaticFieldLeak")
class MediaPipeLLMViewModel(private val context: Context) : ViewModel() {

    private val _uiState: MutableStateFlow<MediaPipeUiState> = MutableStateFlow(MediaPipeUiState(isDownloading = false, errorMessage = ""))
    val uiState: StateFlow<MediaPipeUiState> =_uiState.asStateFlow()

    private val _progress: MutableStateFlow<Int> = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    fun loadModel(client: OkHttpClient, onModelLoaded: () -> Unit = { }) {
        //TODO: Implement this function
    }

    fun downloadModel(
        context: Context,
        llmModel: LLMModel,
        client: OkHttpClient,
        onProgressUpdate: (Int) -> Unit
    ) {
        val requestBuilder = Request.Builder().url(llmModel.url)
        val outputFile = File(InferenceSingleton.Companion.modelPathFromUrl(context))
        val response = client.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful) {
            throw Exception("Download failed: ${response.code}")
        }

        response.body?.byteStream()?.use { inputStream ->
            FileOutputStream(outputFile).use { outputStream ->
                val buffer = ByteArray(4096)
                var bytesRead: Int
                var totalBytesRead = 0L
                val contentLength = response.body?.contentLength() ?: -1

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    val progress = if (contentLength > 0) {
                        (totalBytesRead * 100 / contentLength).toInt()
                    } else {
                        -1
                    }
                    onProgressUpdate(progress)
                }
                outputStream.flush()
            }
        }
    }

    suspend fun deleteDownloadedFile(context: Context) {
        withContext(Dispatchers.IO) {
            val outputFile = File(InferenceSingleton.Companion.modelPathFromUrl(context))
            if (outputFile.exists()) {
                outputFile.delete()
            }
        }
    }

    suspend fun cancelDownload() {
        deleteDownloadedFile(context)
        val errorMessage = "Download Cancelled"
        _uiState.value = MediaPipeUiState(isDownloading = false, errorMessage = errorMessage)
    }

    companion object {
        fun getFactory(context: Context) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return MediaPipeLLMViewModel(context) as T
            }
        }
    }
}