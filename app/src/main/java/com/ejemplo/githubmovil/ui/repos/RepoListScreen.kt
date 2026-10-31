package com.ejemplo.githubmovil.ui.repos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ejemplo.githubmovil.data.model.GitHubRepo
import com.ejemplo.githubmovil.ui.components.ErrorView
import com.ejemplo.githubmovil.ui.components.LoadingView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoListScreen(
    viewModel: ReposViewModel,
    onRepoClick: (String, String) -> Unit,
    onCreateRepoClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Repositorios") },
                actions = {
                    IconButton(onClick = onCreateRepoClick) {
                        Icon(Icons.Default.Add, contentDescription = "Crear Repositorio")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateRepoClick) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Repositorio")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchRepositories(it) },
                label = { Text("Buscar repositorios...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            when (val currentState = state) {
                is ReposUiState.Loading -> LoadingView("Cargando repositorios...")
                is ReposUiState.Error -> ErrorView(currentState.message) { viewModel.loadRepositories() }
                is ReposUiState.Success -> {
                    if (currentState.repos.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No se encontraron repositorios.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(currentState.repos) { repo ->
                                RepoItemCard(repo = repo, onClick = { onRepoClick(repo.owner.login, repo.name) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RepoItemCard(repo: GitHubRepo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = repo.fullName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (!repo.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = repo.description!!,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (repo.private) "Privado" else "Público",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                if (!repo.language.isNullOrBlank()) {
                    Text(
                        text = repo.language!!,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
