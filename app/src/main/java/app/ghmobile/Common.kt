@file:OptIn(ExperimentalMaterial3Api::class)

package app.ghmobile

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun GhTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Ok<out T>(val v: T) : UiState<T>
    data class Err(val msg: String) : UiState<Nothing>
}

class Loader<T>(private val scope: CoroutineScope, private val fn: suspend () -> T) {
    var state by mutableStateOf<UiState<T>>(UiState.Loading)
        private set

    suspend fun load(silent: Boolean = false) {
        if (!silent) state = UiState.Loading
        state = try {
            UiState.Ok(fn())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (silent && state is UiState.Ok) state else UiState.Err(e.message ?: "Error")
        }
    }

    fun reload(silent: Boolean = false) {
        scope.launch { load(silent) }
    }

    fun okOrNull(): T? = (state as? UiState.Ok<T>)?.v
    fun errOrNull(): String? = (state as? UiState.Err)?.msg
}

@Composable
fun <T> rememberLoader(key: Any?, fn: suspend () -> T): Loader<T> {
    val scope = rememberCoroutineScope()
    val l = remember(key) { Loader(scope, fn) }
    LaunchedEffect(l) { l.load() }
    return l
}

@Composable
fun Loading(modifier: Modifier = Modifier.fillMaxSize()) {
    Box(modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun ErrorView(msg: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(msg, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}

@Composable
fun <T> LoaderView(l: Loader<T>, content: @Composable (T) -> Unit) {
    when (val s = l.state) {
        is UiState.Loading -> Loading()
        is UiState.Err -> ErrorView(s.msg) { l.reload() }
        is UiState.Ok -> content(s.v)
    }
}

fun toast(c: Context, m: String?) {
    Toast.makeText(c, m ?: "Error", Toast.LENGTH_LONG).show()
}

fun String?.d(): String = this?.take(10).orEmpty()

@Composable
fun Avatar(url: String?, size: Dp = 40.dp) {
    AsyncImage(
        model = url,
        contentDescription = null,
        modifier = Modifier.size(size).clip(CircleShape),
    )
}

@Composable
fun StatusIcon(status: String?, conclusion: String?) {
    val (icon, color) = when {
        status != "completed" -> Icons.Default.Schedule to Color(0xFFD29922)
        conclusion == "success" -> Icons.Default.CheckCircle to Color(0xFF2EA043)
        conclusion == "failure" || conclusion == "timed_out" -> Icons.Default.Cancel to Color(0xFFF85149)
        else -> Icons.Default.Block to Color(0xFF8B949E)
    }
    Icon(icon, contentDescription = conclusion ?: status, tint = color)
}

@Composable
fun RepoCard(r: Repo, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp).clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    r.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (r.isPrivate) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.Lock, null, Modifier.size(16.dp))
                }
            }
            if (!r.description.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(r.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                listOfNotNull(
                    r.language,
                    "★ ${r.stars}",
                    "forks ${r.forks}",
                    if (r.fork) "fork" else null,
                    r.updatedAt?.let { "act. ${it.d()}" },
                ).joinToString("  ·  "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun CommitDialog(
    title: String,
    defaultMsg: String,
    confirm: String = "Commit",
    onDismiss: () -> Unit,
    onOk: (String) -> Unit,
) {
    var msg by remember { mutableStateOf(defaultMsg) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(msg, { msg = it }, label = { Text("Mensaje del commit") })
        },
        confirmButton = { TextButton(onClick = { onOk(msg.ifBlank { defaultMsg }) }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
