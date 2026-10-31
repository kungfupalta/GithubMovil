package com.example.githubclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.githubclient.ui.navigation.AppNavigation
import com.example.githubclient.ui.screens.LoadingView
import com.example.githubclient.ui.screens.LoginScreen
import com.example.githubclient.ui.theme.GitHubClientTheme
import com.example.githubclient.ui.viewmodel.AuthViewModel
import com.example.githubclient.ui.viewmodel.Session

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as GitHubApp).container

        setContent {
            GitHubClientTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authViewModel: AuthViewModel = viewModel(
                        factory = AuthViewModel.factory(container.tokenStore, container.repository)
                    )
                    val session by authViewModel.session.collectAsStateWithLifecycle()
                    val isLoggingIn by authViewModel.isLoggingIn.collectAsStateWithLifecycle()

                    when (val current = session) {
                        is Session.Checking -> LoadingView()
                        is Session.LoggedOut -> LoginScreen(
                            error = current.error,
                            isLoading = isLoggingIn,
                            canRetry = current.canRetry,
                            onLogin = authViewModel::login,
                            onRetry = authViewModel::retry
                        )
                        is Session.LoggedIn -> AppNavigation(
                            container = container,
                            user = current.user,
                            onLogout = authViewModel::logout
                        )
                    }
                }
            }
        }
    }
}
