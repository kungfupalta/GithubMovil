package com.ejemplo.githubmovil.ui.repos

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ejemplo.githubmovil.data.model.ContentItem
import com.ejemplo.githubmovil.data.remote.GithubApiService
import com.ejemplo.githubmovil.data.remote.RetrofitClient
import com.ejemplo.githubmovil.ui.components.ErrorView
import com.ejemplo.githubmovil.ui.components.LoadingView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FilesUiState {
    object Loading : FilesUiState()
    data class Success(val items: List<ContentItem>) : FilesUiState()
    data class Error(val message: String) : FilesUiState()
}

class FilesViewModel(application: Application) : AndroidViewModel(application) {
    private val api: GithubApiService = RetrofitClient.getInstance(application)

    private val _uiState = MutableStateFlow<FilesUiState>(FilesUiState.Loading)
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()

    fun loadContents(owner: String, repo: String, path: String) {
        viewModelScope.launch {
            _uiState.value = FilesUiState.Loading
            try {
                val response = api.getContents(owner, repo, path)
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = FilesUiState.Success(response.body()!!)
                } else {
                    _uiState.value = FilesUiState.Error("Error al cargar archivos: código ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = FilesUiState.Error("Error de red: ${e.localizedMessage ?: "Verificá tu conexión."}")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoFilesScreen(
    owner: String,
    repoName: String,
    viewModel: FilesViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBackClick: () -> Unit
) {
    var currentPath by remember { mutableStateOf("") }
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(currentPath) {
        viewModel.loadContents(owner, repoName, currentPath)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (currentPath.isEmpty()) repoName else "/$currentPath") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentPath.isEmpty()) {
                            onBackClick()
                        } else {
                            val lastSlash = currentPath.lastIndexOf('/')
                            currentPath = if (lastSlash == -1) "" else currentPath.substring(0, lastSlash)
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (val currentState = state) {
                is FilesUiState.Loading -> LoadingView("Cargando archivos...")
                is FilesUiState.Error -> ErrorView(currentState.message) { viewModel.loadContents(owner, repoName, currentPath) }
                is FilesUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(currentState.items) { item ->
                            ListItem(
                                headlineContent = { Text(item.name) },
                                leadingContent = {
                                    Icon(
                                        imageVector = if (item.type == "dir") Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier.clickable {
                                    if (item.type == "dir") {
                                        currentPath = item.path
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
