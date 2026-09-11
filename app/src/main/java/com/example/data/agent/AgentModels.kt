package com.example.data.agent

enum class AgentMode {
    INTERACTIVE_CHAT,
    AUTONOMOUS_HARNESS
}

enum class StepStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED
}

enum class ToolCallType {
    CREATE_FILE,
    EDIT_FILE,
    SCAFFOLD_PROJECT,
    RUN_CODE,
    SEARCH_RAG,
    AUDIT_SECURITY,
    GIT_COMMIT,
    TERMINAL_EXEC
}

data class AgentToolCall(
    val id: String,
    val type: ToolCallType,
    val description: String,
    val args: Map<String, String>,
    val status: StepStatus = StepStatus.PENDING,
    val output: String? = null
)

data class PlanStep(
    val id: Int,
    val title: String,
    val description: String,
    val status: StepStatus = StepStatus.PENDING,
    val toolCalls: List<AgentToolCall> = emptyList(),
    val observation: String? = null
)

data class AgentPlan(
    val goal: String,
    val status: String = "IDLE", // PLANNING, RUNNING, VERIFYING, COMPLETED, FAILED
    val steps: List<PlanStep> = emptyList(),
    val progress: Float = 0f,
    val currentStepIndex: Int = 0,
    val logs: List<String> = emptyList(),
    val modifiedFiles: List<String> = emptyList()
)

data class CodeRunnerResult(
    val language: String,
    val exitCode: Int, // 0 = SUCCESS, 1 = ERROR
    val stdout: String,
    val stderr: String,
    val executionTimeMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)

data class RagSearchResult(
    val filePath: String,
    val snippet: String,
    val lineStart: Int,
    val matchScore: Float,
    val symbolType: String // function, class, config, file
)

data class CustomSkill(
    val id: String,
    val name: String,
    val description: String,
    val promptDirective: String,
    val isEnabled: Boolean = true
)
