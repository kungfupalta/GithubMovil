package com.example.githubclient.data

import com.google.gson.JsonParser
import retrofit2.HttpException
import java.io.IOException

/** Convierte cualquier excepción en un mensaje legible para el usuario. */
fun Throwable.toUserMessage(): String = when (this) {
    is HttpException -> httpMessage()
    is IOException -> "No se pudo conectar con GitHub. Revisa tu conexión e inténtalo de nuevo."
    else -> localizedMessage ?: "Ocurrió un error inesperado."
}

private fun HttpException.httpMessage(): String {
    val detail = apiDetail()
    return when (code()) {
        401 -> "Token inválido o expirado. Genera uno nuevo en GitHub."
        403 -> "Acceso denegado o límite de peticiones alcanzado." +
            (detail?.let { " ($it)" } ?: "")
        404 -> "No encontrado. Verifica que el token tenga permisos sobre este recurso."
        409 -> "Conflicto: ${detail ?: "el repositorio está vacío."}"
        422 -> if (detail != null && detail.contains("already exists", ignoreCase = true)) {
            "Ya existe un repositorio con ese nombre en tu cuenta."
        } else {
            "GitHub rechazó los datos enviados" + (detail?.let { ": $it" } ?: ".")
        }
        in 500..599 -> "GitHub no está disponible en este momento (${code()})."
        else -> "Error ${code()}" + (detail?.let { ": $it" } ?: "")
    }
}

/** Extrae el mensaje de error que devuelve la API de GitHub, si existe. */
private fun HttpException.apiDetail(): String? = try {
    val body = response()?.errorBody()?.string().orEmpty()
    val json = JsonParser.parseString(body).asJsonObject
    val first = json.getAsJsonArray("errors")?.firstOrNull()
    val fromErrors = if (first != null && first.isJsonObject) {
        first.asJsonObject.get("message")?.asString
    } else {
        null
    }
    fromErrors ?: json.get("message")?.asString
} catch (e: Exception) {
    null
}
