package com.examples.learningcompanion.view

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.examples.learningcompanion.R
import com.examples.learningcompanion.viewstate.MediaPipeUiState
import com.examples.learningcompanion.viewmodel.MediaPipeLLMViewModel
import kotlinx.coroutines.*
import okhttp3.OkHttpClient

@Composable
internal fun LoadingRoute(
    onModelLoaded: () -> Unit = { },
    onGoBack: () -> Unit = {}
) {
    val context = LocalContext.current.applicationContext
    val mediaPipeLLMViewModel: MediaPipeLLMViewModel = viewModel(factory = MediaPipeLLMViewModel.Companion.getFactory(context))
    val uiState: State<MediaPipeUiState> = mediaPipeLLMViewModel.uiState.collectAsState()
    val progress: State<Int> = mediaPipeLLMViewModel.progress.collectAsState()
    val client = remember { OkHttpClient() }

    if (uiState.value.errorMessage != "") {
        ErrorMessage(uiState.value.errorMessage, onGoBack)
    } else if (uiState.value.isDownloading) {
        DownloadIndicator(progress.value) {
            CoroutineScope(Dispatchers.Main).launch {
                mediaPipeLLMViewModel.cancelDownload()
            }
        }
    } else {
        LoadingIndicator()
    }

    LaunchedEffect(Unit) {
        mediaPipeLLMViewModel.loadModel(client, onModelLoaded)
    }
}

@Composable
fun DownloadIndicator(progress: Int, onCancel: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Downloading Model: $progress%",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        CircularProgressIndicator(progress = { progress / 100f })
        Button(onClick = onCancel, modifier = Modifier.padding(top = 8.dp)) {
            Text("Cancel")
        }
    }
}

@Composable
fun LoadingIndicator() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.loading_model),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(bottom = 8.dp)
        )
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorMessage(
    errorMessage: String,
    onGoBack: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
        Button(onClick = onGoBack, modifier = Modifier.padding(top = 16.dp)) {
            Text("Go Back")
        }
    }
}

