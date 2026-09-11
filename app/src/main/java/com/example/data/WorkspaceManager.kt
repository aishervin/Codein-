package com.example.data

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.WorkspaceFileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class WorkspaceManager(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val workspaceDao = db.workspaceDao()

    fun getAllFiles(): Flow<List<WorkspaceFileEntity>> = workspaceDao.getAllFiles()

    suspend fun getFile(path: String): WorkspaceFileEntity? = workspaceDao.getFileByPath(path)

    suspend fun saveFile(path: String, content: String) = withContext(Dispatchers.IO) {
        val existing = workspaceDao.getFileByPath(path)
        val ext = path.substringAfterLast('.', "")
        val lang = when (ext.lowercase()) {
            "html", "htm" -> "html"
            "css" -> "css"
            "js" -> "javascript"
            "kt", "kts" -> "kotlin"
            "py" -> "python"
            "json" -> "json"
            else -> "text"
        }
        val isModified = existing?.originalContent != content
        workspaceDao.upsertFile(
            WorkspaceFileEntity(
                path = path,
                name = path.substringAfterLast('/'),
                content = content,
                originalContent = existing?.originalContent ?: content,
                language = lang,
                isModified = isModified,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun addNewFile(path: String, initialContent: String = "") = withContext(Dispatchers.IO) {
        val ext = path.substringAfterLast('.', "")
        val lang = when (ext.lowercase()) {
            "html" -> "html"
            "css" -> "css"
            "js" -> "javascript"
            "kt" -> "kotlin"
            else -> "text"
        }
        workspaceDao.upsertFile(
            WorkspaceFileEntity(
                path = path,
                name = path.substringAfterLast('/'),
                content = initialContent,
                originalContent = initialContent,
                language = lang,
                isModified = false,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteFile(path: String) = withContext(Dispatchers.IO) {
        workspaceDao.deleteFile(path)
    }

    /**
     * Computes a unified diff between originalContent and currentContent
     */
    fun computeDiff(original: String, current: String): List<DiffLine> {
        val origLines = if (original.isEmpty()) emptyList() else original.lines()
        val currLines = if (current.isEmpty()) emptyList() else current.lines()
        val diff = mutableListOf<DiffLine>()

        val max = maxOf(origLines.size, currLines.size)
        for (i in 0 until max) {
            val o = origLines.getOrNull(i)
            val c = currLines.getOrNull(i)
            if (o == null && c != null) {
                diff.add(DiffLine(type = DiffType.ADD, text = "+ $c", lineNumber = i + 1))
            } else if (o != null && c == null) {
                diff.add(DiffLine(type = DiffType.DELETE, text = "- $o", lineNumber = i + 1))
            } else if (o != c) {
                diff.add(DiffLine(type = DiffType.DELETE, text = "- $o", lineNumber = i + 1))
                diff.add(DiffLine(type = DiffType.ADD, text = "+ $c", lineNumber = i + 1))
            } else if (o != null) {
                diff.add(DiffLine(type = DiffType.SAME, text = "  $o", lineNumber = i + 1))
            }
        }
        return diff
    }

    /**
     * Bundles the current workspace files into a .zip archive
     */
    suspend fun exportZipArchive(files: List<WorkspaceFileEntity>): Result<File> = withContext(Dispatchers.IO) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val zipFile = File(exportDir, "shen_project_${System.currentTimeMillis()}.zip")
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                for (file in files) {
                    val entry = ZipEntry(file.path)
                    zos.putNextEntry(entry)
                    zos.write(file.content.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }
            }
            Result.success(zipFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

enum class DiffType {
    SAME, ADD, DELETE
}

data class DiffLine(
    val type: DiffType,
    val text: String,
    val lineNumber: Int
)
