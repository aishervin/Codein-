package com.example.data

import android.content.Context
import android.os.Build
import com.example.data.local.AppDatabase
import com.example.data.local.WorkspaceFileEntity
import com.example.data.network.CloudflareService
import com.example.data.network.GitHubRepositoryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TerminalEngine(
    private val context: Context,
    private val workspaceManager: WorkspaceManager,
    private val gitHubService: GitHubRepositoryService = GitHubRepositoryService(),
    private val cloudflareService: CloudflareService = CloudflareService()
) {
    private val db = AppDatabase.getDatabase(context)

    suspend fun executeCommand(cmdLine: String): TerminalOutput = withContext(Dispatchers.IO) {
        val trimmed = cmdLine.trim()
        if (trimmed.isEmpty()) return@withContext TerminalOutput("", false)

        val parts = trimmed.split("\\s+".toRegex())
        val cmd = parts[0].lowercase()
        val args = parts.drop(1)

        when (cmd) {
            "help" -> TerminalOutput(
                """
                ❮ SHΞN™ᴄᴏᴅᴇʀ ❯ AUTONOMOUS SHELL COMMANDS:
                  help                     Show available CLI commands
                  sysinfo                  Display device architecture & Android telemetry
                  ls                       List workspace files and sizes
                  cat <file>               Display file content in terminal
                  rm <file>                Delete workspace file
                  touch <file>             Create empty workspace file
                  git status               Check branch and modified workspace files
                  git push                 Push current workspace file to GitHub via Git Data API
                  cf workers               List deployed Cloudflare workers
                  cf verify                Test Cloudflare API token
                  clear                    Clear terminal screen
                """.trimIndent()
            )

            "sysinfo" -> {
                val runtime = Runtime.getRuntime()
                val totalMem = runtime.totalMemory() / (1024 * 1024)
                val freeMem = runtime.freeMemory() / (1024 * 1024)
                val usedMem = totalMem - freeMem
                TerminalOutput(
                    """
                    [SYSTEM TELEMETRY]
                    OS: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
                    DEVICE: ${Build.MANUFACTURER} ${Build.MODEL}
                    SUPPORTED ABIS: ${Build.SUPPORTED_ABIS.joinToString(", ")}
                    RUNTIME HEAP: ${usedMem}MB / ${totalMem}MB used
                    STUDIO CORE: ❮ SHΞN™ᴄᴏᴅᴇʀ ❯ v3.8 Autonomous Engine
                    STATUS: SECURE_SANDBOX_ACTIVE
                    """.trimIndent()
                )
            }

            "ls" -> {
                val files = db.workspaceDao().getFileByPath("index.html") // trigger query or get list
                // get all files
                val allFiles = mutableListOf<WorkspaceFileEntity>()
                val cursor = db.openHelper.readableDatabase.query("SELECT path, LENGTH(content), isModified FROM workspace_files")
                while (cursor.moveToNext()) {
                    val p = cursor.getString(0)
                    val len = cursor.getInt(1)
                    val mod = cursor.getInt(2) == 1
                    allFiles.add(WorkspaceFileEntity(path = p, name = p, content = "", isModified = mod, updatedAt = len.toLong()))
                }
                cursor.close()

                if (allFiles.isEmpty()) {
                    TerminalOutput("No files in workspace.")
                } else {
                    val sb = StringBuilder("MODE        SIZE    MODIFIED  NAME\n")
                    for (f in allFiles) {
                        val modFlag = if (f.isModified) "[*MOD*]" else "       "
                        sb.append("-rw-r--r--  ${f.updatedAt.toString().padStart(6)}B  $modFlag  ${f.path}\n")
                    }
                    TerminalOutput(sb.toString().trimEnd())
                }
            }

            "cat" -> {
                if (args.isEmpty()) return@withContext TerminalOutput("Usage: cat <filename>", isError = true)
                val filename = args[0]
                val file = db.workspaceDao().getFileByPath(filename)
                if (file == null) {
                    TerminalOutput("cat: $filename: No such file in workspace", isError = true)
                } else {
                    TerminalOutput(file.content)
                }
            }

            "rm" -> {
                if (args.isEmpty()) return@withContext TerminalOutput("Usage: rm <filename>", isError = true)
                val filename = args[0]
                workspaceManager.deleteFile(filename)
                TerminalOutput("Removed '$filename' from workspace.")
            }

            "touch" -> {
                if (args.isEmpty()) return@withContext TerminalOutput("Usage: touch <filename>", isError = true)
                val filename = args[0]
                workspaceManager.addNewFile(filename, "")
                TerminalOutput("Created '$filename'.")
            }

            "git" -> {
                handleGitCommand(args)
            }

            "cf" -> {
                handleCloudflareCommand(args)
            }

            "clear" -> TerminalOutput("CLEAR_SCREEN")

            else -> TerminalOutput("shen-sh: command not found: $cmd (type 'help' for commands)", isError = true)
        }
    }

    private suspend fun handleGitCommand(args: List<String>): TerminalOutput {
        if (args.isEmpty()) return TerminalOutput("Usage: git <status | push>", isError = true)
        val sub = args[0].lowercase()
        return when (sub) {
            "status" -> {
                val branch = db.configDao().getValue("github_branch") ?: "main"
                val repo = db.configDao().getValue("github_repo") ?: "not-configured"
                val cursor = db.openHelper.readableDatabase.query("SELECT path FROM workspace_files WHERE isModified = 1")
                val modified = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    modified.add(cursor.getString(0))
                }
                cursor.close()

                val sb = StringBuilder()
                sb.append("On branch $branch\n")
                sb.append("Connected repo: $repo\n")
                if (modified.isEmpty()) {
                    sb.append("nothing to commit, working tree clean")
                } else {
                    sb.append("Changes not staged for commit:\n")
                    for (m in modified) {
                        sb.append("  modified: $m\n")
                    }
                    sb.append("\nUse Deploy Hub to direct-push to GitHub via Git Data API.")
                }
                TerminalOutput(sb.toString())
            }
            "push" -> {
                val pat = db.configDao().getValue("github_pat") ?: ""
                val repoStr = db.configDao().getValue("github_repo") ?: ""
                val branch = db.configDao().getValue("github_branch") ?: "main"
                if (pat.isBlank() || !repoStr.contains("/")) {
                    return TerminalOutput("git push error: GitHub PAT or Repo not configured. Go to Deploy Hub to set credentials.", isError = true)
                }
                val parts = repoStr.split("/")
                val owner = parts[0]
                val repo = parts[1]
                val file = db.workspaceDao().getFileByPath("index.html")
                    ?: return TerminalOutput("No index.html to push.", isError = true)

                val res = gitHubService.commitFile(
                    token = pat,
                    owner = owner,
                    repo = repo,
                    branch = branch,
                    path = file.path,
                    content = file.content,
                    commitMessage = "Update ${file.path} via ❮ SHΞN™ᴄᴏᴅᴇʀ ❯ Studio"
                )
                if (res.isSuccess) {
                    val c = res.getOrThrow()
                    TerminalOutput("To https://github.com/$repoStr.git\n   ${c.commitSha.take(7)}..${branch} -> $branch\nCommit: ${c.message}")
                } else {
                    TerminalOutput("git push failed: ${res.exceptionOrNull()?.message}", isError = true)
                }
            }
            else -> TerminalOutput("git: unknown subcommand '$sub'. Use 'git status' or 'git push'.", isError = true)
        }
    }

    private suspend fun handleCloudflareCommand(args: List<String>): TerminalOutput {
        if (args.isEmpty()) return TerminalOutput("Usage: cf <verify | workers>", isError = true)
        val token = db.configDao().getValue("cf_token") ?: ""
        val accountId = db.configDao().getValue("cf_account_id") ?: ""
        when (args[0].lowercase()) {
            "verify" -> {
                if (token.isBlank()) return TerminalOutput("Error: Cloudflare token not configured.", isError = true)
                val res = cloudflareService.verifyToken(token)
                return if (res.getOrDefault(false)) {
                    TerminalOutput("Cloudflare API Token: VALID [Status: Active]")
                } else {
                    TerminalOutput("Cloudflare API Token: INVALID or EXPIRED", isError = true)
                }
            }
            "workers" -> {
                if (token.isBlank() || accountId.isBlank()) {
                    return TerminalOutput("Error: Cloudflare Token & Account ID required.", isError = true)
                }
                val res = cloudflareService.listWorkers(accountId, token)
                return if (res.isSuccess) {
                    val list = res.getOrThrow()
                    if (list.isEmpty()) TerminalOutput("No Workers found in account $accountId.")
                    else TerminalOutput("Cloudflare Workers (${list.size}):\n" + list.joinToString("\n") { "• ${it.id} (routes: ${it.routes})" })
                } else {
                    TerminalOutput("Failed to fetch workers: ${res.exceptionOrNull()?.message}", isError = true)
                }
            }
            else -> return TerminalOutput("Unknown cf subcommand. Use 'cf verify' or 'cf workers'.", isError = true)
        }
    }
}

data class TerminalOutput(
    val text: String,
    val isError: Boolean = false
)
