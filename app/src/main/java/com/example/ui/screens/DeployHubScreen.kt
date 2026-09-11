package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.StudioViewModel

@Composable
fun DeployHubScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val gitHubPat by viewModel.gitHubPat.collectAsState()
    val gitHubRepo by viewModel.gitHubRepo.collectAsState()
    val gitHubBranch by viewModel.gitHubBranch.collectAsState()
    val remoteTree by viewModel.remoteTree.collectAsState()
    val isGitLoading by viewModel.isGitLoading.collectAsState()
    val gitStatusMsg by viewModel.gitStatusMessage.collectAsState()

    val cfToken by viewModel.cfToken.collectAsState()
    val cfAccountId by viewModel.cfAccountId.collectAsState()
    val cfStatusMsg by viewModel.cfStatusMessage.collectAsState()

    var patInput by remember(gitHubPat) { mutableStateOf(gitHubPat) }
    var repoInput by remember(gitHubRepo) { mutableStateOf(gitHubRepo) }
    var branchInput by remember(gitHubBranch) { mutableStateOf(gitHubBranch) }
    var customCommitMsg by remember { mutableStateOf("") }
    var autoCommitMsg by remember { mutableStateOf(true) }
    var showPatPassword by remember { mutableStateOf(false) }

    var cfTokenInput by remember(cfToken) { mutableStateOf(cfToken) }
    var cfAccountInput by remember(cfAccountId) { mutableStateOf(cfAccountId) }
    var cfWorkerNameInput by remember { mutableStateOf("shen-edge-worker") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0E))
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // GITHUB CARD
        Surface(
            color = Color(0xFF16171A),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF26282E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "GitHub",
                        tint = Color(0xFFFF6B00),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GITHUB REST & GIT DATA API ENGINE",
                        color = Color(0xFFFFFFFF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "Push commits directly to heads/$branchInput via Git Data API (Create Blob -> Tree -> Commit -> Ref)",
                    color = Color(0xFF8E9099),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                // PAT Input
                OutlinedTextField(
                    value = patInput,
                    onValueChange = { patInput = it },
                    label = { Text("Personal Access Token (PAT)", fontSize = 11.sp) },
                    singleLine = true,
                    visualTransformation = if (showPatPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPatPassword = !showPatPassword }) {
                            Icon(
                                if (showPatPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle PAT",
                                tint = Color(0xFF8E9099)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B00),
                        unfocusedBorderColor = Color(0xFF26282E),
                        focusedTextColor = Color(0xFFFFFFFF),
                        unfocusedTextColor = Color(0xFFFFFFFF)
                    )
                )

                // Repo & Branch
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = repoInput,
                        onValueChange = { repoInput = it },
                        label = { Text("owner/repo", fontSize = 11.sp) },
                        placeholder = { Text("e.g. user/my-project") },
                        singleLine = true,
                        modifier = Modifier.weight(1.5f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF6B00),
                            unfocusedBorderColor = Color(0xFF26282E),
                            focusedTextColor = Color(0xFFFFFFFF),
                            unfocusedTextColor = Color(0xFFFFFFFF)
                        )
                    )

                    OutlinedTextField(
                        value = branchInput,
                        onValueChange = { branchInput = it },
                        label = { Text("Branch", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF6B00),
                            unfocusedBorderColor = Color(0xFF26282E),
                            focusedTextColor = Color(0xFFFFFFFF),
                            unfocusedTextColor = Color(0xFFFFFFFF)
                        )
                    )
                }

                Button(
                    onClick = {
                        viewModel.saveGitHubConfig(patInput.trim(), repoInput.trim(), branchInput.trim())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26282E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save GitHub Credentials", color = Color(0xFFFF8800), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }

                // Action Row: Fetch tree & Commit
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.fetchGitHubTree() },
                        enabled = !isGitLoading,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color(0xFFFF6B00))
                    ) {
                        Text("Fetch Remote Tree", color = Color(0xFFFFFFFF), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = {
                            val msg = if (autoCommitMsg) "Autonomous refactor via ❮ SHΞN™ᴄᴏᴅᴇʀ ❯" else customCommitMsg
                            viewModel.pushCommitToGitHub(msg)
                        },
                        enabled = !isGitLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Commit to Main", color = Color(0xFFFFFFFF), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                // Auto-commit option
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = autoCommitMsg,
                        onCheckedChange = { autoCommitMsg = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF6B00))
                    )
                    Text("Auto-generate clean commit message without AI attribution tags", color = Color(0xFF8E9099), fontSize = 11.sp)
                }

                if (!autoCommitMsg) {
                    OutlinedTextField(
                        value = customCommitMsg,
                        onValueChange = { customCommitMsg = it },
                        label = { Text("Commit Message", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF6B00),
                            unfocusedBorderColor = Color(0xFF26282E),
                            focusedTextColor = Color(0xFFFFFFFF),
                            unfocusedTextColor = Color(0xFFFFFFFF)
                        )
                    )
                }

                // Status Message Box
                if (gitStatusMsg.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0D0D0E), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF26282E), RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = gitStatusMsg,
                            color = Color(0xFF00FF88),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Remote Tree Nodes Display
                if (remoteTree.isNotEmpty()) {
                    Text(
                        text = "REMOTE TREE (${remoteTree.size} OBJECTS):",
                        color = Color(0xFFFF8800),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                            .background(Color(0xFF0D0D0E))
                            .border(1.dp, Color(0xFF26282E))
                            .padding(6.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(remoteTree.take(20)) { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.path,
                                        color = Color(0xFFFFFFFF),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = item.type,
                                        color = Color(0xFF8E9099),
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

        // CLOUDFLARE CARD
        Surface(
            color = Color(0xFF16171A),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF26282E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Cloudflare",
                        tint = Color(0xFFFF8800),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CLOUDFLARE EDGE & WORKERS DISPATCH",
                        color = Color(0xFFFFFFFF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                OutlinedTextField(
                    value = cfTokenInput,
                    onValueChange = { cfTokenInput = it },
                    label = { Text("Cloudflare API Token", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B00),
                        unfocusedBorderColor = Color(0xFF26282E),
                        focusedTextColor = Color(0xFFFFFFFF),
                        unfocusedTextColor = Color(0xFFFFFFFF)
                    )
                )

                OutlinedTextField(
                    value = cfAccountInput,
                    onValueChange = { cfAccountInput = it },
                    label = { Text("Cloudflare Account ID", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B00),
                        unfocusedBorderColor = Color(0xFF26282E),
                        focusedTextColor = Color(0xFFFFFFFF),
                        unfocusedTextColor = Color(0xFFFFFFFF)
                    )
                )

                OutlinedTextField(
                    value = cfWorkerNameInput,
                    onValueChange = { cfWorkerNameInput = it },
                    label = { Text("Worker / Script Name", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B00),
                        unfocusedBorderColor = Color(0xFF26282E),
                        focusedTextColor = Color(0xFFFFFFFF),
                        unfocusedTextColor = Color(0xFFFFFFFF)
                    )
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.saveCloudflareConfig(cfTokenInput.trim(), cfAccountInput.trim())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26282E)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Config", color = Color(0xFFFF8800), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = {
                            viewModel.deployCloudflareWorker(cfWorkerNameInput.trim())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Deploy to Edge", color = Color(0xFFFFFFFF), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                if (cfStatusMsg.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0D0D0E), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF26282E), RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = cfStatusMsg,
                            color = Color(0xFF00FF88),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
