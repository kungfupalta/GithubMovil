@file:OptIn(ExperimentalMaterial3Api::class)

package app.ghmobile

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FileData(val meta: ContentItem, val bytes: ByteArray, val tooBig: Boolean)

private val IMAGE_EXT = setOf("png", "jpg", "jpeg", "gif", "webp", "bmp")

@Composable
fun FileScreen(nav: NavController, owner: String, name: String, path: String, ref: String) {
    val full = "$owner/$name"
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var tick by remember { mutableIntStateOf(0) }
    val l = rememberLoader(Triple(full, path, tick)) {
        val meta = Api.get<ContentItem>(Api.contentsPath(full, path, ref))
        if (meta.size > 5_000_000) {
            FileData(meta, ByteArray(0), true)
        } else {
            FileData(meta, Api.bytes(Api.contentsPath(full, path, ref), Api.RAW), false)
        }
    }
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var committing by remember { mutableStateOf(false) }
    val fname = path.substringAfterLast('/')

    if (committing) {
        CommitDialog("Guardar $fname", "Update $fname", "Guardar", { committing = false }) { msg ->
            committing = false
            scope.launch {
                try {
                    Api.putFile(full, path, draft.toByteArray(), msg, ref)
                    toast(ctx, "Commit creado")
                    editing = false
                    tick++
                } catch (e: Exception) {
                    toast(ctx, e.message)
                }
            }
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(fname, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = {
                IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") }
            },
            actions = {
                IconButton(onClick = {
                    scope.launch {
                        try {
                            Api.download(ctx, Api.contentsPath(full, path, ref), Api.RAW, fname)
                            toast(ctx, "Guardado en Descargas/GitHubMobile")
                        } catch (e: Exception) {
                            toast(ctx, e.message)
                        }
                    }
                }) { Icon(Icons.Default.CloudDownload, "Descargar") }
                val data = l.okOrNull()
                val isText = data != null && !data.tooBig && !data.bytes.take(8000).any { it.toInt() == 0 } &&
                    fname.substringAfterLast('.', "").lowercase() !in IMAGE_EXT
                if (isText) {
                    if (editing) {
                        IconButton(onClick = { committing = true }) { Icon(Icons.Default.Save, "Guardar") }
                    } else {
                        IconButton(onClick = {
                            draft = String(data!!.bytes)
                            editing = true
                        }) { Icon(Icons.Default.Edit, "Editar") }
                    }
                }
                IconButton(onClick = {
                    ctx.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/$full/blob/${Uri.encode(ref)}/$path"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }) { Icon(Icons.Default.OpenInBrowser, "Abrir en navegador") }
            },
        )
    }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            LoaderView(l) { data ->
                val ext = fname.substringAfterLast('.', "").lowercase()
                when {
                    data.tooBig -> Center("Archivo demasiado grande para previsualizar (${data.meta.size / 1024} KB). Usa Descargar.")
                    ext in IMAGE_EXT -> {
                        val bmp = remember(data) { BitmapFactory.decodeByteArray(data.bytes, 0, data.bytes.size) }
                        if (bmp == null) Center("No se pudo mostrar la imagen") else {
                            Image(bmp.asImageBitmap(), fname, Modifier.fillMaxSize().padding(8.dp), contentScale = ContentScale.Fit)
                        }
                    }
                    data.bytes.take(8000).any { it.toInt() == 0 } -> Center("Archivo binario (${data.meta.size / 1024} KB). Usa Descargar.")
                    editing -> OutlinedTextField(
                        draft, { draft = it },
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                    )
                    else -> {
                        val lines = remember(data) { String(data.bytes).split("\n") }
                        Box(Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
                            LazyColumn(Modifier.fillMaxHeight()) {
                                itemsIndexed(lines) { i, ln ->
                                    Row {
                                        Text(
                                            "${i + 1}",
                                            Modifier.width(52.dp).padding(end = 8.dp),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.End,
                                            color = MaterialTheme.colorScheme.outline,
                                        )
                                        Text(ln, fontFamily = FontFamily.Monospace, fontSize = 12.sp, softWrap = false)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Center(msg: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Text(msg) }
}

@Composable
fun RunScreen(nav: NavController, owner: String, name: String, id: String) {
    val full = "$owner/$name"
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val runL = rememberLoader(id) { Api.get<WfRun>("/repos/$full/actions/runs/$id") }
    val jobsL = rememberLoader(id) { Api.get<JobsResponse>("/repos/$full/actions/runs/$id/jobs").jobs }
    val artL = rememberLoader(id) { Api.get<ArtifactsResponse>("/repos/$full/actions/runs/$id/artifacts").artifacts }
    var logs by remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(runL) {
        while (true) {
            delay(8_000)
            val s = runL.okOrNull()
            if (s != null && s.status == "completed") break
            runL.load(silent = true)
            jobsL.load(silent = true)
            artL.load(silent = true)
        }
        // última pasada para traer artifacts ya publicados
        delay(2_000)
        artL.load(silent = true)
    }

    logs?.let { (title, text) ->
        AlertDialog(
            onDismissRequest = { logs = null },
            title = { Text(title) },
            text = {
                Box(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState())) {
                    Text(text, fontFamily = FontFamily.Monospace, fontSize = 11.sp, softWrap = false)
                }
            },
            confirmButton = { TextButton(onClick = { logs = null }) { Text("Cerrar") } },
        )
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Run #$id", maxLines = 1) },
            navigationIcon = {
                IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") }
            },
            actions = {
                IconButton(onClick = {
                    runL.reload(true); jobsL.reload(true); artL.reload(true)
                }) { Icon(Icons.Default.Refresh, "Recargar") }
            },
        )
    }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            LoaderView(runL) { run ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusIcon(run.status, run.conclusion)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(run.displayTitle ?: run.name ?: "Run", style = MaterialTheme.typography.titleMedium)
                            Text("${run.status} · ${run.conclusion ?: "—"} · ${run.headBranch.orEmpty()} · ${run.event}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            scope.launch {
                                try {
                                    Api.call("/repos/$full/actions/runs/$id/rerun", "POST")
                                    toast(ctx, "Re-ejecutando…")
                                    delay(2000)
                                    runL.reload(true); jobsL.reload(true)
                                } catch (e: Exception) {
                                    toast(ctx, e.message)
                                }
                            }
                        }) { Text("Re-ejecutar") }
                        if (run.status != "completed") {
                            OutlinedButton(onClick = {
                                scope.launch {
                                    try {
                                        Api.call("/repos/$full/actions/runs/$id/cancel", "POST")
                                        toast(ctx, "Cancelando…")
                                    } catch (e: Exception) {
                                        toast(ctx, e.message)
                                    }
                                }
                            }) { Text("Cancelar") }
                        }
                    }

                    Text("Artifacts", style = MaterialTheme.typography.titleMedium)
                    val arts = artL.okOrNull()
                    when {
                        arts == null -> LinearProgressIndicator(Modifier.fillMaxWidth())
                        arts.isEmpty() -> Text("Aún no hay artifacts", style = MaterialTheme.typography.bodySmall)
                        else -> arts.forEach { a ->
                            FilledTonalButton(
                                enabled = !a.expired,
                                onClick = {
                                    scope.launch {
                                        try {
                                            toast(ctx, "Descargando ${a.name}…")
                                            Api.download(ctx, "/repos/$full/actions/artifacts/${a.id}/zip", Api.ACCEPT, "${a.name}.zip", "application/zip")
                                            toast(ctx, "Guardado en Descargas/GitHubMobile (descomprime el ZIP para ver el APK)")
                                        } catch (e: Exception) {
                                            toast(ctx, e.message)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Default.CloudDownload, null)
                                Spacer(Modifier.width(8.dp))
                                Text("${a.name} (${a.size / 1024} KB)" + if (a.expired) " · expirado" else "")
                            }
                        }
                    }

                    Text("Jobs", style = MaterialTheme.typography.titleMedium)
                    val jobs = jobsL.okOrNull()
                    if (jobs == null) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    } else {
                        jobs.forEach { job ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        StatusIcon(job.status, job.conclusion)
                                        Spacer(Modifier.width(8.dp))
                                        Text(job.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                                        TextButton(onClick = {
                                            scope.launch {
                                                try {
                                                    val t = Api.call("/repos/$full/actions/jobs/${job.id}/logs")
                                                    logs = job.name to t.takeLast(30_000)
                                                } catch (e: Exception) {
                                                    toast(ctx, e.message)
                                                }
                                            }
                                        }) { Text("Ver logs") }
                                    }
                                    job.steps.forEach { st ->
                                        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            StatusIcon(st.status, st.conclusion)
                                            Spacer(Modifier.width(8.dp))
                                            Text(st.name, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IssueScreen(nav: NavController, owner: String, name: String, num: String, isPr: Boolean) {
    val full = "$owner/$name"
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val issueL = rememberLoader(num) { Api.get<Issue>("/repos/$full/issues/$num") }
    val comL = rememberLoader(num) { Api.get<List<IssueComment>>("/repos/$full/issues/$num/comments?per_page=100") }
    var text by remember { mutableStateOf("") }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (isPr) "PR #$num" else "Issue #$num") },
            navigationIcon = {
                IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") }
            },
        )
    }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            LoaderView(issueL) { i ->
                Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(i.title, style = MaterialTheme.typography.titleLarge)
                        Text("${i.state} · ${i.user?.login.orEmpty()} · ${i.createdAt.d()}", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                scope.launch {
                                    try {
                                        val ns = if (i.state == "open") "closed" else "open"
                                        Api.call("/repos/$full/issues/$num", "PATCH", Api.body("state" to ns))
                                        issueL.reload(true)
                                    } catch (e: Exception) {
                                        toast(ctx, e.message)
                                    }
                                }
                            }) { Text(if (i.state == "open") "Cerrar" else "Reabrir") }
                            if (isPr && i.state == "open") {
                                Button(onClick = {
                                    scope.launch {
                                        try {
                                            Api.call("/repos/$full/pulls/$num/merge", "PUT", Api.body("merge_method" to "merge"))
                                            toast(ctx, "PR fusionado")
                                            issueL.reload(true)
                                        } catch (e: Exception) {
                                            toast(ctx, e.message)
                                        }
                                    }
                                }) { Text("Merge") }
                            }
                        }
                        if (!i.body.isNullOrBlank()) Card(Modifier.fillMaxWidth()) { Text(i.body, Modifier.padding(12.dp)) }
                        Text("Comentarios", style = MaterialTheme.typography.titleMedium)
                        val cs = comL.okOrNull()
                        if (cs == null) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        } else {
                            cs.forEach { c ->
                                Card(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text("${c.user?.login.orEmpty()} · ${c.createdAt.d()}", style = MaterialTheme.typography.labelMedium)
                                        Text(c.body)
                                    }
                                }
                            }
                        }
                    }
                    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(text, { text = it }, placeholder = { Text("Escribe un comentario") }, modifier = Modifier.weight(1f), maxLines = 4)
                        IconButton(enabled = text.isNotBlank(), onClick = {
                            val t = text
                            text = ""
                            scope.launch {
                                try {
                                    Api.call("/repos/$full/issues/$num/comments", "POST", Api.body("body" to t))
                                    comL.reload(true)
                                } catch (e: Exception) {
                                    toast(ctx, e.message)
                                }
                            }
                        }) { Icon(Icons.Default.PlayArrow, "Enviar") }
                    }
                }
            }
        }
    }
}
