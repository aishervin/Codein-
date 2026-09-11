package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "llm_endpoints")
data class LlmEndpointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val modelName: String,
    val systemPrompt: String,
    val isDefault: Boolean = false
)

@Entity(tableName = "workspace_files")
data class WorkspaceFileEntity(
    @PrimaryKey val path: String,
    val name: String,
    val content: String,
    val originalContent: String = "",
    val language: String = "javascript",
    val isModified: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val model: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey val key: String,
    val value: String
)
