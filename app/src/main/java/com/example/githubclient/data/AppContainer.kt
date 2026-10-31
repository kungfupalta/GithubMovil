package com.example.githubclient.data

import android.content.Context

/** Contenedor sencillo de dependencias compartidas por toda la app. */
class AppContainer(context: Context) {
    val tokenStore: TokenStore = TokenStore(context.applicationContext)
    private val api: GitHubApi = ApiClient.create { tokenStore.getToken() }
    val repository: GitHubRepository = GitHubRepository(api)
}
