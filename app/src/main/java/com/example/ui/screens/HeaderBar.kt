package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R

@Composable
fun StudioHeader(
    currentModel: String,
    currentBranch: String,
    onSaveClick: () -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF16171A),
        border = BorderStroke(1.dp, Color(0xFF26282E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Emblem & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color(0xFFFF6B00), CircleShape)
                        .background(Color(0xFF0D0D0E)),
                    contentAlignment = Alignment.Center
                ) {
                    // Load remote brand logo with local fallback
                    AsyncImage(
                        model = "https://raw.githubusercontent.com/aishervin/Xrayng/refs/heads/main/Picsart_26-08-07_19-36-12-944.png",
                        contentDescription = "SHΞN Brand Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = painterResource(id = R.drawable.brand_icon)
                    )
                }

                Column {
                    Text(
                        text = "❮ SHΞN™ᴄᴏᴅᴇʀ ❯",
                        color = Color(0xFFFFFFFF),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF6B00))
                        )
                        Text(
                            text = if (currentModel.isNotBlank()) currentModel else "AI STUDIO",
                            color = Color(0xFFFF8800),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "• git: $currentBranch",
                            color = Color(0xFF8E9099),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Action icons
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onSaveClick,
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0xFF26282E), RoundedCornerShape(6.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save Workspace",
                        tint = Color(0xFFFF8800),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onExportClick,
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0xFF26282E), RoundedCornerShape(6.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = "Export Project ZIP",
                        tint = Color(0xFFFFFFFF),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
