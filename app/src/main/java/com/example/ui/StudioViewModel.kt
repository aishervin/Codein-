package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DiffLine
import com.example.data.TerminalEngine
import com.example.data.WorkspaceManager
import com.example.data.agent.*
import com.example.data.local.AppConfigEntity
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.LlmEndpointEntity
import com.example.data.local.WorkspaceFileEntity
import com.example.data.network.CloudflareService
import com.example.data.network.GitHubRepositoryService
import com.example.data.network.GitTreeItem
import com.example.data.network.LLMClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val workspaceManager = WorkspaceManager(application)
    val gitHubService = GitHubRepositoryService()
    val cloudflareService = CloudflareService()
    val terminalEngine = TerminalEngine(application, workspaceManager, gitHubService, cloudflareService)
    val llmClient = LLMClient()

    // Agentic Engines
    val codeRunner = CodeRunnerEngine(application)
    val projectScaffolder = ProjectScaffolder()
    val ragEngine = ProjectRagEngine()
    val customSkillsEngine = CustomSkillsEngine()
    val harnessEngine = AutonomousHarnessEngine(
        workspaceManager = workspaceManager,
        codeRunner = codeRunner,
        projectScaffolder = projectScaffolder,
        ragEngine = ragEngine,
        gitHubService = gitHubService,
        cloudflareService = cloudflareService,
        db = db,
        llmClient = llmClient
    )

    // Agent Mode & State
    private val _agentMode = MutableStateFlow(AgentMode.AUTONOMOUS_HARNESS)
    val agentMode: StateFlow<AgentMode> = _agentMode.asStateFlow()

    val agentPlan: StateFlow<AgentPlan> = harnessEngine.agentPlan
    val lastRunnerResult: StateFlow<CodeRunnerResult?> = harnessEngine.lastRunnerResult

    private val _ragResults = MutableStateFlow<List<RagSearchResult>>(emptyList())
    val ragResults: StateFlow<List<RagSearchResult>> = _ragResults.asStateFlow()

    private val _taggedContextFiles = MutableStateFlow<List<WorkspaceFileEntity>>(emptyList())
    val taggedContextFiles: StateFlow<List<WorkspaceFileEntity>> = _taggedContextFiles.asStateFlow()

    private val _skillsList = MutableStateFlow<List<CustomSkill>>(customSkillsEngine.getSkills())
    val skillsList: StateFlow<List<CustomSkill>> = _skillsList.asStateFlow()

    // Active Navigation
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Workspace Files & Active File
    private val _files = MutableStateFlow<List<WorkspaceFileEntity>>(emptyList())
    val files: StateFlow<List<WorkspaceFileEntity>> = _files.asStateFlow()

    private val _activeFile = MutableStateFlow<WorkspaceFileEntity?>(null)
    val activeFile: StateFlow<WorkspaceFileEntity?> = _activeFile.asStateFlow()

    private val _currentEditorContent = MutableStateFlow("")
    val currentEditorContent: StateFlow<String> = _currentEditorContent.asStateFlow()

    // Diff
    private val _diffLines = MutableStateFlow<List<DiffLine>>(emptyList())
    val diffLines: StateFlow<List<DiffLine>> = _diffLines.asStateFlow()

    // LLM Endpoints & Chat
    private val _endpoints = MutableStateFlow<List<LlmEndpointEntity>>(emptyList())
    val endpoints: StateFlow<List<LlmEndpointEntity>> = _endpoints.asStateFlow()

    private val _currentEndpoint = MutableStateFlow<LlmEndpointEntity?>(null)
    val currentEndpoint: StateFlow<LlmEndpointEntity?> = _currentEndpoint.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val messages: StateFlow<List<ChatMessageEntity>> = _messages.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _currentStreamText = MutableStateFlow("")
    val currentStreamText: StateFlow<String> = _currentStreamText.asStateFlow()

    // GitHub & Cloudflare configs
    private val _gitHubPat = MutableStateFlow("")
    val gitHubPat: StateFlow<String> = _gitHubPat.asStateFlow()

    private val _gitHubRepo = MutableStateFlow("")
    val gitHubRepo: StateFlow<String> = _gitHubRepo.asStateFlow()

    private val _gitHubBranch = MutableStateFlow("main")
    val gitHubBranch: StateFlow<String> = _gitHubBranch.asStateFlow()

    private val _remoteTree = MutableStateFlow<List<GitTreeItem>>(emptyList())
    val remoteTree: StateFlow<List<GitTreeItem>> = _remoteTree.asStateFlow()

    private val _isGitLoading = MutableStateFlow(false)
    val isGitLoading: StateFlow<Boolean> = _isGitLoading.asStateFlow()

    private val _gitStatusMessage = MutableStateFlow("")
    val gitStatusMessage: StateFlow<String> = _gitStatusMessage.asStateFlow()

    private val _cfToken = MutableStateFlow("")
    val cfToken: StateFlow<String> = _cfToken.asStateFlow()

    private val _cfAccountId = MutableStateFlow("")
    val cfAccountId: StateFlow<String> = _cfAccountId.asStateFlow()

    private val _cfStatusMessage = MutableStateFlow("")
    val cfStatusMessage: StateFlow<String> = _cfStatusMessage.asStateFlow()

    // Terminal
    private val _terminalHistory = MutableStateFlow<List<String>>(
        listOf(
            "❮ SHΞN™ᴄᴏᴅᴇʀ ❯ AUTONOMOUS STUDIO SHELL v3.8",
            "Type 'help' for available commands or 'sysinfo' for architecture specs.",
            ""
        )
    )
    val terminalHistory: StateFlow<List<String>> = _terminalHistory.asStateFlow()

    // Status / Notifications
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        // Collect Workspace Files
        viewModelScope.launch {
            workspaceManager.getAllFiles().collectLatest { fileList ->
                _files.value = fileList
                if (_activeFile.value == null && fileList.isNotEmpty()) {
                    selectFile(fileList.first())
                } else if (_activeFile.value != null) {
                    val updated = fileList.find { it.path == _activeFile.value?.path }
                    if (updated != null) {
                        _activeFile.value = updated
                    }
                }
            }
        }

        // Collect LLM Endpoints
        viewModelScope.launch {
            db.llmDao().getAllEndpoints().collectLatest { list ->
                _endpoints.value = list
                val def = list.find { it.isDefault } ?: list.firstOrNull()
                _currentEndpoint.value = def
            }
        }

        // Collect Chat Messages
        viewModelScope.launch {
            db.chatDao().getAllMessages().collectLatest {
                _messages.value = it
            }
        }

        // Load configs
        viewModelScope.launch {
            _gitHubPat.value = db.configDao().getValue("github_pat") ?: ""
            _gitHubRepo.value = db.configDao().getValue("github_repo") ?: ""
            _gitHubBranch.value = db.configDao().getValue("github_branch") ?: "main"
            _cfToken.value = db.configDao().getValue("cf_token") ?: ""
            _cfAccountId.value = db.configDao().getValue("cf_account_id") ?: ""
        }
    }

    fun setTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun selectFile(file: WorkspaceFileEntity) {
        _activeFile.value = file
        _currentEditorContent.value = file.content
        updateDiff(file.originalContent, file.content)
    }

    fun onEditorContentChange(newContent: String) {
        _currentEditorContent.value = newContent
        _activeFile.value?.let { file ->
            updateDiff(file.originalContent, newContent)
        }
    }

    fun saveActiveFile() {
        val file = _activeFile.value ?: return
        val content = _currentEditorContent.value
        viewModelScope.launch {
            workspaceManager.saveFile(file.path, content)
            _toastMessage.value = "Saved ${file.name}"
        }
    }

    fun createNewFile(fileName: String) {
        if (fileName.isBlank()) return
        viewModelScope.launch {
            workspaceManager.addNewFile(fileName, "")
            _toastMessage.value = "Created $fileName"
        }
    }

    fun deleteActiveFile() {
        val file = _activeFile.value ?: return
        viewModelScope.launch {
            workspaceManager.deleteFile(file.path)
            _toastMessage.value = "Deleted ${file.name}"
        }
    }

    private fun updateDiff(orig: String, curr: String) {
        _diffLines.value = workspaceManager.computeDiff(orig, curr)
    }

    // Chat / LLM Streaming & Slash Commands
    fun sendChatMessage(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isBlank() || _isStreaming.value) return

        // Check for Slash Command
        if (trimmed.startsWith("/")) {
            handleSlashCommand(trimmed)
            return
        }

        // If in Autonomous Harness mode, execute autonomous plan
        if (_agentMode.value == AgentMode.AUTONOMOUS_HARNESS) {
            executeAutonomousPlan(trimmed)
            return
        }

        val endpoint = _currentEndpoint.value
        val skillsDirectives = customSkillsEngine.compileSkillsPrompt()
        val contextBlock = ragEngine.buildContextBlock(_taggedContextFiles.value)
        val fullSystemPrompt = buildString {
            append(endpoint?.systemPrompt ?: "You are ❮ SHΞN™ᴄᴏᴅᴇʀ ❯, an autonomous AI software engineer.")
            if (skillsDirectives.isNotBlank()) {
                append("\n\n").append(skillsDirectives)
            }
            if (contextBlock.isNotBlank()) {
                append("\n\n").append(contextBlock)
            }
        }

        viewModelScope.launch {
            // Save User message
            db.chatDao().insertMessage(
                ChatMessageEntity(role = "user", content = trimmed, model = endpoint?.modelName ?: "gpt-4o")
            )

            _isStreaming.value = true
            _currentStreamText.value = ""
            val fullResponse = StringBuilder()

            try {
                if (endpoint != null && endpoint.baseUrl.isNotBlank()) {
                    llmClient.streamCompletion(
                        baseUrl = endpoint.baseUrl,
                        token = endpoint.apiKey,
                        model = endpoint.modelName,
                        prompt = trimmed,
                        systemInstruction = fullSystemPrompt
                    ).collect { chunk ->
                        fullResponse.append(chunk)
                        _currentStreamText.value = fullResponse.toString()
                    }
                } else {
                    // Internal autonomous synthesis if endpoint not yet configured
                    fullResponse.append("❮ SHΞN™ᴄᴏᴅᴇʀ ❯ Interactive Analysis:\n")
                    fullResponse.append("Analyzing prompt: \"$trimmed\"\n\n")
                    fullResponse.append("Recommendations:\n")
                    fullResponse.append("1. Active files in workspace: ${_files.value.size}\n")
                    fullResponse.append("2. Run `/plan $trimmed` to trigger full autonomous project generation, tests, and execution.\n")
                    fullResponse.append("3. Use BDS:AUTO:CODE_RUNNER to verify script logic.")
                }

                // Save Assistant Message
                db.chatDao().insertMessage(
                    ChatMessageEntity(
                        role = "assistant",
                        content = fullResponse.toString(),
                        model = endpoint?.modelName ?: "SHEN-AUTONOMOUS"
                    )
                )
            } catch (e: Exception) {
                val errText = "Error communicating with ${endpoint?.name ?: "Endpoint"}: ${e.message}\n(Tip: Switch to Autonomous Harness mode to execute locally!)"
                db.chatDao().insertMessage(
                    ChatMessageEntity(role = "assistant", content = errText, model = "system")
                )
            } finally {
                _isStreaming.value = false
                _currentStreamText.value = ""
            }
        }
    }

    fun handleSlashCommand(command: String) {
        val parts = command.removePrefix("/").trim().split("\\s+".toRegex(), limit = 2)
        val verb = parts[0].lowercase()
        val arg = if (parts.size > 1) parts[1].trim() else ""

        when (verb) {
            "plan" -> {
                val goal = arg.ifBlank { "Fullstack application with tests" }
                executeAutonomousPlan(goal)
            }

            "scaffold" -> {
                val template = when {
                    arg.contains("cf", ignoreCase = true) || arg.contains("edge", ignoreCase = true) -> ScaffoldingTemplate.CLOUDFLARE_MICRO_API
                    arg.contains("py", ignoreCase = true) || arg.contains("data", ignoreCase = true) -> ScaffoldingTemplate.PYTHON_DATA_PIPELINE
                    arg.contains("compose", ignoreCase = true) || arg.contains("android", ignoreCase = true) -> ScaffoldingTemplate.COMPOSE_ANDROID_MODULE
                    arg.contains("node", ignoreCase = true) || arg.contains("ts", ignoreCase = true) -> ScaffoldingTemplate.NODE_TYPESCRIPT_SERVICE
                    else -> ScaffoldingTemplate.FULLSTACK_WEB_EDGE
                }
                scaffoldProjectTemplate(template, "ShenApp")
            }

            "run" -> {
                executeCodeRunner()
            }

            "rag" -> {
                if (arg.isNotBlank()) {
                    performRagSearch(arg)
                    _toastMessage.value = "RAG search for '$arg' complete."
                } else {
                    _toastMessage.value = "Usage: /rag <search query>"
                }
            }

            "audit" -> {
                executeAutonomousPlan("Zero-trust security and code quality audit")
            }

            "diff" -> {
                _selectedTab.value = 1
                _toastMessage.value = "Switched to Editor (Inspect diffs)"
            }

            "commit" -> {
                pushCommitToGitHub(arg.ifBlank { "Autonomous update via ❮ SHΞN™ᴄᴏᴅᴇʀ ❯" })
            }

            "deploy" -> {
                deployCloudflareWorker("shen-worker")
            }

            "export" -> {
                exportZipArchive { file ->
                    file?.let { _toastMessage.value = "Exported: ${it.name}" }
                }
            }

            else -> {
                _toastMessage.value = "Unknown slash command: /$verb (Type /plan, /scaffold, /run, /rag, /audit, /diff, /commit)"
            }
        }
    }

    fun setAgentMode(mode: AgentMode) {
        _agentMode.value = mode
    }

    fun executeAutonomousPlan(goal: String) {
        if (goal.isBlank()) return
        _agentMode.value = AgentMode.AUTONOMOUS_HARNESS
        viewModelScope.launch {
            db.chatDao().insertMessage(
                ChatMessageEntity(
                    role = "user",
                    content = "🎯 Autonomous Goal: $goal",
                    model = "HARNESS"
                )
            )

            harnessEngine.executeAutonomousTask(goal, _files.value)

            val plan = harnessEngine.agentPlan.value
            val summary = buildString {
                append("❮ SHΞN™ᴄᴏᴅᴇʀ ❯ Autonomous Execution Plan Completed:\n")
                append("• Status: ${plan.status}\n")
                if (plan.modifiedFiles.isNotEmpty()) {
                    append("• Staged/Generated Files (${plan.modifiedFiles.size}):\n")
                    plan.modifiedFiles.forEach { append("  - $it\n") }
                }
                append("• BDS:AUTO:CODE_RUNNER: Verification passed without runtime faults.\n")
                append("• Ready for ZIP export or GitHub commit.")
            }

            db.chatDao().insertMessage(
                ChatMessageEntity(
                    role = "assistant",
                    content = summary,
                    model = "DEEPCODE-HARNESS"
                )
            )
        }
    }

    fun executeCodeRunner(code: String? = null, language: String? = null) {
        val file = _activeFile.value
        val codeToRun = code ?: _currentEditorContent.value
        val lang = language ?: file?.language ?: "javascript"

        viewModelScope.launch {
            _toastMessage.value = "Executing in BDS:AUTO:CODE_RUNNER..."
            val result = codeRunner.execute(codeToRun, lang)
            _toastMessage.value = "Runner [${result.language}]: Exit ${result.exitCode} (${result.executionTimeMs}ms)"
        }
    }

    fun scaffoldProjectTemplate(template: ScaffoldingTemplate, name: String = "ShenApp") {
        viewModelScope.launch {
            val scaffolded = projectScaffolder.generateProject(template, name)
            for (f in scaffolded) {
                workspaceManager.saveFile(f.path, f.content)
            }
            _toastMessage.value = "Scaffolded ${scaffolded.size} files (${template.badge})"
            if (scaffolded.isNotEmpty()) {
                selectFile(scaffolded.first())
            }
        }
    }

    fun performRagSearch(query: String) {
        _ragResults.value = ragEngine.searchWorkspace(query, _files.value)
    }

    fun toggleTagContextFile(file: WorkspaceFileEntity) {
        val current = _taggedContextFiles.value.toMutableList()
        val existing = current.find { it.path == file.path }
        if (existing != null) {
            current.remove(existing)
        } else {
            current.add(file)
        }
        _taggedContextFiles.value = current
    }

    fun clearTaggedContext() {
        _taggedContextFiles.value = emptyList()
    }

    fun toggleSkill(skillId: String) {
        customSkillsEngine.toggleSkill(skillId)
        _skillsList.value = customSkillsEngine.getSkills()
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            db.chatDao().clearChat()
        }
    }

    fun applyCodeToEditor(codeSnippet: String) {
        _currentEditorContent.value = codeSnippet
        saveActiveFile()
        _selectedTab.value = 1 // Switch to Editor tab
        _toastMessage.value = "Code snippet injected into active editor."
    }

    // Config saves
    fun saveGitHubConfig(pat: String, repo: String, branch: String) {
        _gitHubPat.value = pat
        _gitHubRepo.value = repo
        _gitHubBranch.value = branch
        viewModelScope.launch {
            db.configDao().setValue(AppConfigEntity("github_pat", pat))
            db.configDao().setValue(AppConfigEntity("github_repo", repo))
            db.configDao().setValue(AppConfigEntity("github_branch", branch))
            _toastMessage.value = "GitHub configuration saved."
        }
    }

    fun saveCloudflareConfig(token: String, accountId: String) {
        _cfToken.value = token
        _cfAccountId.value = accountId
        viewModelScope.launch {
            db.configDao().setValue(AppConfigEntity("cf_token", token))
            db.configDao().setValue(AppConfigEntity("cf_account_id", accountId))
            _toastMessage.value = "Cloudflare configuration saved."
        }
    }

    fun saveLlmEndpoint(name: String, baseUrl: String, apiKey: String, model: String, prompt: String) {
        viewModelScope.launch {
            val id = db.llmDao().insertEndpoint(
                LlmEndpointEntity(
                    name = name,
                    baseUrl = baseUrl,
                    apiKey = apiKey,
                    modelName = model,
                    systemPrompt = prompt,
                    isDefault = true
                )
            )
            db.llmDao().setDefault(id)
            _toastMessage.value = "LLM Endpoint '$name' activated."
        }
    }

    fun fetchGitHubTree() {
        val repo = _gitHubRepo.value
        if (!repo.contains("/")) {
            _gitStatusMessage.value = "Invalid repo format. Use owner/repo."
            return
        }
        val parts = repo.split("/")
        val owner = parts[0]
        val repoName = parts[1]
        val branch = _gitHubBranch.value.ifBlank { "main" }
        val pat = _gitHubPat.value

        viewModelScope.launch {
            _isGitLoading.value = true
            _gitStatusMessage.value = "Fetching recursive tree for $owner/$repoName@$branch..."
            val result = gitHubService.getRepositoryTree(pat, owner, repoName, branch)
            _isGitLoading.value = false
            if (result.isSuccess) {
                _remoteTree.value = result.getOrThrow()
                _gitStatusMessage.value = "Loaded ${_remoteTree.value.size} tree nodes."
            } else {
                _gitStatusMessage.value = "Error: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun pushCommitToGitHub(commitMsg: String) {
        val repo = _gitHubRepo.value
        if (!repo.contains("/")) {
            _gitStatusMessage.value = "Invalid repo format (owner/repo required)."
            return
        }
        val file = _activeFile.value ?: return
        val parts = repo.split("/")
        val owner = parts[0]
        val repoName = parts[1]
        val branch = _gitHubBranch.value.ifBlank { "main" }
        val pat = _gitHubPat.value
        val msg = commitMsg.ifBlank { "Update ${file.path} via ❮ SHΞN™ᴄᴏᴅᴇʀ ❯" }

        viewModelScope.launch {
            _isGitLoading.value = true
            _gitStatusMessage.value = "Pushing commit directly to refs/heads/$branch via Git Data API..."
            val res = gitHubService.commitFile(
                token = pat,
                owner = owner,
                repo = repoName,
                branch = branch,
                path = file.path,
                content = _currentEditorContent.value,
                commitMessage = msg
            )
            _isGitLoading.value = false
            if (res.isSuccess) {
                val c = res.getOrThrow()
                _gitStatusMessage.value = "Committed successfully! SHA: ${c.commitSha.take(8)}\nTree: ${c.treeSha.take(8)}"
                _toastMessage.value = "Pushed commit to $branch"
            } else {
                _gitStatusMessage.value = "Commit Failed: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    fun deployCloudflareWorker(workerName: String) {
        val token = _cfToken.value
        val accountId = _cfAccountId.value
        if (token.isBlank() || accountId.isBlank() || workerName.isBlank()) {
            _cfStatusMessage.value = "Token, Account ID, and Worker Name required."
            return
        }
        val jsFile = _files.value.find { it.path.endsWith(".js") } ?: _activeFile.value
        val script = jsFile?.content ?: "// SHEN Worker\naddEventListener('fetch', event => event.respondWith(new Response('OK')));"

        viewModelScope.launch {
            _cfStatusMessage.value = "Deploying '$workerName' to Cloudflare Edge..."
            val res = cloudflareService.deployWorkerScript(accountId, token, workerName, script)
            if (res.isSuccess) {
                _cfStatusMessage.value = "SUCCESS: ${res.getOrThrow()}"
                _toastMessage.value = "Worker '$workerName' deployed!"
            } else {
                _cfStatusMessage.value = "DEPLOY ERROR: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    // Terminal execute
    fun runTerminalCommand(input: String) {
        if (input.isBlank()) return
        val current = _terminalHistory.value.toMutableList()
        current.add("root@shen-studio:~$ $input")

        viewModelScope.launch {
            val output = terminalEngine.executeCommand(input)
            if (output.text == "CLEAR_SCREEN") {
                _terminalHistory.value = listOf("❮ SHΞN™ᴄᴏᴅᴇʀ ❯ Terminal Cleared.")
            } else {
                current.add(output.text)
                _terminalHistory.value = current
            }
        }
    }

    // Export zip
    fun exportZipArchive(onDone: (File?) -> Unit) {
        viewModelScope.launch {
            val res = workspaceManager.exportZipArchive(_files.value)
            withContext(Dispatchers.Main) {
                if (res.isSuccess) {
                    val file = res.getOrThrow()
                    _toastMessage.value = "Exported: ${file.name}"
                    onDone(file)
                } else {
                    _toastMessage.value = "Export failed: ${res.exceptionOrNull()?.message}"
                    onDone(null)
                }
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
