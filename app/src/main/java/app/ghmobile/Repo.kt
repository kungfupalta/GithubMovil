@file:OptIn(ExperimentalMaterial3Api::class)

package app.ghmobile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun queryName(ctx: Context, uri: Uri): String =
    ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
        val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (i >= 0 && c.moveToFirst()) c.getString(i) else null
    } ?: "archivo"

@Composable
fun RepoScreen(nav: NavController, owner: String, name: String) {
    val full = "$owner/$name"
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val repoL = rememberLoader(full) { Api.get<Repo>("/repos/$full") }
    var tab by rememberSaveable { mutableStateOf(0) }
    var branch by rememberSaveable { mutableStateOf("") }
    var starred by remember { mutableStateOf(false) }
    LaunchedEffect(full) {
        starred = try {
            Api.call("/user/starred/$full"); true
        } catch (e: Exception) {
            false
        }
    }
    val tabs = listOf("Código", "Issues", "Pull requests", "Actions", "Commits", "Releases")

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = {
                IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") }
            },
            actions = {
                IconButton(onClick = {
                    scope.launch {
                        try {
                            if (starred) Api.call("/user/starred/$full", "DELETE") else Api.call("/user/starred/$full", "PUT")
                            starred = !starred
                        } catch (e: Exception) {
                            toast(ctx, e.message)
                        }
                    }
                }) { Icon(if (starred) Icons.Default.Star else Icons.Default.StarBorder, "Destacar") }
                IconButton(onClick = {
                    scope.launch {
                        try {
                            Api.call("/repos/$full/forks", "POST")
                            toast(ctx, "Fork en proceso; aparecerá en tus repos")
                        } catch (e: Exception) {
                            toast(ctx, e.message)
                        }
                    }
                }) { Icon(Icons.Default.CloudUpload, "Fork") }
                IconButton(onClick = {
                    ctx.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/$full")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }) { Icon(Icons.Default.OpenInBrowser, "Abrir en navegador") }
            },
        )
    }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            LoaderView(repoL) { repo ->
                val ref = branch.ifEmpty { repo.defaultBranch }
                Column {
                    ScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp) {
                        tabs.forEachIndexed { i, t ->
                            Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
                        }
                    }
                    when (tab) {
                        0 -> CodeTab(nav, repo, ref) { branch = it }
                        1 -> IssuesTab(nav, repo, false)
                        2 -> IssuesTab(nav, repo, true)
                        3 -> ActionsTab(nav, repo, ref)
                        4 -> CommitsTab(repo, ref) { branch = it }
                        else -> ReleasesTab(repo)
                    }
                }
            }
        }
    }
}

@Composable
fun BranchPicker(full: String, ref: String, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val l = rememberLoader(full) { Api.get<List<BranchItem>>("/repos/$full/branches?per_page=100") }
    Box {
        AssistChip(onClick = { open = true }, label = { Text(ref) }, leadingIcon = { Icon(Icons.Default.Code, null) })
        DropdownMenu(open, { open = false }) {
            l.okOrNull()?.forEach { b ->
                DropdownMenuItem(text = { Text(b.name) }, onClick = { open = false; onPick(b.name) })
            }
        }
    }
}

@Composable
fun CodeTab(nav: NavController, repo: Repo, ref: String, onRef: (String) -> Unit) {
    val full = repo.fullName
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var path by rememberSaveable { mutableStateOf("") }
    var tick by remember { mutableIntStateOf(0) }
    val l = rememberLoader(Triple(path, ref, tick)) {
        Api.contents(full, path, ref).sortedWith(compareBy({ it.type != "dir" }, { it.name.lowercase() }))
    }
    BackHandler(path.isNotEmpty()) { path = path.substringBeforeLast('/', "") }

    var pending by remember { mutableStateOf<Pair<Uri, String>?>(null) }
    var newFile by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ContentItem?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pending = uri to queryName(ctx, uri)
    }
    fun prefix() = if (path.isEmpty()) "" else "$path/"

    pending?.let { (uri, fname) ->
        CommitDialog("Subir $fname", "Add $fname", "Subir", { pending = null }) { msg ->
            pending = null
            scope.launch {
                try {
                    toast(ctx, "Subiendo…")
                    val data = withContext(Dispatchers.IO) { ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes() } }
                    Api.putFile(full, prefix() + fname, data, msg, ref)
                    toast(ctx, "Archivo subido")
                    tick++
                } catch (e: Exception) {
                    toast(ctx, e.message)
                }
            }
        }
    }
    deleting?.let { item ->
        CommitDialog("Eliminar ${item.name}", "Delete ${item.name}", "Eliminar", { deleting = null }) { msg ->
            deleting = null
            scope.launch {
                try {
                    Api.deleteFile(full, item.path, item.sha, msg, ref)
                    tick++
                } catch (e: Exception) {
                    toast(ctx, e.message)
                }
            }
        }
    }
    if (newFile) {
        var fname by remember { mutableStateOf("") }
        var text by remember { mutableStateOf("") }
        var msg by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { newFile = false },
            title = { Text("Nuevo archivo en /${path}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(fname, { fname = it }, label = { Text("Nombre (puede incluir carpetas)") }, singleLine = true)
                    OutlinedTextField(text, { text = it }, label = { Text("Contenido") }, minLines = 4, maxLines = 8)
                    OutlinedTextField(msg, { msg = it }, label = { Text("Mensaje del commit") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(enabled = fname.isNotBlank(), onClick = {
                    newFile = false
                    scope.launch {
                        try {
                            Api.putFile(full, prefix() + fname.trim(), text.toByteArray(), msg.ifBlank { "Create ${fname.trim()}" }, ref)
                            tick++
                        } catch (e: Exception) {
                            toast(ctx, e.message)
                        }
                    }
                }) { Text("Crear") }
            },
            dismissButton = { TextButton(onClick = { newFile = false }) { Text("Cancelar") } },
        )
    }

    Column {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            BranchPicker(full, ref, onRef)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { picker.launch(arrayOf("*/*")) }) { Icon(Icons.Default.CloudUpload, "Subir archivo") }
            IconButton(onClick = { newFile = true }) { Icon(Icons.Default.Add, "Nuevo archivo") }
            IconButton(onClick = {
                scope.launch {
                    try {
                        toast(ctx, "Descargando ZIP…")
                        Api.download(ctx, "/repos/$full/zipball/${Uri.encode(ref)}", Api.ACCEPT, "${repo.name}-$ref.zip", "application/zip")
                        toast(ctx, "Guardado en Descargas/GitHubMobile")
                    } catch (e: Exception) {
                        toast(ctx, e.message)
                    }
                }
            }) { Icon(Icons.Default.CloudDownload, "Descargar ZIP") }
        }
        Text("/$path", Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.labelLarge)
        LoaderView(l) { entries ->
            val readme = if (path.isEmpty()) entries.firstOrNull { it.name.startsWith("README", true) && it.type == "file" } else null
            LazyColumn {
                if (path.isNotEmpty()) {
                    item {
                        ListItem(
                            headlineContent = { Text("..") },
                            leadingContent = { Icon(Icons.Default.Folder, null) },
                            modifier = Modifier.clickable { path = path.substringBeforeLast('/', "") },
                        )
                    }
                }
                items(entries, key = { it.sha + it.path }) { it ->
                    var menu by remember { mutableStateOf(false) }
                    ListItem(
                        headlineContent = { Text(it.name) },
                        supportingContent = if (it.type == "file") ({ Text("${it.size / 1024} KB") }) else null,
                        leadingContent = { Icon(if (it.type == "dir") Icons.Default.Folder else Icons.Default.Description, null) },
                        trailingContent = if (it.type == "file") ({
                            Box {
                                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Más") }
                                DropdownMenu(menu, { menu = false }) {
                                    DropdownMenuItem(text = { Text("Descargar") }, onClick = {
                                        menu = false
                                        scope.launch {
                                            try {
                                                Api.download(ctx, Api.contentsPath(full, it.path, ref), Api.RAW, it.name)
                                                toast(ctx, "Guardado en Descargas/GitHubMobile")
                                            } catch (e: Exception) {
                                                toast(ctx, e.message)
                                            }
                                        }
                                    })
                                    DropdownMenuItem(text = { Text("Eliminar") }, onClick = { menu = false; deleting = it })
                                }
                            }
                        }) else null,
                        modifier = Modifier.clickable {
                            if (it.type == "dir") path = it.path else nav.navigate(fileRoute(full, it.path, ref))
                        },
                    )
                }
                if (readme != null) item { ReadmeCard(full, readme.path, ref) }
            }
        }
    }
}

@Composable
fun ReadmeCard(full: String, path: String, ref: String) {
    val l = rememberLoader(path) { String(Api.bytes(Api.contentsPath(full, path, ref), Api.RAW)) }
    val t = l.okOrNull()
    if (t != null) {
        Card(Modifier.fillMaxWidth().padding(12.dp)) {
            Column(Modifier.padding(14.dp)) {
                Text("README", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Text(t.take(6000), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun NewItemDialog(isPr: Boolean, defaultHead: String, defaultBase: String, onDismiss: () -> Unit, onOk: (String, String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var head by remember { mutableStateOf(defaultHead) }
    var base by remember { mutableStateOf(defaultBase) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isPr) "Nuevo pull request" else "Nuevo issue") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Título") }, singleLine = true)
                OutlinedTextField(body, { body = it }, label = { Text("Descripción") }, minLines = 3, maxLines = 6)
                if (isPr) {
                    OutlinedTextField(head, { head = it }, label = { Text("Rama origen (head)") }, singleLine = true)
                    OutlinedTextField(base, { base = it }, label = { Text("Rama destino (base)") }, singleLine = true)
                }
            }
        },
        confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = { onOk(title, body, head, base) }) { Text("Crear") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
fun IssuesTab(nav: NavController, repo: Repo, pulls: Boolean) {
    val full = repo.fullName
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by rememberSaveable { mutableStateOf("open") }
    var tick by remember { mutableIntStateOf(0) }
    var creating by remember { mutableStateOf(false) }
    val l = rememberLoader(Triple(state, tick, pulls)) {
        if (pulls) {
            Api.get<List<Issue>>("/repos/$full/pulls?state=$state&per_page=50")
        } else {
            Api.get<List<Issue>>("/repos/$full/issues?state=$state&per_page=50").filter { it.pullRequest == null }
        }
    }
    if (creating) {
        NewItemDialog(pulls, repo.defaultBranch, repo.defaultBranch, { creating = false }) { title, body, head, base ->
            creating = false
            scope.launch {
                try {
                    if (pulls) {
                        Api.call("/repos/$full/pulls", "POST", Api.body("title" to title, "body" to body, "head" to head, "base" to base))
                    } else {
                        Api.call("/repos/$full/issues", "POST", Api.body("title" to title, "body" to body))
                    }
                    tick++
                } catch (e: Exception) {
                    toast(ctx, e.message)
                }
            }
        }
    }
    Column {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(state == "open", { state = "open" }, label = { Text("Abiertos") })
            FilterChip(state == "closed", { state = "closed" }, label = { Text("Cerrados") })
            Spacer(Modifier.weight(1f))
            Button(onClick = { creating = true }) { Text("Nuevo") }
        }
        LoaderView(l) { list ->
            if (list.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nada por aquí") }
            } else {
                LazyColumn {
                    items(list, key = { it.number }) { i ->
                        val tag = when {
                            i.mergedAt != null -> "merged"
                            else -> i.state
                        }
                        ListItem(
                            headlineContent = { Text("#${i.number}  ${i.title}") },
                            supportingContent = { Text("$tag · ${i.user?.login.orEmpty()} · ${i.createdAt.d()} · ${i.comments} com.") },
                            modifier = Modifier.clickable { nav.navigate("issue/$full/${i.number}?pr=${if (pulls) 1 else 0}") },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun ActionsTab(nav: NavController, repo: Repo, ref: String) {
    val full = repo.fullName
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var tick by remember { mutableIntStateOf(0) }
    val wfL = rememberLoader(tick) { Api.get<WorkflowsResponse>("/repos/$full/actions/workflows").workflows }
    val runL = rememberLoader(tick) { Api.get<RunsResponse>("/repos/$full/actions/runs?per_page=30").runs }
    LaunchedEffect(runL) {
        while (true) {
            delay(10_000)
            runL.load(silent = true)
        }
    }
    var dispatching by remember { mutableStateOf<Workflow?>(null) }

    dispatching?.let { wf ->
        var r by remember { mutableStateOf(ref) }
        var inputs by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { dispatching = null },
            title = { Text("Ejecutar ${wf.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(r, { r = it }, label = { Text("Rama o tag") }, singleLine = true)
                    OutlinedTextField(inputs, { inputs = it }, label = { Text("Inputs (clave=valor, uno por línea)") }, minLines = 2, maxLines = 5)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    dispatching = null
                    val inObj = buildJsonObject {
                        inputs.lines().forEach { line ->
                            val k = line.substringBefore('=', "").trim()
                            if (k.isNotEmpty()) put(k, line.substringAfter('=').trim())
                        }
                    }
                    scope.launch {
                        try {
                            Api.call(
                                "/repos/$full/actions/workflows/${wf.id}/dispatches", "POST",
                                Api.body("ref" to r, "inputs" to if (inObj.isEmpty()) null else inObj),
                            )
                            toast(ctx, "Workflow disparado")
                            delay(3000)
                            runL.reload(silent = true)
                        } catch (e: Exception) {
                            toast(ctx, e.message)
                        }
                    }
                }) { Text("Ejecutar") }
            },
            dismissButton = { TextButton(onClick = { dispatching = null }) { Text("Cancelar") } },
        )
    }

    val wfs = wfL.okOrNull()
    val runs = runL.okOrNull()
    if (runs == null || wfs == null) {
        val err = runL.errOrNull() ?: wfL.errOrNull()
        if (err != null) ErrorView(err) { tick++ } else Loading()
    } else {
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Workflows", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { tick++ }) { Icon(Icons.Default.Refresh, "Recargar") }
                }
            }
            items(wfs, key = { it.id }) { wf ->
                ListItem(
                    headlineContent = { Text(wf.name) },
                    supportingContent = { Text(wf.path) },
                    trailingContent = {
                        FilledTonalButton(onClick = { dispatching = wf }) {
                            Icon(Icons.Default.PlayArrow, null)
                            Text("Ejecutar")
                        }
                    },
                )
            }
            item {
                Text("Ejecuciones recientes", Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp), style = MaterialTheme.typography.titleMedium)
            }
            items(runs, key = { it.id }) { r ->
                ListItem(
                    headlineContent = { Text(r.displayTitle ?: r.name ?: "Run ${r.id}", maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    supportingContent = { Text("#${r.runNumber} · ${r.headBranch.orEmpty()} · ${r.event} · ${r.createdAt.d()}") },
                    leadingContent = { StatusIcon(r.status, r.conclusion) },
                    modifier = Modifier.clickable { nav.navigate("run/$full/${r.id}") },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun CommitsTab(repo: Repo, ref: String, onRef: (String) -> Unit) {
    val full = repo.fullName
    val l = rememberLoader(ref) { Api.get<List<CommitItem>>("/repos/$full/commits?sha=${Uri.encode(ref)}&per_page=50") }
    Column {
        Row(Modifier.padding(horizontal = 12.dp)) { BranchPicker(full, ref, onRef) }
        LoaderView(l) { list ->
            LazyColumn {
                items(list, key = { it.sha }) { c ->
                    ListItem(
                        headlineContent = { Text(c.commit.message.lineSequence().first(), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text("${c.sha.take(7)} · ${c.commit.author?.name.orEmpty()} · ${c.commit.author?.date.d()}") },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun ReleasesTab(repo: Repo) {
    val full = repo.fullName
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val l = rememberLoader(full) { Api.get<List<ReleaseItem>>("/repos/$full/releases?per_page=20") }
    LoaderView(l) { list ->
        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Sin releases") }
        } else {
            LazyColumn {
                items(list, key = { it.id }) { r ->
                    Column(Modifier.padding(16.dp)) {
                        Text(r.name?.ifBlank { null } ?: r.tag, style = MaterialTheme.typography.titleMedium)
                        Text("${r.tag} · ${r.publishedAt.d()}" + if (r.prerelease) " · pre-release" else "", style = MaterialTheme.typography.labelMedium)
                        r.body?.takeIf { it.isNotBlank() }?.let { Text(it.take(600), Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall) }
                        r.assets.forEach { a ->
                            TextButton(onClick = {
                                scope.launch {
                                    try {
                                        Api.download(ctx, "/repos/$full/releases/assets/${a.id}", Api.BINARY, a.name)
                                        toast(ctx, "Guardado en Descargas/GitHubMobile")
                                    } catch (e: Exception) {
                                        toast(ctx, e.message)
                                    }
                                }
                            }) {
                                Icon(Icons.Default.CloudDownload, null)
                                Spacer(Modifier.width(6.dp))
                                Text("${a.name} (${a.size / 1024} KB)")
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
