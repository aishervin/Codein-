package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.DiffLine
import com.example.data.DiffType
import com.example.ui.StudioViewModel

@Composable
fun EditorScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val files by viewModel.files.collectAsState()
    val activeFile by viewModel.activeFile.collectAsState()
    val editorContent by viewModel.currentEditorContent.collectAsState()
    val diffLines by viewModel.diffLines.collectAsState()
    val lastRunnerResult by viewModel.lastRunnerResult.collectAsState()

    var showNewFileDialog by remember { mutableStateOf(false) }
    var newFileNameInput by remember { mutableStateOf("") }
    var showDiffDialog by remember { mutableStateOf(false) }
    var showSandboxDialog by remember { mutableStateOf(false) }
    var showRunnerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0E))
    ) {
        // File Tabs Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF16171A))
                .border(1.dp, Color(0xFF26282E))
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            files.forEach { file ->
                val isSelected = file.path == activeFile?.path
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectFile(file) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = file.name,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSelected) Color(0xFFFF6B00) else Color(0xFFFFFFFF)
                            )
                            if (file.isModified) {
                                Text(
                                    text = " •",
                                    color = Color(0xFFFF8800),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF26282E),
                        containerColor = Color(0xFF0D0D0E)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFFFF6B00) else Color(0xFF26282E)
                    )
                )
            }

            IconButton(
                onClick = { showNewFileDialog = true },
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFF26282E), RoundedCornerShape(4.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New File",
                    tint = Color(0xFFFF6B00),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Toolbar: Diff, Sandbox Preview, Delete
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111215))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = activeFile?.path ?: "No file open",
                    color = Color(0xFF8E9099),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (activeFile?.isModified == true) {
                    Text(
                        text = "[MODIFIED]",
                        color = Color(0xFFFF8800),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // BDS Sandbox Code Runner button
                OutlinedButton(
                    onClick = {
                        viewModel.executeCodeRunner(editorContent, activeFile?.language ?: "javascript")
                        showRunnerDialog = true
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp),
                    border = BorderStroke(1.dp, Color(0xFF00FF88).copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Run",
                        tint = Color(0xFF00FF88),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Run", fontSize = 11.sp, color = Color(0xFF00FF88), fontFamily = FontFamily.Monospace)
                }

                // Real-time Diff button
                OutlinedButton(
                    onClick = { showDiffDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp),
                    border = BorderStroke(1.dp, Color(0xFF26282E))
                ) {
                    Icon(
                        imageVector = Icons.Default.Difference,
                        contentDescription = "Diff",
                        tint = Color(0xFFFF8800),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Diff", fontSize = 11.sp, color = Color(0xFFFFFFFF), fontFamily = FontFamily.Monospace)
                }

                // Live Sandbox button
                Button(
                    onClick = { showSandboxDialog = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00))
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Sandbox",
                        tint = Color(0xFFFFFFFF),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Preview", fontSize = 11.sp, color = Color(0xFFFFFFFF), fontFamily = FontFamily.Monospace)
                }

                IconButton(
                    onClick = { viewModel.deleteActiveFile() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete File",
                        tint = Color(0xFF63656E),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Code Editor with Line Numbers Gutter
        val lineCount = remember(editorContent) { editorContent.lines().size }

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF0D0D0E))
        ) {
            // Line numbers column
            Column(
                modifier = Modifier
                    .width(40.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF16171A))
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (i in 1..lineCount) {
                    Text(
                        text = "$i",
                        color = Color(0xFF4A4D57),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Editor TextField
            TextField(
                value = editorContent,
                onValueChange = { viewModel.onEditorContentChange(it) },
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0D0D0E)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF0D0D0E),
                    unfocusedContainerColor = Color(0xFF0D0D0E),
                    focusedTextColor = Color(0xFFFFFFFF),
                    unfocusedTextColor = Color(0xFFFFFFFF),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            )
        }
    }

    // New File Dialog
    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = {
                Text("Create Workspace File", color = Color(0xFFFF6B00), fontFamily = FontFamily.Monospace)
            },
            text = {
                OutlinedTextField(
                    value = newFileNameInput,
                    onValueChange = { newFileNameInput = it },
                    placeholder = { Text("e.g. app.js, component.html, main.kt") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFFFFFFF),
                        unfocusedTextColor = Color(0xFFFFFFFF),
                        focusedBorderColor = Color(0xFFFF6B00),
                        unfocusedBorderColor = Color(0xFF26282E)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileNameInput.isNotBlank()) {
                            viewModel.createNewFile(newFileNameInput.trim())
                            newFileNameInput = ""
                            showNewFileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00))
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) {
                    Text("Cancel", color = Color(0xFF8E9099))
                }
            },
            containerColor = Color(0xFF16171A)
        )
    }

    // Real-time Diff Dialog
    if (showDiffDialog) {
        Dialog(
            onDismissRequest = { showDiffDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                color = Color(0xFF0D0D0E),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFFF6B00))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF16171A))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "REAL-TIME AST DIFF: ${activeFile?.name}",
                            color = Color(0xFFFF8800),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        IconButton(onClick = { showDiffDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFFFFFFF))
                        }
                    }

                    if (diffLines.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No modifications detected. Working tree clean.", color = Color(0xFF8E9099), fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            items(diffLines) { line ->
                                val bgColor = when (line.type) {
                                    DiffType.ADD -> Color(0x3300FF88)
                                    DiffType.DELETE -> Color(0x33FF3344)
                                    DiffType.SAME -> Color.Transparent
                                }
                                val textColor = when (line.type) {
                                    DiffType.ADD -> Color(0xFF00FF88)
                                    DiffType.DELETE -> Color(0xFFFF4455)
                                    DiffType.SAME -> Color(0xFFAAAAAA)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(bgColor)
                                        .padding(vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${line.lineNumber}".padStart(4, ' '),
                                        color = Color(0xFF4A4D57),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        text = line.text,
                                        color = textColor,
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
    }

    // Live Sandbox Preview Dialog
    if (showSandboxDialog) {
        val htmlFile = files.find { it.path == "index.html" }?.content ?: ""
        val cssFile = files.find { it.path == "styles.css" }?.content ?: ""
        val jsFile = files.find { it.path == "script.js" }?.content ?: ""

        val combinedHtml = remember(htmlFile, cssFile, jsFile) {
            val withCss = if (htmlFile.contains("</head>")) {
                htmlFile.replace("</head>", "<style>$cssFile</style></head>")
            } else {
                "<style>$cssFile</style>$htmlFile"
            }
            if (withCss.contains("</body>")) {
                withCss.replace("</body>", "<script>$jsFile</script></body>")
            } else {
                "$withCss<script>$jsFile</script>"
            }
        }

        var consoleLogs by remember { mutableStateOf(listOf("Sandbox runtime initialized.")) }

        Dialog(
            onDismissRequest = { showSandboxDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                color = Color(0xFF0D0D0E),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFFF6B00))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF16171A))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF00FF88), RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "❮ SHΞN™ᴄᴏᴅᴇʀ ❯ LIVE SANDBOX PREVIEW",
                                color = Color(0xFFFF8800),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        IconButton(onClick = { showSandboxDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFFFFFFF))
                        }
                    }

                    // WebView Sandbox Area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color.White)
                    ) {
                        SandboxWebView(
                            htmlData = combinedHtml,
                            onConsoleLog = { msg ->
                                consoleLogs = (consoleLogs + msg).takeLast(30)
                            }
                        )
                    }

                    // Live Console Output Drawer
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(Color(0xFF000000))
                            .border(1.dp, Color(0xFF26282E))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "SANDBOX CONSOLE LOGS:",
                            color = Color(0xFFFF6B00),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(consoleLogs) { log ->
                                Text(
                                    text = "> $log",
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

    // BDS Sandbox Code Runner Output Dialog
    if (showRunnerDialog) {
        Dialog(onDismissRequest = { showRunnerDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                color = Color(0xFF0D0D0E),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFFF6B00))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF00FF88), RoundedCornerShape(4.dp)))
                            Text(
                                text = "BDS:AUTO:CODE_RUNNER",
                                color = Color(0xFFFF6B00),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        IconButton(onClick = { showRunnerDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    if (lastRunnerResult != null) {
                        val res = lastRunnerResult!!
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                color = if (res.exitCode == 0) Color(0xFF00FF88).copy(alpha = 0.2f) else Color(0xFFFF3B30).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (res.exitCode == 0) "EXIT 0 (PASS)" else "EXIT ${res.exitCode} (ERR)",
                                    color = if (res.exitCode == 0) Color(0xFF00FF88) else Color(0xFFFF3B30),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "${res.language} • ${res.executionTimeMs}ms",
                                color = Color(0xFF8E9099),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Color(0xFF000000),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFF26282E)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 280.dp)
                        ) {
                            LazyColumn(modifier = Modifier.padding(8.dp)) {
                                if (res.stdout.isNotBlank()) {
                                    item {
                                        Text(
                                            text = res.stdout,
                                            color = Color(0xFF00FF88),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                if (res.stderr.isNotBlank()) {
                                    item {
                                        Text(
                                            text = res.stderr,
                                            color = Color(0xFFFF3B30),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Executing sandbox script...",
                            color = Color(0xFF8E9099),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = {
                                viewModel.executeCodeRunner(editorContent, activeFile?.language ?: "javascript")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00))
                        ) {
                            Text("RERUN", color = Color.White, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SandboxWebView(
    htmlData: String,
    onConsoleLog: (String) -> Unit
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowContentAccess = true
                settings.allowFileAccess = true

                webViewClient = WebViewClient()
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        consoleMessage?.let {
                            onConsoleLog("${it.messageLevel()}: ${it.message()} [line ${it.lineNumber()}]")
                        }
                        return true
                    }
                }
                loadDataWithBaseURL("http://localhost/", htmlData, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("http://localhost/", htmlData, "text/html", "UTF-8", null)
        },
        modifier = Modifier.fillMaxSize()
    )
}
