package com.ejemplo.githubmovil.data.remote

import android.content.Context
import com.ejemplo.githubmovil.data.local.TokenManager
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private var apiServiceInstance: GithubApiService? = null

    fun getInstance(context: Context): GithubApiService {
        if (apiServiceInstance == null) {
            val tokenManager = TokenManager(context.applicationContext)
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(tokenManager))
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val contentType = "application/json".toMediaType()
            val json = Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            }

            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .client(client)
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()

            apiServiceInstance = retrofit.create(GithubApiService::class.java)
        }
        return apiServiceInstance!!
    }
}
