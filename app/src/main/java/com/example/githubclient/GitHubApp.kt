package com.example.githubclient

import android.app.Application
import com.example.githubclient.data.AppContainer

class GitHubApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
