package com.example.todoapp

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

    //Data-класс для зранения состояния UI
data class DataUIState(
    var isTextEditVisible: Boolean = false
)

    //ViewModel переживающий пересоздание activity
class ToDoItemViewModel : ViewModel() {
    lateinit var toDoItemRecyclerAdapter: ToDoItemRecyclerAdapter
    lateinit var toDoItemRepository: ToDoItemRepository
    var isAdaptRepInitialize = false

    private val _uiState = MutableStateFlow(DataUIState())
    val uiState: StateFlow<DataUIState> = _uiState.asStateFlow()

        //сохрание информации
    fun setTextEditVisible(bool: Boolean){
        _uiState.update { currentState ->
            currentState.copy(
                isTextEditVisible = bool
            ) }
    }

}