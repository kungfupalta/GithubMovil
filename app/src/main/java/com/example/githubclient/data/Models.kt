package com.example.githubclient.data

import com.google.gson.annotations.SerializedName

data class User(
    val login: String,
    val name: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    @SerializedName("public_repos") val publicRepos: Int = 0,
    val followers: Int = 0,
    val following: Int = 0
)

data class Owner(
    val login: String
)

data class Repo(
    val id: Long,
    val name: String,
    @SerializedName("full_name") val fullName: String,
    val description: String? = null,
    @SerializedName("private") val isPrivate: Boolean = false,
    val language: String? = null,
    @SerializedName("stargazers_count") val stars: Int = 0,
    @SerializedName("forks_count") val forks: Int = 0,
    @SerializedName("default_branch") val defaultBranch: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("html_url") val htmlUrl: String? = null,
    val owner: Owner
)

/** Elemento de un directorio devuelto por la API de contenidos. */
data class ContentItem(
    val name: String,
    val path: String,
    val sha: String? = null,
    val size: Long = 0,
    /** "file", "dir", "symlink" o "submodule". */
    val type: String,
    @SerializedName("html_url") val htmlUrl: String? = null
)

/** Respuesta de la API al pedir un archivo concreto. */
data class FileContent(
    val name: String,
    val path: String,
    val size: Long = 0,
    val content: String? = null,
    val encoding: String? = null,
    @SerializedName("html_url") val htmlUrl: String? = null
)

data class CreateRepoRequest(
    val name: String,
    val description: String? = null,
    @SerializedName("private") val isPrivate: Boolean,
    @SerializedName("auto_init") val autoInit: Boolean
)

/** Archivo ya decodificado y listo para mostrarse en pantalla. */
data class LoadedFile(
    val name: String,
    val path: String,
    val size: Long,
    val htmlUrl: String?,
    /** Texto a mostrar; es null si el archivo es binario o demasiado grande. */
    val text: String?,
    /** Aviso para el usuario (archivo vacío, binario, truncado...). */
    val note: String?
)
