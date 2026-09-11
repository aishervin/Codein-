package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.StudioViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: StudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainStudioApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainStudioApp(viewModel: StudioViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val currentEndpoint by viewModel.currentEndpoint.collectAsState()
    val currentBranch by viewModel.gitHubBranch.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            StudioHeader(
                currentModel = currentEndpoint?.modelName ?: "gpt-4o",
                currentBranch = currentBranch.ifBlank { "main" },
                onSaveClick = { viewModel.saveActiveFile() },
                onExportClick = {
                    viewModel.exportZipArchive { file ->
                        file?.let {
                            Toast.makeText(context, "Archive saved: ${it.absolutePath}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF16171A),
                contentColor = Color(0xFFFFFFFF)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setTab(0) },
                    icon = { Icon(Icons.Default.SmartToy, contentDescription = "AI Studio", modifier = Modifier.size(20.dp)) },
                    label = { Text("AI Studio", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFFF6B00),
                        selectedTextColor = Color(0xFFFF6B00),
                        unselectedIconColor = Color(0xFF8E9099),
                        unselectedTextColor = Color(0xFF8E9099),
                        indicatorColor = Color(0xFF26282E)
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setTab(1) },
                    icon = { Icon(Icons.Default.Code, contentDescription = "Editor & Sandbox", modifier = Modifier.size(20.dp)) },
                    label = { Text("Editor", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFFF6B00),
                        selectedTextColor = Color(0xFFFF6B00),
                        unselectedIconColor = Color(0xFF8E9099),
                        unselectedTextColor = Color(0xFF8E9099),
                        indicatorColor = Color(0xFF26282E)
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.setTab(2) },
                    icon = { Icon(Icons.Default.Language, contentDescription = "Web Engine", modifier = Modifier.size(20.dp)) },
                    label = { Text("Web Agent", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFFF6B00),
                        selectedTextColor = Color(0xFFFF6B00),
                        unselectedIconColor = Color(0xFF8E9099),
                        unselectedTextColor = Color(0xFF8E9099),
                        indicatorColor = Color(0xFF26282E)
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.setTab(3) },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = "Terminal", modifier = Modifier.size(20.dp)) },
                    label = { Text("Terminal", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFFF6B00),
                        selectedTextColor = Color(0xFFFF6B00),
                        unselectedIconColor = Color(0xFF8E9099),
                        unselectedTextColor = Color(0xFF8E9099),
                        indicatorColor = Color(0xFF26282E)
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { viewModel.setTab(4) },
                    icon = { Icon(Icons.Default.Tune, contentDescription = "Config", modifier = Modifier.size(20.dp)) },
                    label = { Text("Config", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFFF6B00),
                        selectedTextColor = Color(0xFFFF6B00),
                        unselectedIconColor = Color(0xFF8E9099),
                        unselectedTextColor = Color(0xFF8E9099),
                        indicatorColor = Color(0xFF26282E)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0D0D0E))
        ) {
            when (selectedTab) {
                0 -> AiStudioScreen(viewModel = viewModel)
                1 -> EditorScreen(viewModel = viewModel)
                2 -> WebEngineScreen(viewModel = viewModel)
                3 -> TerminalScreen(viewModel = viewModel)
                4 -> ConfigScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier, color = Color(0xFFFF6B00))
}
