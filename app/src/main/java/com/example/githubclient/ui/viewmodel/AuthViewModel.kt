package com.example.githubclient.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.githubclient.data.GitHubRepository
import com.example.githubclient.data.TokenStore
import com.example.githubclient.data.User
import com.example.githubclient.data.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed interface Session {
    data object Checking : Session
    data class LoggedOut(val error: String? = null, val canRetry: Boolean = false) : Session
    data class LoggedIn(val user: User) : Session
}

class AuthViewModel(
    private val tokenStore: TokenStore,
    private val repository: GitHubRepository
) : ViewModel() {

    private val _session = MutableStateFlow<Session>(Session.Checking)
    val session: StateFlow<Session> = _session.asStateFlow()

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn.asStateFlow()

    init {
        restoreSession()
    }

    private fun restoreSession() {
        if (!tokenStore.hasToken()) {
            _session.value = Session.LoggedOut()
            return
        }
        viewModelScope.launch {
            repository.getUser().fold(
                onSuccess = { user -> _session.value = Session.LoggedIn(user) },
                onFailure = { error ->
                    val invalid = error is HttpException && error.code() == 401
                    if (invalid) tokenStore.clearToken()
                    _session.value = Session.LoggedOut(
                        error = error.toUserMessage(),
                        canRetry = !invalid
                    )
                }
            )
        }
    }

    /** Reintenta validar el token guardado (por ejemplo, tras recuperar la conexión). */
    fun retry() {
        _session.value = Session.Checking
        restoreSession()
    }

    fun login(rawToken: String) {
        val token = rawToken.trim()
        if (token.isEmpty() || _isLoggingIn.value) return
        viewModelScope.launch {
            _isLoggingIn.value = true
            tokenStore.saveToken(token)
            repository.getUser().fold(
                onSuccess = { user -> _session.value = Session.LoggedIn(user) },
                onFailure = { error ->
                    tokenStore.clearToken()
                    _session.value = Session.LoggedOut(error = error.toUserMessage())
                }
            )
            _isLoggingIn.value = false
        }
    }

    fun logout() {
        tokenStore.clearToken()
        _session.value = Session.LoggedOut()
    }

    companion object {
        fun factory(tokenStore: TokenStore, repository: GitHubRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { AuthViewModel(tokenStore, repository) }
            }
    }
}
