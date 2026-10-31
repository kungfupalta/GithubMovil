package com.ejemplo.githubmovil.ui.repos

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoDetailScreen(
    owner: String,
    repoName: String,
    onBackClick: () -> Unit,
    onNavigateFiles: (String, String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(repoName) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Propietario: $owner",
                style = MaterialTheme.typography.bodyLarge
            )

            Button(
                onClick = { onNavigateFiles(owner, repoName) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Explorar Archivos y Código")
            }
        }
    }
}
