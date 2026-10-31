package com.ejemplo.githubmovil.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ejemplo.githubmovil.data.local.TokenManager
import com.ejemplo.githubmovil.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenManager = TokenManager(application)
    private val api = RetrofitClient.getInstance(application)

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun loginWithToken(token: String) {
        if (token.isBlank()) {
            _state.value = LoginState.Error("El token no puede estar vacío.")
            return
        }

        viewModelScope.launch {
            _state.value = LoginState.Loading
            try {
                tokenManager.saveToken(token.trim())
                val response = api.getAuthenticatedUser()
                if (response.isSuccessful && response.body() != null) {
                    _state.value = LoginState.Success
                } else {
                    tokenManager.clearToken()
                    val errorMsg = when (response.code()) {
                        401 -> "Token inválido o expirado (401 Unauthorized)."
                        403 -> "Permisos insuficientes o límite de API excedido (403 Forbidden)."
                        else -> "Error al autenticar: código ${response.code()}"
                    }
                    _state.value = LoginState.Error(errorMsg)
                }
            } catch (e: Exception) {
                tokenManager.clearToken()
                _state.value = LoginState.Error("Error de conexión: ${e.localizedMessage ?: "Verificá tu red."}")
            }
        }
    }
}
