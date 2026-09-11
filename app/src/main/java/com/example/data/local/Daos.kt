package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LlmDao {
    @Query("SELECT * FROM llm_endpoints ORDER BY id ASC")
    fun getAllEndpoints(): Flow<List<LlmEndpointEntity>>

    @Query("SELECT * FROM llm_endpoints WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultEndpoint(): LlmEndpointEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEndpoint(endpoint: LlmEndpointEntity): Long

    @Update
    suspend fun updateEndpoint(endpoint: LlmEndpointEntity)

    @Delete
    suspend fun deleteEndpoint(endpoint: LlmEndpointEntity)

    @Query("UPDATE llm_endpoints SET isDefault = 0")
    suspend fun clearDefault()

    @Query("UPDATE llm_endpoints SET isDefault = 1 WHERE id = :id")
    suspend fun setDefault(id: Long)
}

@Dao
interface WorkspaceDao {
    @Query("SELECT * FROM workspace_files ORDER BY path ASC")
    fun getAllFiles(): Flow<List<WorkspaceFileEntity>>

    @Query("SELECT * FROM workspace_files WHERE path = :path LIMIT 1")
    suspend fun getFileByPath(path: String): WorkspaceFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFile(file: WorkspaceFileEntity)

    @Query("DELETE FROM workspace_files WHERE path = :path")
    suspend fun deleteFile(path: String)

    @Query("DELETE FROM workspace_files")
    suspend fun clearAllFiles()
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearChat()
}

@Dao
interface ConfigDao {
    @Query("SELECT value FROM app_config WHERE `key` = :key LIMIT 1")
    suspend fun getValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setValue(config: AppConfigEntity)
}
