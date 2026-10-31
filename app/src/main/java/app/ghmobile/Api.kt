package app.ghmobile

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ApiException(val code: Int, message: String) : Exception(message)

object Api {
    const val ACCEPT = "application/vnd.github+json"
    const val RAW = "application/vnd.github.raw+json"
    const val BINARY = "application/octet-stream"

    @Volatile
    var token: String = ""

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val JSON_TYPE = "application/json".toMediaType()

    fun enc(path: String): String = path.split("/").joinToString("/") { Uri.encode(it) }

    fun contentsPath(repo: String, path: String, ref: String? = null): String {
        val base = "/repos/$repo/contents/${enc(path)}"
        return if (ref.isNullOrEmpty()) base else "$base?ref=${Uri.encode(ref)}"
    }

    fun body(vararg p: Pair<String, Any?>): String = buildJsonObject {
        p.forEach { (k, v) ->
            when (v) {
                null -> {}
                is Boolean -> put(k, v)
                is Number -> put(k, v)
                is JsonElement -> put(k, v)
                else -> put(k, v.toString())
            }
        }
    }.toString()

    private fun build(path: String, method: String, body: String?, accept: String): Request {
        val url = if (path.startsWith("http")) path else "https://api.github.com$path"
        val b = Request.Builder().url(url)
            .header("Accept", accept)
            .header("X-GitHub-Api-Version", "2022-11-28")
        if (token.isNotEmpty()) b.header("Authorization", "Bearer $token")
        val rb = body?.toRequestBody(JSON_TYPE)
        when (method) {
            "GET" -> b.get()
            "DELETE" -> if (rb != null) b.delete(rb) else b.delete()
            else -> b.method(method, rb ?: ByteArray(0).toRequestBody(null))
        }
        return b.build()
    }

    private fun fail(code: Int, text: String): Nothing {
        val msg = try {
            json.parseToJsonElement(text).jsonObject["message"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            null
        }
        throw ApiException(code, msg ?: "HTTP $code")
    }

    suspend fun call(
        path: String,
        method: String = "GET",
        body: String? = null,
        accept: String = ACCEPT,
    ): String = withContext(Dispatchers.IO) {
        client.newCall(build(path, method, body, accept)).execute().use { r ->
            val t = r.body?.string().orEmpty()
            if (!r.isSuccessful) fail(r.code, t)
            t
        }
    }

    suspend inline fun <reified T> get(path: String): T = json.decodeFromString<T>(call(path))

    suspend fun bytes(path: String, accept: String): ByteArray = withContext(Dispatchers.IO) {
        client.newCall(build(path, "GET", null, accept)).execute().use { r ->
            if (!r.isSuccessful) fail(r.code, r.body?.string().orEmpty())
            r.body!!.bytes()
        }
    }

    suspend fun contents(repo: String, path: String, ref: String): List<ContentItem> =
        json.decodeFromString(call(contentsPath(repo, path, ref)))

    suspend fun putFile(repo: String, path: String, data: ByteArray, message: String, branch: String) {
        val sha = try {
            json.decodeFromString<ContentItem>(call(contentsPath(repo, path, branch))).sha
        } catch (e: ApiException) {
            if (e.code == 404) null else throw e
        } catch (e: SerializationException) {
            null
        }
        call(
            contentsPath(repo, path), "PUT",
            body(
                "message" to message,
                "content" to Base64.encodeToString(data, Base64.NO_WRAP),
                "branch" to branch,
                "sha" to sha,
            ),
        )
    }

    suspend fun deleteFile(repo: String, path: String, sha: String, message: String, branch: String) {
        call(
            contentsPath(repo, path), "DELETE",
            body("message" to message, "sha" to sha, "branch" to branch),
        )
    }

    /** Descarga a Descargas/GitHubMobile usando MediaStore (sin permisos extra). */
    suspend fun download(
        ctx: Context,
        path: String,
        accept: String,
        fileName: String,
        mime: String = BINARY,
    ) {
        withContext(Dispatchers.IO) {
            client.newCall(build(path, "GET", null, accept)).execute().use { r ->
                if (!r.isSuccessful) fail(r.code, r.body?.string().orEmpty())
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mime)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/GitHubMobile")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val res = ctx.contentResolver
                val uri = res.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("No se pudo crear el archivo")
                res.openOutputStream(uri)!!.use { out ->
                    r.body!!.byteStream().use { it.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                res.update(uri, values, null, null)
            }
        }
    }
}

object Auth {
    private const val SCOPES = "repo workflow user notifications gist read:org delete_repo"

    suspend fun deviceCode(clientId: String): DeviceCode = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("https://github.com/login/device/code")
            .header("Accept", "application/json")
            .post(FormBody.Builder().add("client_id", clientId).add("scope", SCOPES).build())
            .build()
        Api.client.newCall(req).execute().use { r ->
            val d = Api.json.decodeFromString<DeviceCode>(r.body?.string().orEmpty())
            if (d.error != null) throw ApiException(r.code, d.errorDescription ?: d.error)
            d
        }
    }

    suspend fun poll(clientId: String, deviceCode: String): TokenResp = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("https://github.com/login/oauth/access_token")
            .header("Accept", "application/json")
            .post(
                FormBody.Builder()
                    .add("client_id", clientId)
                    .add("device_code", deviceCode)
                    .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                    .build(),
            )
            .build()
        Api.client.newCall(req).execute().use { r ->
            Api.json.decodeFromString<TokenResp>(r.body?.string().orEmpty())
        }
    }
}
