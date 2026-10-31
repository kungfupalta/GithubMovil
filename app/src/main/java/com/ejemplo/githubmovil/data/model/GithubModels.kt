package com.ejemplo.githubmovil.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubUser(
    val id: Long,
    val login: String,
    val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val email: String? = null
)

@Serializable
data class GitHubRepo(
    val id: Long,
    val name: String,
    @SerialName("full_name") val fullName: String,
    val description: String? = null,
    val private: Boolean,
    @SerialName("default_branch") val defaultBranch: String = "main",
    val owner: GitHubUser,
    @SerialName("stargazers_count") val stargazersCount: Int = 0,
    @SerialName("forks_count") val forksCount: Int = 0,
    val language: String? = null
)

@Serializable
data class CreateRepoRequest(
    val name: String,
    val description: String? = null,
    val private: Boolean = false,
    @SerialName("auto_init") val autoInit: Boolean = true
)

@Serializable
data class ContentItem(
    val name: String,
    val path: String,
    val sha: String,
    val size: Long,
    val type: String,
    @SerialName("download_url") val downloadUrl: String? = null
)
