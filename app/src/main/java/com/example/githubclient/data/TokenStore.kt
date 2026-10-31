package com.example.githubclient.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Guarda el Personal Access Token cifrado con EncryptedSharedPreferences
 * (clave maestra AES-256 respaldada por Android Keystore).
 */
class TokenStore(private val context: Context) {

    private val prefs: SharedPreferences = openPrefs()

    @Volatile
    private var cachedToken: String? = prefs.getString(KEY_TOKEN, null)

    fun getToken(): String? = cachedToken

    fun hasToken(): Boolean = !cachedToken.isNullOrBlank()

    fun saveToken(token: String) {
        cachedToken = token
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun clearToken() {
        cachedToken = null
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    private fun openPrefs(): SharedPreferences {
        return try {
            createEncryptedPrefs()
        } catch (e: Exception) {
            // El archivo cifrado quedó inutilizable (por ejemplo, la clave del Keystore
            // se perdió). Se descarta y se crea de nuevo; el usuario deberá volver a iniciar sesión.
            context.deleteSharedPreferences(FILE_NAME)
            createEncryptedPrefs()
        }
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private companion object {
        const val FILE_NAME = "secure_github_prefs"
        const val KEY_TOKEN = "personal_access_token"
    }
}
