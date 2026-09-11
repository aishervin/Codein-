package com.example.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class GitHubRepositoryService(private val client: OkHttpClient = OkHttpClient()) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun getRepositoryTree(
        token: String,
        owner: String,
        repo: String,
        branch: String = "main"
    ): Result<List<GitTreeItem>> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$owner/$repo/git/trees/$branch?recursive=1"
            val request = Request.Builder()
                .url(url)
                .apply { if (token.isNotBlank()) header("Authorization", "Bearer $token") }
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "ShenCoderStudio")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    return@withContext Result.failure(IOException("HTTP ${response.code}: $errorBody"))
                }
                val bodyStr = response.body?.string() ?: ""
                val jsonObj = JSONObject(bodyStr)
                val treeArray = jsonObj.optJSONArray("tree") ?: JSONArray()
                val items = mutableListOf<GitTreeItem>()
                for (i in 0 until treeArray.length()) {
                    val item = treeArray.getJSONObject(i)
                    items.add(
                        GitTreeItem(
                            path = item.optString("path"),
                            mode = item.optString("mode"),
                            type = item.optString("type"),
                            sha = item.optString("sha"),
                            size = item.optLong("size", 0)
                        )
                    )
                }
                Result.success(items)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchRawFile(
        token: String,
        owner: String,
        repo: String,
        branch: String,
        path: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "https://raw.githubusercontent.com/$owner/$repo/$branch/$path"
            val request = Request.Builder()
                .url(url)
                .apply { if (token.isNotBlank()) header("Authorization", "Bearer $token") }
                .header("User-Agent", "ShenCoderStudio")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Failed to fetch raw file: HTTP ${response.code}"))
                }
                Result.success(response.body?.string() ?: "")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Executes the complete Git Data API workflow directly pushing to the specified branch:
     * 1. Get Reference heads/{branch} -> latest commit SHA
     * 2. Get latest commit -> base tree SHA
     * 3. Create Blob -> new blob SHA
     * 4. Create Tree -> new tree SHA
     * 5. Create Commit -> new commit SHA
     * 6. Update Reference heads/{branch}
     */
    suspend fun commitFile(
        token: String,
        owner: String,
        repo: String,
        branch: String,
        path: String,
        content: String,
        commitMessage: String
    ): Result<GitCommitResult> = withContext(Dispatchers.IO) {
        try {
            if (token.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("GitHub Personal Access Token (PAT) is required"))
            }

            // Step 1: Get Reference
            val refUrl = "https://api.github.com/repos/$owner/$repo/git/ref/heads/$branch"
            val refReq = Request.Builder()
                .url(refUrl)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "ShenCoderStudio")
                .build()

            val latestCommitSha = client.newCall(refReq).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("Ref lookup failed (${resp.code}): ${resp.body?.string()}")
                val json = JSONObject(resp.body?.string() ?: "")
                json.getJSONObject("object").getString("sha")
            }

            // Step 2: Get Base Tree SHA from latest commit
            val commitUrl = "https://api.github.com/repos/$owner/$repo/git/commits/$latestCommitSha"
            val commitReq = Request.Builder()
                .url(commitUrl)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "ShenCoderStudio")
                .build()

            val baseTreeSha = client.newCall(commitReq).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("Commit lookup failed (${resp.code}): ${resp.body?.string()}")
                val json = JSONObject(resp.body?.string() ?: "")
                json.getJSONObject("tree").getString("sha")
            }

            // Step 3: Create Blob
            val blobUrl = "https://api.github.com/repos/$owner/$repo/git/blobs"
            val blobPayload = JSONObject().apply {
                put("content", content)
                put("encoding", "utf-8")
            }
            val blobReq = Request.Builder()
                .url(blobUrl)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "ShenCoderStudio")
                .post(blobPayload.toString().toRequestBody(jsonMediaType))
                .build()

            val blobSha = client.newCall(blobReq).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("Create blob failed (${resp.code}): ${resp.body?.string()}")
                val json = JSONObject(resp.body?.string() ?: "")
                json.getString("sha")
            }

            // Step 4: Create Tree
            val treeUrl = "https://api.github.com/repos/$owner/$repo/git/trees"
            val treePayload = JSONObject().apply {
                put("base_tree", baseTreeSha)
                put("tree", JSONArray().apply {
                    put(JSONObject().apply {
                        put("path", path)
                        put("mode", "100644")
                        put("type", "blob")
                        put("sha", blobSha)
                    })
                })
            }
            val treeReq = Request.Builder()
                .url(treeUrl)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "ShenCoderStudio")
                .post(treePayload.toString().toRequestBody(jsonMediaType))
                .build()

            val newTreeSha = client.newCall(treeReq).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("Create tree failed (${resp.code}): ${resp.body?.string()}")
                val json = JSONObject(resp.body?.string() ?: "")
                json.getString("sha")
            }

            // Step 5: Create Commit
            val newCommitUrl = "https://api.github.com/repos/$owner/$repo/git/commits"
            val newCommitPayload = JSONObject().apply {
                put("message", commitMessage)
                put("tree", newTreeSha)
                put("parents", JSONArray().apply { put(latestCommitSha) })
            }
            val newCommitReq = Request.Builder()
                .url(newCommitUrl)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "ShenCoderStudio")
                .post(newCommitPayload.toString().toRequestBody(jsonMediaType))
                .build()

            val newCommitSha = client.newCall(newCommitReq).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("Create commit failed (${resp.code}): ${resp.body?.string()}")
                val json = JSONObject(resp.body?.string() ?: "")
                json.getString("sha")
            }

            // Step 6: Update Reference
            val updateRefUrl = "https://api.github.com/repos/$owner/$repo/git/refs/heads/$branch"
            val updateRefPayload = JSONObject().apply {
                put("sha", newCommitSha)
                put("force", false)
            }
            val updateRefReq = Request.Builder()
                .url(updateRefUrl)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "ShenCoderStudio")
                .patch(updateRefPayload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(updateRefReq).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("Update ref failed (${resp.code}): ${resp.body?.string()}")
            }

            Result.success(
                GitCommitResult(
                    commitSha = newCommitSha,
                    treeSha = newTreeSha,
                    branch = branch,
                    message = commitMessage
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class GitTreeItem(
    val path: String,
    val mode: String,
    val type: String,
    val sha: String,
    val size: Long
)

data class GitCommitResult(
    val commitSha: String,
    val treeSha: String,
    val branch: String,
    val message: String
)
