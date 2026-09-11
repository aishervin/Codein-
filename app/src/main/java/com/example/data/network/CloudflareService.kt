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

class CloudflareService(private val client: OkHttpClient = OkHttpClient()) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val jsMediaType = "application/javascript; charset=utf-8".toMediaType()

    suspend fun verifyToken(token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://api.cloudflare.com/client/v4/user/tokens/verify")
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(IOException("HTTP ${resp.code}"))
                val json = JSONObject(resp.body?.string() ?: "")
                val success = json.optBoolean("success", false)
                Result.success(success)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listWorkers(accountId: String, token: String): Result<List<CloudflareWorkerItem>> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://api.cloudflare.com/client/v4/accounts/$accountId/workers/scripts")
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val err = resp.body?.string() ?: ""
                    return@withContext Result.failure(IOException("HTTP ${resp.code}: $err"))
                }
                val json = JSONObject(resp.body?.string() ?: "")
                val resultArr = json.optJSONArray("result") ?: JSONArray()
                val list = mutableListOf<CloudflareWorkerItem>()
                for (i in 0 until resultArr.length()) {
                    val item = resultArr.getJSONObject(i)
                    list.add(
                        CloudflareWorkerItem(
                            id = item.optString("id"),
                            createdOn = item.optString("created_on"),
                            modifiedOn = item.optString("modified_on"),
                            routes = item.optJSONArray("routes")?.length() ?: 0
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deployWorkerScript(
        accountId: String,
        token: String,
        scriptName: String,
        scriptContent: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://api.cloudflare.com/client/v4/accounts/$accountId/workers/scripts/$scriptName")
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/javascript")
                .put(scriptContent.toRequestBody(jsMediaType))
                .build()

            client.newCall(req).execute().use { resp ->
                val bodyStr = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IOException("Deploy failed (${resp.code}): $bodyStr"))
                }
                Result.success("Deployed successfully: $scriptName")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listD1Databases(accountId: String, token: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://api.cloudflare.com/client/v4/accounts/$accountId/d1/database")
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(IOException("HTTP ${resp.code}"))
                val json = JSONObject(resp.body?.string() ?: "")
                val arr = json.optJSONArray("result") ?: JSONArray()
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    val db = arr.getJSONObject(i)
                    list.add(db.optString("name", "d1_db_$i"))
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class CloudflareWorkerItem(
    val id: String,
    val createdOn: String,
    val modifiedOn: String,
    val routes: Int
)
