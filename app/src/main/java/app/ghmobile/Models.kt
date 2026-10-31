package app.ghmobile

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Owner(
    val login: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class GhUser(
    val login: String,
    val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    val company: String? = null,
    val location: String? = null,
    @SerialName("public_repos") val publicRepos: Int = 0,
    val followers: Int = 0,
    val following: Int = 0,
)

@Serializable
data class Repo(
    val id: Long,
    val name: String,
    @SerialName("full_name") val fullName: String,
    val description: String? = null,
    @SerialName("private") val isPrivate: Boolean = false,
    val fork: Boolean = false,
    val language: String? = null,
    @SerialName("stargazers_count") val stars: Int = 0,
    @SerialName("forks_count") val forks: Int = 0,
    @SerialName("open_issues_count") val openIssues: Int = 0,
    @SerialName("default_branch") val defaultBranch: String = "main",
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("html_url") val htmlUrl: String = "",
    val owner: Owner? = null,
)

@Serializable
data class SearchRepos(val items: List<Repo> = emptyList())

@Serializable
data class ContentItem(
    val name: String = "",
    val path: String = "",
    val sha: String = "",
    val size: Long = 0,
    val type: String = "file",
)

@Serializable
data class BranchItem(val name: String)

@Serializable
data class CommitAuthor(val name: String? = null, val date: String? = null)

@Serializable
data class CommitInfo(val message: String = "", val author: CommitAuthor? = null)

@Serializable
data class CommitItem(val sha: String, val commit: CommitInfo)

@Serializable
data class Issue(
    val number: Int,
    val title: String,
    val state: String = "open",
    val body: String? = null,
    val user: Owner? = null,
    val comments: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("merged_at") val mergedAt: String? = null,
    @SerialName("pull_request") val pullRequest: JsonElement? = null,
)

@Serializable
data class IssueComment(
    val id: Long,
    val body: String = "",
    val user: Owner? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class Workflow(
    val id: Long,
    val name: String,
    val path: String = "",
    val state: String = "active",
)

@Serializable
data class WorkflowsResponse(val workflows: List<Workflow> = emptyList())

@Serializable
data class WfRun(
    val id: Long,
    val name: String? = null,
    @SerialName("display_title") val displayTitle: String? = null,
    val status: String? = null,
    val conclusion: String? = null,
    @SerialName("head_branch") val headBranch: String? = null,
    val event: String = "",
    @SerialName("run_number") val runNumber: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    val actor: Owner? = null,
)

@Serializable
data class RunsResponse(@SerialName("workflow_runs") val runs: List<WfRun> = emptyList())

@Serializable
data class WfStep(
    val name: String = "",
    val status: String? = null,
    val conclusion: String? = null,
    val number: Int = 0,
)

@Serializable
data class WfJob(
    val id: Long,
    val name: String = "",
    val status: String? = null,
    val conclusion: String? = null,
    val steps: List<WfStep> = emptyList(),
)

@Serializable
data class JobsResponse(val jobs: List<WfJob> = emptyList())

@Serializable
data class ArtifactItem(
    val id: Long,
    val name: String = "",
    @SerialName("size_in_bytes") val size: Long = 0,
    val expired: Boolean = false,
)

@Serializable
data class ArtifactsResponse(val artifacts: List<ArtifactItem> = emptyList())

@Serializable
data class AssetItem(val id: Long, val name: String = "", val size: Long = 0)

@Serializable
data class ReleaseItem(
    val id: Long,
    val name: String? = null,
    @SerialName("tag_name") val tag: String = "",
    val body: String? = null,
    val prerelease: Boolean = false,
    @SerialName("published_at") val publishedAt: String? = null,
    val assets: List<AssetItem> = emptyList(),
)

@Serializable
data class NotifSubject(val title: String = "", val type: String = "")

@Serializable
data class NotifRepo(@SerialName("full_name") val fullName: String = "")

@Serializable
data class Notif(
    val id: String,
    val unread: Boolean = false,
    val reason: String = "",
    val subject: NotifSubject = NotifSubject(),
    val repository: NotifRepo = NotifRepo(),
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class DeviceCode(
    @SerialName("device_code") val deviceCode: String = "",
    @SerialName("user_code") val userCode: String = "",
    @SerialName("verification_uri") val verificationUri: String = "",
    @SerialName("expires_in") val expiresIn: Int = 900,
    val interval: Int = 5,
    val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
)

@Serializable
data class TokenResp(
    @SerialName("access_token") val accessToken: String? = null,
    val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
)
