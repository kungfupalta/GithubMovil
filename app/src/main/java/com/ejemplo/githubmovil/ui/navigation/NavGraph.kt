package com.ejemplo.githubmovil.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ejemplo.githubmovil.data.local.TokenManager
import com.ejemplo.githubmovil.ui.login.LoginScreen
import com.ejemplo.githubmovil.ui.login.LoginViewModel
import com.ejemplo.githubmovil.ui.repos.*

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val tokenManager = TokenManager(context)
    val startDestination = if (tokenManager.getToken().isNullOrBlank()) "login" else "repos"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            val viewModel: LoginViewModel = viewModel()
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate("repos") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("repos") {
            val viewModel: ReposViewModel = viewModel()
            RepoListScreen(
                viewModel = viewModel,
                onRepoClick = { owner, repoName ->
                    navController.navigate("repo_detail/$owner/$repoName")
                },
                onCreateRepoClick = {
                    navController.navigate("create_repo")
                }
            )
        }

        composable(
            route = "repo_detail/{owner}/{repoName}",
            arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("repoName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val owner = backStackEntry.arguments?.getString("owner") ?: ""
            val repoName = backStackEntry.arguments?.getString("repoName") ?: ""
            RepoDetailScreen(
                owner = owner,
                repoName = repoName,
                onBackClick = { navController.popBackStack() },
                onNavigateFiles = { o, r -> navController.navigate("repo_files/$o/$r") }
            )
        }

        composable(
            route = "repo_files/{owner}/{repoName}",
            arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("repoName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val owner = backStackEntry.arguments?.getString("owner") ?: ""
            val repoName = backStackEntry.arguments?.getString("repoName") ?: ""
            RepoFilesScreen(
                owner = owner,
                repoName = repoName,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("create_repo") {
            val viewModel: ReposViewModel = viewModel()
            CreateRepoScreen(
                viewModel = viewModel,
                onRepoCreated = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
