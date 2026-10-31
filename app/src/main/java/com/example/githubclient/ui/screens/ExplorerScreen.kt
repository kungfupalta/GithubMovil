package com.example.githubclient.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.githubclient.data.ContentItem
import com.example.githubclient.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(
    owner: String,
    repo: String,
    path: String,
    state: UiState<List<ContentItem>>,
    onBack: () -> Unit,
    onPopLevels: (Int) -> Unit,
    onOpenDir: (ContentItem) -> Unit,
    onOpenFile: (ContentItem) -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = repo,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = owner,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Breadcrumbs(repo = repo, path = path, onPopLevels = onPopLevels)
            HorizontalDivider()

            when (state) {
                is UiState.Loading -> LoadingView()
                is UiState.Error -> ErrorView(message = state.message, onRetry = onRetry)
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        EmptyView("Esta carpeta está vacía.")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.data, key = { it.path }) { item ->
                                ContentRow(
                                    item = item,
                                    onOpenDir = onOpenDir,
                                    onOpenFile = onOpenFile
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 72.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Breadcrumbs(repo: String, path: String, onPopLevels: (Int) -> Unit) {
    val segments = if (path.isEmpty()) emptyList() else path.split("/")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Crumb(
            label = repo,
            isCurrent = segments.isEmpty(),
            onClick = { onPopLevels(segments.size) }
        )
        segments.forEachIndexed { index, segment ->
            Text(
                text = "/",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Crumb(
                label = segment,
                isCurrent = index == segments.lastIndex,
                onClick = { onPopLevels(segments.size - (index + 1)) }
            )
        }
    }
}

@Composable
private fun Crumb(label: String, isCurrent: Boolean, onClick: () -> Unit) {
    if (isCurrent) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    } else {
        TextButton(onClick = onClick) {
            Text(label)
        }
    }
}

@Composable
private fun ContentRow(
    item: ContentItem,
    onOpenDir: (ContentItem) -> Unit,
    onOpenFile: (ContentItem) -> Unit
) {
    val isDir = item.type == "dir"
    val isSubmodule = item.type == "submodule"
    val icon = when {
        isDir -> Icons.Default.Folder
        isSubmodule -> Icons.Default.Link
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
    val tint = if (isDir) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val rowModifier = Modifier.clickable(enabled = !isSubmodule) {
        if (isDir) onOpenDir(item) else onOpenFile(item)
    }

    if (item.type == "file") {
        ListItem(
            modifier = rowModifier,
            leadingContent = { Icon(icon, contentDescription = null, tint = tint) },
            headlineContent = {
                Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            supportingContent = { Text(formatSize(item.size)) }
        )
    } else {
        ListItem(
            modifier = rowModifier,
            leadingContent = { Icon(icon, contentDescription = null, tint = tint) },
            headlineContent = {
                Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        )
    }
}
