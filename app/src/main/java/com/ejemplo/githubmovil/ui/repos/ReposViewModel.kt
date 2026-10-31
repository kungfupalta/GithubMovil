package com.ejemplo.githubmovil.ui.repos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ejemplo.githubmovil.data.model.CreateRepoRequest
import com.ejemplo.githubmovil.data.model.GitHubRepo
import com.ejemplo.githubmovil.data.remote.GithubApiService
import com.ejemplo.githubmovil.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ReposUiState {
    object Loading : ReposUiState()
    data class Success(val repos: List<GitHubRepo>) : ReposUiState()
    data class Error(val message: String) : ReposUiState()
}

class ReposViewModel(application: Application) : AndroidViewModel(application) {
    private val api: GithubApiService = RetrofitClient.getInstance(application)

    private val _uiState = MutableStateFlow<ReposUiState>(ReposUiState.Loading)
    val uiState: StateFlow<ReposUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadRepositories()
    }

    fun loadRepositories() {
        viewModelScope.launch {
            _uiState.value = ReposUiState.Loading
            try {
                val allRepos = mutableListOf<GitHubRepo>()
                var page = 1
                var hasMore = true

                while (hasMore) {
                    val response = api.getUserRepos(page = page, perPage = 100)
                    if (response.isSuccessful && response.body() != null) {
                        val list = response.body()!!
                        allRepos.addAll(list)
                        if (list.size < 100) {
                            hasMore = false
                        } else {
                            page++
                        }
                    } else {
                        val errorMsg = when (response.code()) {
                            403 -> "Límite de API excedido o permisos insuficientes (403 Forbidden)."
                            404 -> "Recurso no encontrado (404 Not Found)."
                            else -> "Error al cargar repositorios: código ${response.code()}"
                        }
                        _uiState.value = ReposUiState.Error(errorMsg)
                        return@launch
                    }
                }
                _uiState.value = ReposUiState.Success(allRepos)
            } catch (e: Exception) {
                _uiState.value = ReposUiState.Error("Error de red: ${e.localizedMessage ?: "Verificá tu conexión."}")
            }
        }
    }

    fun searchRepositories(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            loadRepositories()
            return
        }

        viewModelScope.launch {
            _uiState.value = ReposUiState.Loading
            try {
                val response = api.searchRepos(query = "$query in:name", page = 1)
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = ReposUiState.Success(response.body()!!.items)
                } else {
                    _uiState.value = ReposUiState.Error("Error en la búsqueda: código ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = ReposUiState.Error("Error de red: ${e.localizedMessage ?: "Verificá tu conexión."}")
            }
        }
    }

    fun createRepository(name: String, description: String, isPrivate: Boolean, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.createRepo(CreateRepoRequest(name = name, description = description, private = isPrivate))
                if (response.isSuccessful) {
                    loadRepositories()
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }
}
