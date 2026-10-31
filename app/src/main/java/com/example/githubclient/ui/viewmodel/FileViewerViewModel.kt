package com.example.githubclient.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.githubclient.data.GitHubRepository
import com.example.githubclient.data.LoadedFile
import com.example.githubclient.data.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FileViewerViewModel(
    private val repository: GitHubRepository,
    private val owner: String,
    private val repo: String,
    private val path: String
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<LoadedFile>>(UiState.Loading)
    val state: StateFlow<UiState<LoadedFile>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            repository.getFile(owner, repo, path).fold(
                onSuccess = { file -> _state.value = UiState.Success(file) },
                onFailure = { error -> _state.value = UiState.Error(error.toUserMessage()) }
            )
        }
    }

    companion object {
        fun factory(
            repository: GitHubRepository,
            owner: String,
            repo: String,
            path: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { FileViewerViewModel(repository, owner, repo, path) }
        }
    }
}
