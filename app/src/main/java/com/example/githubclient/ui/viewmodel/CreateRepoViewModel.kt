package com.example.githubclient.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.githubclient.data.GitHubRepository
import com.example.githubclient.data.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateRepoUiState(
    val name: String = "",
    val description: String = "",
    val isPrivate: Boolean = true,
    val autoInit: Boolean = true,
    val nameError: String? = null,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    /** Nombre completo (owner/repo) del repositorio recién creado. */
    val createdRepo: String? = null
)

class CreateRepoViewModel(private val repository: GitHubRepository) : ViewModel() {

    private val _state = MutableStateFlow(CreateRepoUiState())
    val state: StateFlow<CreateRepoUiState> = _state.asStateFlow()

    fun onNameChange(value: String) {
        _state.update { it.copy(name = value, nameError = null, error = null) }
    }

    fun onDescriptionChange(value: String) {
        _state.update { it.copy(description = value) }
    }

    fun onPrivateChange(value: Boolean) {
        _state.update { it.copy(isPrivate = value) }
    }

    fun onAutoInitChange(value: Boolean) {
        _state.update { it.copy(autoInit = value) }
    }

    fun submit() {
        val current = _state.value
        if (current.isSubmitting) return

        val name = current.name.trim()
        val nameError = validateName(name)
        if (nameError != null) {
            _state.update { it.copy(nameError = nameError) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            repository.createRepo(
                name = name,
                description = current.description.trim().ifEmpty { null },
                isPrivate = current.isPrivate,
                autoInit = current.autoInit
            ).fold(
                onSuccess = { repo ->
                    _state.update { it.copy(isSubmitting = false, createdRepo = repo.fullName) }
                },
                onFailure = { error ->
                    _state.update { it.copy(isSubmitting = false, error = error.toUserMessage()) }
                }
            )
        }
    }

    private fun validateName(name: String): String? = when {
        name.isEmpty() -> "Escribe un nombre para el repositorio."
        name.length > 100 -> "El nombre no puede superar los 100 caracteres."
        name == "." || name == ".." -> "Ese nombre no es válido."
        !NAME_REGEX.matches(name) -> "Usa solo letras, números, punto, guion y guion bajo."
        else -> null
    }

    companion object {
        private val NAME_REGEX = Regex("^[A-Za-z0-9._-]+$")

        fun factory(repository: GitHubRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { CreateRepoViewModel(repository) }
            }
    }
}
