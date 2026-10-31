package com.example.githubclient.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.githubclient.data.AppContainer
import com.example.githubclient.data.User
import com.example.githubclient.ui.screens.CreateRepoScreen
import com.example.githubclient.ui.screens.ExplorerScreen
import com.example.githubclient.ui.screens.FileViewerScreen
import com.example.githubclient.ui.screens.ReposScreen
import com.example.githubclient.ui.viewmodel.CreateRepoViewModel
import com.example.githubclient.ui.viewmodel.ExplorerViewModel
import com.example.githubclient.ui.viewmodel.FileViewerViewModel
import com.example.githubclient.ui.viewmodel.ReposViewModel

object Routes {
    const val REPOS = "repos"
    const val CREATE = "create"
    const val EXPLORER = "explorer/{owner}/{repo}?path={path}"
    const val FILE = "file/{owner}/{repo}?path={path}"

    const val ARG_OWNER = "owner"
    const val ARG_REPO = "repo"
    const val ARG_PATH = "path"

    /** Clave del resultado que "Nuevo repositorio" deja para la pantalla de repositorios. */
    const val KEY_CREATED = "repo_created"

    fun explorer(owner: String, repo: String, path: String = ""): String {
        val base = "explorer/${Uri.encode(owner)}/${Uri.encode(repo)}"
        return if (path.isEmpty()) base else "$base?path=${Uri.encode(path)}"
    }

    fun file(owner: String, repo: String, path: String): String {
        return "file/${Uri.encode(owner)}/${Uri.encode(repo)}?path=${Uri.encode(path)}"
    }
}

private val repoArguments = listOf(
    navArgument(Routes.ARG_OWNER) { type = NavType.StringType },
    navArgument(Routes.ARG_REPO) { type = NavType.StringType },
    navArgument(Routes.ARG_PATH) {
        type = NavType.StringType
        defaultValue = ""
    }
)

@Composable
fun AppNavigation(
    container: AppContainer,
    user: User,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.REPOS) {

        composable(Routes.REPOS) { entry ->
            val viewModel: ReposViewModel = viewModel(
                factory = ReposViewModel.factory(container.repository)
            )
            val state by viewModel.state.collectAsStateWithLifecycle()
            val created by entry.savedStateHandle
                .getStateFlow<String?>(Routes.KEY_CREATED, null)
                .collectAsStateWithLifecycle()

            ReposScreen(
                user = user,
                state = state,
                createdRepoName = created,
                onCreatedConsumed = { entry.savedStateHandle.set<String?>(Routes.KEY_CREATED, null) },
                onQueryChange = viewModel::onQueryChange,
                onRefresh = viewModel::refresh,
                onRepoClick = { repo ->
                    navController.navigate(Routes.explorer(repo.owner.login, repo.name))
                },
                onCreateClick = { navController.navigate(Routes.CREATE) },
                onLogout = onLogout
            )
        }

        composable(Routes.CREATE) {
            val viewModel: CreateRepoViewModel = viewModel(
                factory = CreateRepoViewModel.factory(container.repository)
            )
            val state by viewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(state.createdRepo) {
                val name = state.createdRepo
                if (name != null) {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(Routes.KEY_CREATED, name)
                    navController.popBackStack()
                }
            }

            CreateRepoScreen(
                state = state,
                onNameChange = viewModel::onNameChange,
                onDescriptionChange = viewModel::onDescriptionChange,
                onPrivateChange = viewModel::onPrivateChange,
                onAutoInitChange = viewModel::onAutoInitChange,
                onSubmit = viewModel::submit,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.EXPLORER, arguments = repoArguments) { entry ->
            val owner = entry.arguments?.getString(Routes.ARG_OWNER).orEmpty()
            val repo = entry.arguments?.getString(Routes.ARG_REPO).orEmpty()
            val path = entry.arguments?.getString(Routes.ARG_PATH).orEmpty()

            val viewModel: ExplorerViewModel = viewModel(
                factory = ExplorerViewModel.factory(container.repository, owner, repo, path)
            )
            val state by viewModel.state.collectAsStateWithLifecycle()

            ExplorerScreen(
                owner = owner,
                repo = repo,
                path = path,
                state = state,
                onBack = { navController.popBackStack() },
                onPopLevels = { levels -> repeat(levels) { navController.popBackStack() } },
                onOpenDir = { item ->
                    navController.navigate(Routes.explorer(owner, repo, item.path))
                },
                onOpenFile = { item ->
                    navController.navigate(Routes.file(owner, repo, item.path))
                },
                onRetry = viewModel::load
            )
        }

        composable(Routes.FILE, arguments = repoArguments) { entry ->
            val owner = entry.arguments?.getString(Routes.ARG_OWNER).orEmpty()
            val repo = entry.arguments?.getString(Routes.ARG_REPO).orEmpty()
            val path = entry.arguments?.getString(Routes.ARG_PATH).orEmpty()

            val viewModel: FileViewerViewModel = viewModel(
                factory = FileViewerViewModel.factory(container.repository, owner, repo, path)
            )
            val state by viewModel.state.collectAsStateWithLifecycle()

            FileViewerScreen(
                path = path,
                state = state,
                onBack = { navController.popBackStack() },
                onRetry = viewModel::load
            )
        }
    }
}
