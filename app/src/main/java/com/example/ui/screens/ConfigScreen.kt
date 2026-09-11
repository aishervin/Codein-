package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.StudioViewModel

@Composable
fun ConfigScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val endpoints by viewModel.endpoints.collectAsState()
    val currentEndpoint by viewModel.currentEndpoint.collectAsState()

    var nameInput by remember { mutableStateOf("Groq Fast Inference") }
    var baseUrlInput by remember { mutableStateOf("https://api.groq.com/openai/v1") }
    var tokenInput by remember { mutableStateOf("") }
    var modelInput by remember { mutableStateOf("llama-3.3-70b-versatile") }
    var promptInput by remember { mutableStateOf("You are SHΞN™ Autonomous Coding Agent. You deliver concise, production-ready, clean code without conversational filler.") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0E))
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Presets quick selection
        Surface(
            color = Color(0xFF16171A),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF26282E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Presets",
                        tint = Color(0xFFFF6B00),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MODEL AGNOSTIC ENGINE (PROVIDER PRESETS)",
                        color = Color(0xFFFFFFFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            nameInput = "OpenAI Official"
                            baseUrlInput = "https://api.openai.com/v1"
                            modelInput = "gpt-4o"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26282E)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f).height(30.dp)
                    ) {
                        Text("OpenAI", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = {
                            nameInput = "Groq Cloud"
                            baseUrlInput = "https://api.groq.com/openai/v1"
                            modelInput = "llama-3.3-70b-versatile"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26282E)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f).height(30.dp)
                    ) {
                        Text("Groq", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = {
                            nameInput = "Ollama Local"
                            baseUrlInput = "http://10.0.2.2:11434/v1"
                            modelInput = "deepseek-coder"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26282E)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f).height(30.dp)
                    ) {
                        Text("Ollama", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // Configuration Fields Card
        Surface(
            color = Color(0xFF16171A),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF26282E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "ADD / EDIT CUSTOM ENDPOINT",
                    color = Color(0xFFFF8800),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Provider Name") },
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
                    value = baseUrlInput,
                    onValueChange = { baseUrlInput = it },
                    label = { Text("Base URL (e.g. https://api.openai.com/v1)") },
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
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { Text("Auth Token / Bearer API Key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B00),
                        unfocusedBorderColor = Color(0xFF26282E),
                        focusedTextColor = Color(0xFFFFFFFF),
                        unfocusedTextColor = Color(0xFFFFFFFF)
                    )
                )

                OutlinedTextField(
                    value = modelInput,
                    onValueChange = { modelInput = it },
                    label = { Text("Model Name (e.g. gpt-4o, claude-3-5, llama-3)") },
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
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    label = { Text("System Instructions / Coding Persona") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B00),
                        unfocusedBorderColor = Color(0xFF26282E),
                        focusedTextColor = Color(0xFFFFFFFF),
                        unfocusedTextColor = Color(0xFFFFFFFF)
                    )
                )

                Button(
                    onClick = {
                        if (nameInput.isNotBlank() && baseUrlInput.isNotBlank()) {
                            viewModel.saveLlmEndpoint(
                                name = nameInput.trim(),
                                baseUrl = baseUrlInput.trim(),
                                apiKey = tokenInput.trim(),
                                model = modelInput.trim(),
                                prompt = promptInput.trim()
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Activate Endpoint", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save & Activate Endpoint", color = Color(0xFFFFFFFF), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Active Endpoints List
        Surface(
            color = Color(0xFF16171A),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF26282E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "STORED CONFIGURATIONS (${endpoints.size})",
                    color = Color(0xFFFF8800),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                endpoints.forEach { ep ->
                    val isActive = ep.id == currentEndpoint?.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isActive) Color(0xFF26282E) else Color(0xFF0D0D0E), RoundedCornerShape(6.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = ep.name,
                                color = Color(0xFFFFFFFF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${ep.modelName} • ${ep.baseUrl}",
                                color = Color(0xFF8E9099),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (isActive) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active",
                                tint = Color(0xFF00FF88),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
