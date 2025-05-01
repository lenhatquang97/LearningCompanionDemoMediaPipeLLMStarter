package com.examples.learningcompanion.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.examples.learningcompanion.model.LLMModel
import com.examples.learningcompanion.viewmodel.SelectionViewModel

@Composable
internal fun SelectionRoute(
    onModelSelected: () -> Unit = {},
) {
    val selectionViewModel: SelectionViewModel = viewModel(factory = SelectionViewModel.Companion.getFactory())
    val uiState = selectionViewModel.uiState.collectAsState()
    LaunchedEffect(uiState.value) {
        if (uiState.value != LLMModel.NONE) {
            onModelSelected()
        }
    }

    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
    ) {
        items(LLMModel.entries.filter { it != LLMModel.NONE }) { model ->
            Button(
                onClick = {
                    selectionViewModel.onChooseModel(model)

                },
            ) {
                Text(model.toString())
            }
        }
    }
}
