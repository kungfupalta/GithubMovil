package com.example.githubclient.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.githubclient.data.ContentItem
import com.example.githubclient.data.GitHubRepository
import com.example.githubclient.data.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExplorerViewModel(
    private val repository: GitHubRepository,
    private val owner: String,
    private val repo: String,
    private val path: String
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<ContentItem>>>(UiState.Loading)
    val state: StateFlow<UiState<List<ContentItem>>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            repository.getContents(owner, repo, path).fold(
                onSuccess = { items ->
                    // Carpetas primero y luego archivos, ambos por orden alfabético.
                    val sorted = items.sortedWith(
                        compareBy<ContentItem>({ it.type != "dir" }, { it.name.lowercase() })
                    )
                    _state.value = UiState.Success(sorted)
                },
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
            initializer { ExplorerViewModel(repository, owner, repo, path) }
        }
    }
}
