@file:OptIn(ExperimentalMaterial3Api::class)

package app.ghmobile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var clientId by remember { mutableStateOf(Store.clientId) }
    var device by remember { mutableStateOf<DeviceCode?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var pat by remember { mutableStateOf("") }

    fun finish(token: String) {
        Store.token = token
        Api.token = token
        onDone()
    }

    LaunchedEffect(device) {
        val d = device ?: return@LaunchedEffect
        var wait = d.interval
        val end = System.currentTimeMillis() + d.expiresIn * 1000L
        while (System.currentTimeMillis() < end) {
            delay(wait * 1000L)
            val r = try {
                Auth.poll(clientId, d.deviceCode)
            } catch (e: Exception) {
                null
            }
            if (r == null) continue
            if (r.accessToken != null) {
                finish(r.accessToken)
                return@LaunchedEffect
            }
            when (r.error) {
                "authorization_pending" -> {}
                "slow_down" -> wait += 5
                else -> {
                    error = r.errorDescription ?: r.error
                    device = null
                    return@LaunchedEffect
                }
            }
        }
        error = "El código expiró. Vuelve a intentarlo."
        device = null
    }

    Scaffold { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Text("GitHub Mobile", style = MaterialTheme.typography.headlineLarge)
            Text("Repos, código, Actions, issues y PRs desde tu celular.")

            val d = device
            if (d == null) {
                if (BuildConfig.GH_CLIENT_ID.isEmpty()) {
                    OutlinedTextField(
                        clientId, { clientId = it },
                        label = { Text("Client ID de tu OAuth App") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Button(
                    enabled = !busy && clientId.isNotBlank(),
                    onClick = {
                        busy = true
                        error = null
                        scope.launch {
                            try {
                                Store.clientId = clientId
                                val code = Auth.deviceCode(clientId.trim())
                                (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                                    .setPrimaryClip(ClipData.newPlainText("code", code.userCode))
                                ctx.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(code.verificationUri))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                                device = code
                            } catch (e: Exception) {
                                error = e.message
                            }
                            busy = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Iniciar sesión con GitHub") }
            } else {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Pega este código en GitHub (ya se copió):")
                        Text(d.userCode, fontSize = 34.sp, style = MaterialTheme.typography.headlineMedium)
                        CircularProgressIndicator()
                        Text("Esperando autorización…")
                        TextButton(onClick = {
                            ctx.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(d.verificationUri))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }) { Text("Abrir GitHub otra vez") }
                        TextButton(onClick = { device = null }) { Text("Cancelar") }
                    }
                }
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            HorizontalDivider()
            Text("¿Prefieres usar un token personal (PAT)?", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                pat, { pat = it },
                label = { Text("Token ghp_… / github_pat_…") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                enabled = !busy && pat.isNotBlank(),
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            val old = Api.token
                            Api.token = pat.trim()
                            try {
                                Api.get<GhUser>("/user")
                            } catch (e: Exception) {
                                Api.token = old
                                throw e
                            }
                            finish(pat.trim())
                        } catch (e: Exception) {
                            error = e.message
                        }
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Entrar con token") }
        }
    }
}
