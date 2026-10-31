package com.example.githubclient.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.githubclient.data.GitHubRepository
import com.example.githubclient.data.Repo
import com.example.githubclient.data.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReposUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val repos: List<Repo> = emptyList(),
    val query: String = "",
    val error: String? = null
) {
    val filtered: List<Repo>
        get() {
            val q = query.trim()
            if (q.isEmpty()) return repos
            return repos.filter { repo ->
                repo.fullName.contains(q, ignoreCase = true) ||
                    (repo.description?.contains(q, ignoreCase = true) == true)
            }
        }
}

class ReposViewModel(private val repository: GitHubRepository) : ViewModel() {

    private val _state = MutableStateFlow(ReposUiState())
    val state: StateFlow<ReposUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun refresh() = load()

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = it.repos.isEmpty(),
                    isRefreshing = it.repos.isNotEmpty(),
                    error = null
                )
            }
            repository.getAllRepos().fold(
                onSuccess = { list ->
                    _state.update {
                        it.copy(isLoading = false, isRefreshing = false, repos = list, error = null)
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = error.toUserMessage()
                        )
                    }
                }
            )
        }
    }

    companion object {
        fun factory(repository: GitHubRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { ReposViewModel(repository) }
            }
    }
}
