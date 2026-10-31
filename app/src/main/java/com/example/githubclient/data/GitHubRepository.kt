package com.example.githubclient.data

import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class GitHubRepository(private val api: GitHubApi) {

    suspend fun getUser(): Result<User> = safe { api.getUser() }

    /** Descarga todos los repositorios del usuario, página a página. */
    suspend fun getAllRepos(): Result<List<Repo>> = safe {
        val all = mutableListOf<Repo>()
        var page = 1
        while (page <= MAX_PAGES) {
            val batch = api.getRepos(PAGE_SIZE, page, "updated", AFFILIATION)
            all += batch
            if (batch.size < PAGE_SIZE) break
            page++
        }
        all.toList()
    }

    suspend fun createRepo(
        name: String,
        description: String?,
        isPrivate: Boolean,
        autoInit: Boolean
    ): Result<Repo> = safe {
        api.createRepo(
            CreateRepoRequest(
                name = name,
                description = description,
                isPrivate = isPrivate,
                autoInit = autoInit
            )
        )
    }

    /** Lista el contenido de una carpeta ("" = raíz del repositorio). */
    suspend fun getContents(owner: String, repo: String, path: String): Result<List<ContentItem>> =
        safe {
            try {
                if (path.isEmpty()) {
                    api.getRootContents(owner, repo)
                } else {
                    api.getContents(owner, repo, Uri.encode(path, "/"))
                }
            } catch (e: HttpException) {
                // Un repositorio recién creado y vacío responde 404/409 en la raíz.
                if (path.isEmpty() && (e.code() == 404 || e.code() == 409)) {
                    emptyList()
                } else {
                    throw e
                }
            }
        }

    suspend fun getFile(owner: String, repo: String, path: String): Result<LoadedFile> = safe {
        val file = api.getFile(owner, repo, Uri.encode(path, "/"))
        withContext(Dispatchers.Default) { file.toLoadedFile() }
    }

    private fun FileContent.toLoadedFile(): LoadedFile {
        if (size == 0L) {
            return LoadedFile(name, path, size, htmlUrl, text = "", note = "El archivo está vacío.")
        }

        val raw = content
        if (raw.isNullOrBlank() || encoding != "base64") {
            return LoadedFile(
                name, path, size, htmlUrl,
                text = null,
                note = "El archivo es demasiado grande para previsualizarlo aquí. Ábrelo en GitHub."
            )
        }

        val bytes = Base64.decode(raw, Base64.DEFAULT)
        val probe = minOf(bytes.size, BINARY_PROBE_BYTES)
        for (i in 0 until probe) {
            if (bytes[i] == 0.toByte()) {
                return LoadedFile(
                    name, path, size, htmlUrl,
                    text = null,
                    note = "Archivo binario: no se puede mostrar como texto."
                )
            }
        }

        val full = String(bytes, Charsets.UTF_8)
        return if (full.length > MAX_CHARS) {
            LoadedFile(
                name, path, size, htmlUrl,
                text = full.take(MAX_CHARS),
                note = "Vista truncada a los primeros $MAX_CHARS caracteres."
            )
        } else {
            LoadedFile(name, path, size, htmlUrl, text = full, note = null)
        }
    }

    private suspend fun <T> safe(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private companion object {
        const val PAGE_SIZE = 100
        const val MAX_PAGES = 10
        const val AFFILIATION = "owner,collaborator,organization_member"
        const val MAX_CHARS = 200_000
        const val BINARY_PROBE_BYTES = 8_000
    }
}
