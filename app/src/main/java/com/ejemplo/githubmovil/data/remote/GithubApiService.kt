package com.ejemplo.githubmovil.data.remote

import com.ejemplo.githubmovil.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface GithubApiService {
    @GET("user")
    suspend fun getAuthenticatedUser(): Response<GitHubUser>

    @GET("user/repos")
    suspend fun getUserRepos(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int = 100,
        @Query("sort") sort: String = "updated"
    ): Response<List<GitHubRepo>>

    @GET("search/repositories")
    suspend fun searchRepos(
        @Query("q") query: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int = 30
    ): Response<RepoSearchResponse>

    @POST("user/repos")
    suspend fun createRepo(@Body request: CreateRepoRequest): Response<GitHubRepo>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String
    ): Response<List<ContentItem>>
}

@kotlinx.serialization.Serializable
data class RepoSearchResponse(
    @kotlinx.serialization.SerialName("total_count") val totalCount: Int,
    @kotlinx.serialization.SerialName("items") val items: List<GitHubRepo>
)
