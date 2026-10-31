package com.ejemplo.githubmovil.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TokenManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "github_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_GCM,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _tokenFlow = MutableStateFlow<String?>(getToken())
    val tokenFlow: StateFlow<String?> = _tokenFlow.asStateFlow()

    fun getToken(): String? {
        return sharedPreferences.getString("github_token", null)
    }

    fun saveToken(token: String) {
        sharedPreferences.edit().putString("github_token", token).apply()
        _tokenFlow.value = token
    }

    fun clearToken() {
        sharedPreferences.edit().remove("github_token").apply()
        _tokenFlow.value = null
    }
}
