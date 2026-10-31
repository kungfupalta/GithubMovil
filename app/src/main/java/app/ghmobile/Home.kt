@file:OptIn(ExperimentalMaterial3Api::class)

package app.ghmobile

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString

@Composable
fun HomeScreen(nav: NavController, onLogout: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(0) }
    val items: List<Pair<String, ImageVector>> = listOf(
        "Repos" to Icons.Default.Home,
        "Buscar" to Icons.Default.Search,
        "Avisos" to Icons.Default.Notifications,
        "Perfil" to Icons.Default.Person,
    )
    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { i, (label, icon) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(icon, null) },
                        label = { Text(label) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == 0) {
                FloatingActionButton(onClick = { nav.navigate("newrepo") }) { Icon(Icons.Default.Add, "Nuevo repo") }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> ReposTab(nav)
                1 -> SearchTab(nav)
                2 -> NotifTab(nav)
                else -> ProfileTab(onLogout)
            }
        }
    }
}

@Composable
fun ReposTab(nav: NavController) {
    var mode by rememberSaveable { mutableStateOf(0) }
    var q by rememberSaveable { mutableStateOf("") }
    val l = rememberLoader(mode) {
        if (mode == 0) {
            Api.get<List<Repo>>("/user/repos?per_page=100&sort=updated&affiliation=owner,collaborator,organization_member")
        } else {
            Api.get<List<Repo>>("/user/starred?per_page=100")
        }
    }
    Column(Modifier.statusBarsPadding()) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(mode == 0, { mode = 0 }, label = { Text("Mis repos") })
            FilterChip(mode == 1, { mode = 1 }, label = { Text("Destacados") })
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { l.reload() }) { Icon(Icons.Default.Refresh, "Recargar") }
        }
        OutlinedTextField(
            q, { q = it },
            placeholder = { Text("Filtrar repositorios") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(6.dp))
        LoaderView(l) { repos ->
            val f = repos.filter { it.fullName.contains(q, ignoreCase = true) }
            if (f.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Sin repositorios") }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                    items(f, key = { it.id }) { r -> RepoCard(r) { nav.navigate("repo/${r.fullName}") } }
                }
            }
        }
    }
}

@Composable
fun SearchTab(nav: NavController) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var q by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Repo>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }

    fun run() {
        if (q.isBlank()) return
        busy = true
        scope.launch {
            try {
                results = Api.get<SearchRepos>("/search/repositories?q=${Uri.encode(q)}&per_page=30").items
            } catch (e: Exception) {
                toast(ctx, e.message)
            }
            busy = false
        }
    }

    Column(Modifier.statusBarsPadding()) {
        OutlinedTextField(
            q, { q = it },
            placeholder = { Text("Buscar repos en todo GitHub") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { run() }),
            trailingIcon = { IconButton(onClick = { run() }) { Icon(Icons.Default.Search, "Buscar") } },
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        )
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn {
            items(results, key = { it.id }) { r -> RepoCard(r) { nav.navigate("repo/${r.fullName}") } }
        }
    }
}

@Composable
fun NotifTab(nav: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val l = rememberLoader(Unit) { Api.get<List<Notif>>("/notifications?all=false&per_page=50") }
    Column(Modifier.statusBarsPadding()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Notificaciones", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = {
                scope.launch {
                    try {
                        Api.call("/notifications", "PUT", Api.body("read" to true))
                        l.reload()
                    } catch (e: Exception) {
                        toast(ctx, e.message)
                    }
                }
            }) { Text("Marcar todo leído") }
            IconButton(onClick = { l.reload() }) { Icon(Icons.Default.Refresh, "Recargar") }
        }
        LoaderView(l) { list ->
            if (list.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Todo al día") }
            } else {
                LazyColumn {
                    items(list, key = { it.id }) { n ->
                        ListItem(
                            headlineContent = { Text(n.subject.title) },
                            supportingContent = { Text("${n.repository.fullName} · ${n.subject.type} · ${n.reason}") },
                            trailingContent = { if (n.unread) Text("●", color = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.clickable {
                                scope.launch {
                                    try {
                                        Api.call("/notifications/threads/${n.id}", "PATCH")
                                    } catch (_: Exception) {
                                    }
                                }
                                if (n.repository.fullName.isNotEmpty()) nav.navigate("repo/${n.repository.fullName}")
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileTab(onLogout: () -> Unit) {
    val l = rememberLoader(Unit) { Api.get<GhUser>("/user") }
    LoaderView(l) { u ->
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Avatar(u.avatarUrl, 96.dp)
            Text(u.name ?: u.login, style = MaterialTheme.typography.headlineSmall)
            Text("@${u.login}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            u.bio?.let { Text(it) }
            listOfNotNull(u.company, u.location).takeIf { it.isNotEmpty() }?.let { Text(it.joinToString(" · ")) }
            Text("${u.publicRepos} repos públicos · ${u.followers} seguidores · ${u.following} siguiendo")
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onLogout) { Text("Cerrar sesión") }
        }
    }
}

@Composable
fun NewRepoScreen(nav: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var priv by remember { mutableStateOf(true) }
    var readme by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Nuevo repositorio") },
            navigationIcon = {
                IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") }
            },
        )
    }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(desc, { desc = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(priv, { priv = it }); Spacer(Modifier.width(8.dp)); Text("Privado")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(readme, { readme = it }); Spacer(Modifier.width(8.dp)); Text("Crear con README")
            }
            Button(
                enabled = name.isNotBlank() && !busy,
                onClick = {
                    busy = true
                    scope.launch {
                        try {
                            val r = Api.json.decodeFromString<Repo>(
                                Api.call(
                                    "/user/repos", "POST",
                                    Api.body("name" to name.trim(), "description" to desc, "private" to priv, "auto_init" to readme),
                                ),
                            )
                            nav.popBackStack()
                            nav.navigate("repo/${r.fullName}")
                        } catch (e: Exception) {
                            toast(ctx, e.message)
                        }
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Crear") }
        }
    }
}
