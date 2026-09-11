package com.example.data.agent

import com.example.data.local.WorkspaceFileEntity

class ProjectRagEngine {

    fun searchWorkspace(query: String, files: List<WorkspaceFileEntity>): List<RagSearchResult> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()

        val results = mutableListOf<RagSearchResult>()

        for (file in files) {
            val lines = file.content.lines()
            var fileScore = 0f

            // Check file name & path
            if (file.path.lowercase().contains(q)) {
                fileScore += 5.0f
            }

            for ((idx, line) in lines.withIndex()) {
                val lowerLine = line.lowercase()
                if (lowerLine.contains(q)) {
                    val symbolType = when {
                        lowerLine.contains("fun ") || lowerLine.contains("function ") || lowerLine.contains("def ") -> "function"
                        lowerLine.contains("class ") || lowerLine.contains("interface ") -> "class"
                        lowerLine.contains("export ") || lowerLine.contains("import ") -> "module"
                        lowerLine.contains("test") || lowerLine.contains("assert") -> "test"
                        else -> "code"
                    }

                    val start = maxOf(0, idx - 1)
                    val end = minOf(lines.size, idx + 2)
                    val snippet = lines.subList(start, end).joinToString("\n")

                    val matchScore = fileScore + (if (symbolType != "code") 3.0f else 1.0f)
                    results.add(
                        RagSearchResult(
                            filePath = file.path,
                            snippet = snippet,
                            lineStart = idx + 1,
                            matchScore = matchScore,
                            symbolType = symbolType
                        )
                    )
                }
            }
        }

        return results.sortedByDescending { it.matchScore }.take(12)
    }

    fun buildContextBlock(selectedFiles: List<WorkspaceFileEntity>): String {
        if (selectedFiles.isEmpty()) return ""
        val sb = StringBuilder()
        sb.append("\n=== WORKSPACE CONTEXT (@context RAG) ===\n")
        for (file in selectedFiles) {
            sb.append("File: ${file.path} (${file.language}, ${file.content.lines().size} lines)\n```${file.language}\n")
            sb.append(file.content)
            sb.append("\n```\n\n")
        }
        sb.append("=== END CONTEXT ===\n")
        return sb.toString()
    }
}
