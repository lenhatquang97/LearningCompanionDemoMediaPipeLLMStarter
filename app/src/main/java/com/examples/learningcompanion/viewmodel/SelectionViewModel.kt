package com.examples.learningcompanion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.examples.learningcompanion.model.LLMModel
import com.examples.learningcompanion.singleton.InferenceSingleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Suppress("UNCHECKED_CAST")
class SelectionViewModel: ViewModel() {

    private val _uiState: MutableStateFlow<LLMModel> = MutableStateFlow(LLMModel.NONE)
    val uiState: StateFlow<LLMModel> =_uiState.asStateFlow()

    fun onChooseModel(model: LLMModel) {
        //TODO: Implement this function
    }
    companion object {
        fun getFactory() = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return SelectionViewModel() as T
            }
        }
    }
}