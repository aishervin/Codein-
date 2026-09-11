package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.agent.*
import com.example.data.local.ChatMessageEntity
import com.example.data.local.WorkspaceFileEntity
import com.example.ui.StudioViewModel
import kotlinx.coroutines.launch

@Composable
fun AiStudioScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val agentMode by viewModel.agentMode.collectAsState()
    val agentPlan by viewModel.agentPlan.collectAsState()
    val lastRunnerResult by viewModel.lastRunnerResult.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val currentStreamText by viewModel.currentStreamText.collectAsState()
    val currentEndpoint by viewModel.currentEndpoint.collectAsState()
    val files by viewModel.files.collectAsState()
    val taggedFiles by viewModel.taggedContextFiles.collectAsState()
    val skills by viewModel.skillsList.collectAsState()

    var inputPrompt by remember { mutableStateOf("") }
    var showScaffoldDialog by remember { mutableStateOf(false) }
    var showContextDialog by remember { mutableStateOf(false) }
    var showSkillsDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val slashCommands = listOf(
        "/plan" to "Launch DeepCode Autonomous Planning",
        "/scaffold" to "Scaffold Multi-File Project",
        "/run" to "Execute in BDS:AUTO:CODE_RUNNER",
        "/rag" to "Semantic Workspace Search",
        "/audit" to "Zero-Trust Security Audit",
        "/diff" to "Review Code Changes",
        "/commit" to "Push Git Data API Commit",
        "/export" to "Pack Workspace to ZIP"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0E))
    ) {
        // Top Mode Switcher & Quick Tool Bar
        Surface(
            color = Color(0xFF16171A),
            border = BorderStroke(1.dp, Color(0xFF26282E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mode Selector Tabs
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF0D0D0E), RoundedCornerShape(6.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = if (agentMode == AgentMode.AUTONOMOUS_HARNESS) Color(0xFFFF6B00) else Color.Transparent,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.clickable { viewModel.setAgentMode(AgentMode.AUTONOMOUS_HARNESS) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Autonomous Harness",
                                    tint = if (agentMode == AgentMode.AUTONOMOUS_HARNESS) Color.White else Color(0xFF8E9099),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "AUTONOMOUS HARNESS",
                                    color = if (agentMode == AgentMode.AUTONOMOUS_HARNESS) Color.White else Color(0xFF8E9099),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Surface(
                            color = if (agentMode == AgentMode.INTERACTIVE_CHAT) Color(0xFF26282E) else Color.Transparent,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.clickable { viewModel.setAgentMode(AgentMode.INTERACTIVE_CHAT) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = "Interactive Studio",
                                    tint = if (agentMode == AgentMode.INTERACTIVE_CHAT) Color(0xFFFF6B00) else Color(0xFF8E9099),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "STUDIO CHAT",
                                    color = if (agentMode == AgentMode.INTERACTIVE_CHAT) Color.White else Color(0xFF8E9099),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Action buttons: Context, Skills, Scaffold
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // @Context RAG Pill
                        AssistChip(
                            onClick = { showContextDialog = true },
                            label = {
                                Text(
                                    text = "@Context ${if (taggedFiles.isNotEmpty()) "(${taggedFiles.size})" else ""}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (taggedFiles.isNotEmpty()) Color(0xFFFF6B00) else Color(0xFFFFFFFF)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (taggedFiles.isNotEmpty()) Color(0xFF26282E) else Color(0xFF16171A)
                            ),
                            border = BorderStroke(1.dp, if (taggedFiles.isNotEmpty()) Color(0xFFFF6B00) else Color(0xFF26282E)),
                            modifier = Modifier.height(26.dp)
                        )

                        // Skills Pill
                        AssistChip(
                            onClick = { showSkillsDialog = true },
                            label = {
                                Text(
                                    text = "Skills (${skills.count { it.isEnabled }})",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFFFFFFF)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF16171A)),
                            border = BorderStroke(1.dp, Color(0xFF26282E)),
                            modifier = Modifier.height(26.dp)
                        )

                        // Scaffold Pill
                        IconButton(
                            onClick = { showScaffoldDialog = true },
                            modifier = Modifier
                                .size(26.dp)
                                .background(Color(0xFF26282E), RoundedCornerShape(4.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderZip,
                                contentDescription = "Scaffold Project",
                                tint = Color(0xFFFF6B00),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }

        // Main Content Area (Autonomous Harness Dashboard vs Interactive Chat)
        Box(modifier = Modifier.weight(1f)) {
            if (agentMode == AgentMode.AUTONOMOUS_HARNESS) {
                AutonomousHarnessView(
                    plan = agentPlan,
                    lastRunnerResult = lastRunnerResult,
                    onExecuteGoal = { goal -> viewModel.executeAutonomousPlan(goal) },
                    onReRunRunner = { viewModel.executeCodeRunner() },
                    onSelectFile = { path ->
                        val target = files.find { it.path == path }
                        if (target != null) {
                            viewModel.selectFile(target)
                            viewModel.setTab(1)
                        }
                    }
                )
            } else {
                InteractiveChatView(
                    messages = messages,
                    isStreaming = isStreaming,
                    currentStreamText = currentStreamText,
                    currentEndpoint = currentEndpoint,
                    listState = listState,
                    onApplyCode = { code -> viewModel.applyCodeToEditor(code) }
                )
            }
        }

        // Slash Command Bar & Autocomplete Strip
        Surface(
            color = Color(0xFF16171A),
            border = BorderStroke(1.dp, Color(0xFF26282E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                // Slash Command Autocomplete Horizontal Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    slashCommands.forEach { (cmd, hint) ->
                        SuggestionChip(
                            onClick = {
                                inputPrompt = "$cmd "
                            },
                            label = {
                                Text(
                                    text = cmd,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF8800),
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = Color(0xFF0D0D0E)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF26282E)),
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                // Input Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.clearChatHistory() },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Chat",
                            tint = Color(0xFF8E9099)
                        )
                    }

                    OutlinedTextField(
                        value = inputPrompt,
                        onValueChange = { inputPrompt = it },
                        placeholder = {
                            Text(
                                if (agentMode == AgentMode.AUTONOMOUS_HARNESS)
                                    "Enter autonomous studio goal or /slash command..."
                                else
                                    "Ask ❮ SHΞN™ᴄᴏᴅᴇʀ ❯ anything...",
                                color = Color(0xFF63656E),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0D0D0E),
                            unfocusedContainerColor = Color(0xFF0D0D0E),
                            focusedBorderColor = Color(0xFFFF6B00),
                            unfocusedBorderColor = Color(0xFF26282E),
                            focusedTextColor = Color(0xFFFFFFFF),
                            unfocusedTextColor = Color(0xFFFFFFFF)
                        ),
                        maxLines = 4,
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    )

                    IconButton(
                        onClick = {
                            val prompt = inputPrompt
                            inputPrompt = ""
                            viewModel.sendChatMessage(prompt)
                        },
                        enabled = inputPrompt.isNotBlank() && !isStreaming,
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                if (inputPrompt.isNotBlank() && !isStreaming) Color(0xFFFF6B00) else Color(0xFF26282E),
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            imageVector = if (agentMode == AgentMode.AUTONOMOUS_HARNESS) Icons.Default.Bolt else Icons.Default.Send,
                            contentDescription = "Execute",
                            tint = Color(0xFFFFFFFF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // Project Scaffolder Dialog
    if (showScaffoldDialog) {
        ProjectScaffolderDialog(
            onDismiss = { showScaffoldDialog = false },
            onScaffold = { template, name ->
                showScaffoldDialog = false
                viewModel.scaffoldProjectTemplate(template, name)
            }
        )
    }

    // Context RAG Dialog
    if (showContextDialog) {
        ContextRagDialog(
            files = files,
            taggedFiles = taggedFiles,
            onToggleFile = { viewModel.toggleTagContextFile(it) },
            onClear = { viewModel.clearTaggedContext() },
            onDismiss = { showContextDialog = false }
        )
    }

    // Skills Dialog
    if (showSkillsDialog) {
        SkillsManagementDialog(
            skills = skills,
            onToggleSkill = { viewModel.toggleSkill(it) },
            onDismiss = { showSkillsDialog = false }
        )
    }
}

@Composable
fun AutonomousHarnessView(
    plan: AgentPlan,
    lastRunnerResult: CodeRunnerResult?,
    onExecuteGoal: (String) -> Unit,
    onReRunRunner: () -> Unit,
    onSelectFile: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Goal & Progress Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16171A)),
                border = BorderStroke(1.dp, Color(0xFFFF6B00).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        when (plan.status) {
                                            "COMPLETED" -> Color(0xFF00FF88)
                                            "EXECUTING", "RUNNING" -> Color(0xFFFF6B00)
                                            "PLANNING" -> Color(0xFF00D4FF)
                                            "FAILED" -> Color(0xFFFF3B30)
                                            else -> Color(0xFF8E9099)
                                        },
                                        CircleShape
                                    )
                            )
                            Text(
                                text = "AUTONOMOUS HARNESS ENGINE",
                                color = Color(0xFFFF6B00),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Surface(
                            color = Color(0xFF26282E),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = plan.status,
                                color = Color(0xFFFFFFFF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = plan.goal.ifBlank { "Awaiting autonomous coding assignment..." },
                        color = Color(0xFFFFFFFF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { plan.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = Color(0xFFFF6B00),
                        trackColor = Color(0xFF26282E)
                    )

                    if (plan.status == "IDLE") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Quick Launch Prompts:",
                            color = Color(0xFF8E9099),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Scaffold Cloudflare Edge API with Bearer Token Auth",
                                "Build Python 3.12 Ingestion Pipeline and PyTest Suite",
                                "Create Modern Cyber Web App with Tailwind and Live Telemetry",
                                "Run Zero-Trust Security and Code Quality Audit"
                            ).forEach { prompt ->
                                OutlinedButton(
                                    onClick = { onExecuteGoal(prompt) },
                                    border = BorderStroke(1.dp, Color(0xFF26282E)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text(
                                        text = prompt,
                                        fontSize = 10.sp,
                                        color = Color(0xFFFF8800),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Staged Files Banner (if files modified)
        if (plan.modifiedFiles.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "STAGED / GENERATED ARTIFACTS (${plan.modifiedFiles.size})",
                        color = Color(0xFF8E9099),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        plan.modifiedFiles.forEach { path ->
                            FilterChip(
                                selected = true,
                                onClick = { onSelectFile(path) },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.InsertDriveFile,
                                            contentDescription = null,
                                            tint = Color(0xFFFF6B00),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(text = path, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF1E2024)),
                                border = BorderStroke(1.dp, Color(0xFFFF6B00).copy(alpha = 0.6f))
                            )
                        }
                    }
                }
            }
        }

        // Execution Plan Steps
        if (plan.steps.isNotEmpty()) {
            item {
                Text(
                    text = "EXECUTION PLAN & TOOL CALL TRACE",
                    color = Color(0xFF8E9099),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            items(plan.steps) { step ->
                PlanStepCard(step = step)
            }
        }

        // BDS:AUTO:CODE_RUNNER Sandbox Output
        if (lastRunnerResult != null) {
            item {
                CodeRunnerWidget(result = lastRunnerResult, onRerun = onReRunRunner)
            }
        }

        // Execution Logs Terminal Feed
        if (plan.logs.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF000000)),
                    border = BorderStroke(1.dp, Color(0xFF26282E)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "HARNESS TELEMETRY LOGS",
                            color = Color(0xFF8E9099),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        plan.logs.takeLast(10).forEach { log ->
                            Text(
                                text = log,
                                color = if (log.contains("✔")) Color(0xFF00FF88) else if (log.contains("✘")) Color(0xFFFF3B30) else Color(0xFF8E9099),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlanStepCard(step: PlanStep) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16171A)),
        border = BorderStroke(
            1.dp,
            when (step.status) {
                StepStatus.RUNNING -> Color(0xFFFF6B00)
                StepStatus.SUCCESS -> Color(0xFF00FF88).copy(alpha = 0.5f)
                StepStatus.FAILED -> Color(0xFFFF3B30)
                StepStatus.PENDING -> Color(0xFF26282E)
            }
        ),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (step.status) {
                        StepStatus.SUCCESS -> Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF00FF88),
                            modifier = Modifier.size(16.dp)
                        )
                        StepStatus.RUNNING -> CircularProgressIndicator(
                            color = Color(0xFFFF6B00),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                        StepStatus.FAILED -> Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Failed",
                            tint = Color(0xFFFF3B30),
                            modifier = Modifier.size(16.dp)
                        )
                        StepStatus.PENDING -> Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Pending",
                            tint = Color(0xFF63656E),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = "Step ${step.id}: ${step.title}",
                        color = Color(0xFFFFFFFF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color(0xFF8E9099),
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = step.description,
                        color = Color(0xFF8E9099),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    if (step.toolCalls.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        step.toolCalls.forEach { tc ->
                            Surface(
                                color = Color(0xFF0D0D0E),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, Color(0xFF26282E)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "[TOOL] ${tc.type.name}",
                                            color = Color(0xFFFF8800),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = tc.status.name,
                                            color = if (tc.status == StepStatus.SUCCESS) Color(0xFF00FF88) else Color(0xFF8E9099),
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Text(
                                        text = tc.description,
                                        color = Color(0xFFFFFFFF),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    if (tc.output != null) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Observation: ${tc.output}",
                                            color = Color(0xFF00FF88),
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CodeRunnerWidget(result: CodeRunnerResult, onRerun: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0B0D)),
        border = BorderStroke(1.dp, if (result.exitCode == 0) Color(0xFF00FF88).copy(alpha = 0.5f) else Color(0xFFFF3B30)),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "BDS:AUTO:CODE_RUNNER",
                        color = Color(0xFFFF6B00),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "• ${result.language}",
                        color = Color(0xFF8E9099),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = if (result.exitCode == 0) Color(0xFF00FF88).copy(alpha = 0.2f) else Color(0xFFFF3B30).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (result.exitCode == 0) "EXIT 0 (PASS)" else "EXIT ${result.exitCode} (ERR)",
                            color = if (result.exitCode == 0) Color(0xFF00FF88) else Color(0xFFFF3B30),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "${result.executionTimeMs}ms",
                        color = Color(0xFF8E9099),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    IconButton(onClick = onRerun, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Rerun", tint = Color(0xFFFF8800), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = Color(0xFF000000),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, Color(0xFF1E2024)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    if (result.stdout.isNotBlank()) {
                        Text(
                            text = result.stdout,
                            color = Color(0xFF00FF88),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (result.stderr.isNotBlank()) {
                        Text(
                            text = result.stderr,
                            color = Color(0xFFFF3B30),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveChatView(
    messages: List<ChatMessageEntity>,
    isStreaming: Boolean,
    currentStreamText: String,
    currentEndpoint: com.example.data.local.LlmEndpointEntity?,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onApplyCode: (String) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (messages.isEmpty() && !isStreaming) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "❮ SHΞN™ᴄᴏᴅᴇʀ ❯ AI ENGINE",
                            color = Color(0xFFFF6B00),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Connected Provider: ${currentEndpoint?.name ?: "Autonomous Internal"}",
                            color = Color(0xFF8E9099),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Switch to Autonomous Harness or use /plan to generate full multi-file projects.",
                            color = Color(0xFF63656E),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        items(messages) { msg ->
            ChatMessageItem(message = msg, onApplyCode = onApplyCode)
        }

        if (isStreaming) {
            item {
                StreamingMessageItem(
                    text = currentStreamText,
                    model = currentEndpoint?.modelName ?: "LLM"
                )
            }
        }
    }
}

@Composable
fun ProjectScaffolderDialog(
    onDismiss: () -> Unit,
    onScaffold: (ScaffoldingTemplate, String) -> Unit
) {
    var projectName by remember { mutableStateOf("ShenApp") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF16171A),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFFFF6B00)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "PROJECT SCAFFOLDING ENGINE",
                    color = Color(0xFFFF6B00),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Generate complete multi-file repositories as ZIP-ready archives.",
                    color = Color(0xFF8E9099),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project Identifier", color = Color(0xFF8E9099), fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF6B00),
                        unfocusedBorderColor = Color(0xFF26282E),
                        focusedContainerColor = Color(0xFF0D0D0E),
                        unfocusedContainerColor = Color(0xFF0D0D0E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ScaffoldingTemplate.values()) { template ->
                        Surface(
                            color = Color(0xFF0D0D0E),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFF26282E)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onScaffold(template, projectName) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = template.title,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Surface(color = Color(0xFF26282E), shape = RoundedCornerShape(3.dp)) {
                                        Text(
                                            text = template.badge,
                                            color = Color(0xFFFF8800),
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = template.description,
                                    color = Color(0xFF8E9099),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = Color(0xFF8E9099), fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun ContextRagDialog(
    files: List<WorkspaceFileEntity>,
    taggedFiles: List<WorkspaceFileEntity>,
    onToggleFile: (WorkspaceFileEntity) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF16171A),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFFFF6B00)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PROJECT RAG CONTEXT",
                        color = Color(0xFFFF6B00),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    TextButton(onClick = onClear) {
                        Text("CLEAR ALL", color = Color(0xFFFF8800), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
                Text(
                    text = "Select workspace files to inject directly into agent memory window (@context).",
                    color = Color(0xFF8E9099),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(files) { f ->
                        val isTagged = taggedFiles.any { it.path == f.path }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isTagged) Color(0xFF26282E) else Color(0xFF0D0D0E), RoundedCornerShape(4.dp))
                                .border(1.dp, if (isTagged) Color(0xFFFF6B00) else Color(0xFF1E2024), RoundedCornerShape(4.dp))
                                .clickable { onToggleFile(f) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = f.path,
                                color = if (isTagged) Color(0xFFFF8800) else Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Checkbox(
                                checked = isTagged,
                                onCheckedChange = { onToggleFile(f) },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF6B00))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00))
                    ) {
                        Text("DONE", color = Color.White, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun SkillsManagementDialog(
    skills: List<CustomSkill>,
    onToggleSkill: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF16171A),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFFFF6B00)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "AUTONOMOUS SKILLS & MCP",
                    color = Color(0xFFFF6B00),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Toggle active developer capabilities and specialized system prompts.",
                    color = Color(0xFF8E9099),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(skills) { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0D0D0E), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFF26282E), RoundedCornerShape(4.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = s.name,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = s.description,
                                    color = Color(0xFF8E9099),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Switch(
                                checked = s.isEnabled,
                                onCheckedChange = { onToggleSkill(s.id) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF6B00))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00))
                    ) {
                        Text("DONE", color = Color.White, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessageEntity,
    onApplyCode: (String) -> Unit
) {
    val isUser = message.role == "user"
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Text(
                text = if (isUser) "OPERATOR" else "❮ SHΞN™ᴄᴏᴅᴇʀ ❯",
                color = if (isUser) Color(0xFFFF8800) else Color(0xFFFF6B00),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            if (message.model.isNotBlank()) {
                Text(
                    text = "• ${message.model}",
                    color = Color(0xFF63656E),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Surface(
            color = if (isUser) Color(0xFF1E2024) else Color(0xFF16171A),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, if (isUser) Color(0xFFFF6B00).copy(alpha = 0.3f) else Color(0xFF26282E)),
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            SelectionContainer {
                Column(modifier = Modifier.padding(10.dp)) {
                    val codeBlocks = extractCodeBlocks(message.content)
                    if (codeBlocks.isEmpty()) {
                        Text(
                            text = message.content,
                            color = Color(0xFFFFFFFF),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        RenderContentWithCode(message.content, onApplyCode)
                    }
                }
            }
        }
    }
}

@Composable
fun StreamingMessageItem(text: String, model: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "❮ SHΞN™ᴄᴏᴅᴇʀ ❯ • $model (STREAMING...)",
            color = Color(0xFFFF8800),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Surface(
            color = Color(0xFF16171A),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFFFF6B00)),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = text.ifEmpty { "Generating autonomous tokens..." },
                    color = Color(0xFFFFFFFF),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun RenderContentWithCode(
    content: String,
    onApplyCode: (String) -> Unit
) {
    val regex = "```([a-zA-Z0-9_-]*)\n([\\s\\S]*?)```".toRegex()
    var lastIndex = 0

    regex.findAll(content).forEach { matchResult ->
        val textBefore = content.substring(lastIndex, matchResult.range.first).trim()
        if (textBefore.isNotEmpty()) {
            Text(
                text = textBefore,
                color = Color(0xFFFFFFFF),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        val lang = matchResult.groupValues[1].ifEmpty { "code" }
        val code = matchResult.groupValues[2].trimEnd()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .background(Color(0xFF0D0D0E), RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFF26282E), RoundedCornerShape(6.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E2024))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = lang.uppercase(),
                    color = Color(0xFFFF6B00),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                TextButton(
                    onClick = { onApplyCode(code) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = "Inject to Editor",
                        tint = Color(0xFFFF8800),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Inject to Editor",
                        color = Color(0xFFFF8800),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Text(
                text = code,
                color = Color(0xFF00FF88),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(8.dp)
            )
        }

        lastIndex = matchResult.range.last + 1
    }

    val remaining = content.substring(lastIndex).trim()
    if (remaining.isNotEmpty()) {
        Text(
            text = remaining,
            color = Color(0xFFFFFFFF),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

fun extractCodeBlocks(content: String): List<String> {
    val regex = "```[\\s\\S]*?```".toRegex()
    return regex.findAll(content).map { it.value }.toList()
}
