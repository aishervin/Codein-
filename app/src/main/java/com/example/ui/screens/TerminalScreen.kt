package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.StudioViewModel
import kotlinx.coroutines.launch

@Composable
fun TerminalScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val history by viewModel.terminalHistory.collectAsState()
    var inputCommand by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val quickCommands = listOf("help", "sysinfo", "ls", "git status", "git push", "cf verify", "cf workers", "clear")

    LaunchedEffect(history.size) {
        scope.launch {
            if (history.isNotEmpty()) {
                listState.animateScrollToItem(history.size - 1)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070708))
    ) {
        // Quick Command Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF16171A))
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickCommands.forEach { cmd ->
                AssistChip(
                    onClick = {
                        viewModel.runTerminalCommand(cmd)
                    },
                    label = {
                        Text(
                            text = cmd,
                            color = Color(0xFFFF8800),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFF0D0D0E)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF26282E))
                )
            }
        }

        // Terminal Output
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(history) { line ->
                val isPrompt = line.startsWith("root@shen-studio:~$")
                val isError = line.contains("error", ignoreCase = true) || line.contains("failed", ignoreCase = true)

                Text(
                    text = line,
                    color = when {
                        isPrompt -> Color(0xFFFF6B00)
                        isError -> Color(0xFFFF4455)
                        line.startsWith("❮ SHΞN") -> Color(0xFFFF8800)
                        else -> Color(0xFF00FF88)
                    },
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp
                )
            }
        }

        // Command Prompt Input
        Surface(
            color = Color(0xFF111215),
            border = BorderStroke(1.dp, Color(0xFF26282E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "shen-sh~$ ",
                    color = Color(0xFFFF6B00),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                TextField(
                    value = inputCommand,
                    onValueChange = { inputCommand = it },
                    placeholder = {
                        Text(
                            "enter shell command...",
                            color = Color(0xFF4A4D57),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color(0xFFFFFFFF),
                        unfocusedTextColor = Color(0xFFFFFFFF),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                )

                IconButton(
                    onClick = {
                        val cmd = inputCommand
                        inputCommand = ""
                        viewModel.runTerminalCommand(cmd)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFFF6B00), RoundedCornerShape(6.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run",
                        tint = Color(0xFFFFFFFF),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
