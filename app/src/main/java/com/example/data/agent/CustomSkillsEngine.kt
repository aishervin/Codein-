package com.example.data.agent

class CustomSkillsEngine {

    private val skills = mutableListOf(
        CustomSkill(
            id = "scaffolder",
            name = "Fullstack Scaffolder",
            description = "Autonomously scaffolds multi-file production architectures with configs and tests.",
            promptDirective = "You are an expert fullstack architect. When scaffolding, generate complete, working multi-file project files without placeholder ellipses.",
            isEnabled = true
        ),
        CustomSkill(
            id = "bug_hunter",
            name = "Bug Hunter & Self-Healer",
            description = "Analyzes execution errors from BDS:AUTO:CODE_RUNNER and generates targeted patches.",
            promptDirective = "You are an autonomous bug hunter. If any code runner errors occur, inspect the stack trace, identify the offending line, and output exact replacement patches.",
            isEnabled = true
        ),
        CustomSkill(
            id = "refactor",
            name = "Performance & Refactor",
            description = "Optimizes algorithmic complexity, memory consumption, and modern idiomatic patterns.",
            promptDirective = "You specialize in code refactoring. Ensure minimal memory allocations, optimal async flow, and clean separation of concerns.",
            isEnabled = true
        ),
        CustomSkill(
            id = "security",
            name = "Zero-Trust Security Auditor",
            description = "Scans code for hardcoded secrets, injection vulnerabilities, and CORS/Bearer issues.",
            promptDirective = "Enforce zero-trust security. Disallow hardcoded API keys, enforce input sanitization and HTTPS/Bearer validation.",
            isEnabled = true
        ),
        CustomSkill(
            id = "edge_deploy",
            name = "Cloudflare Edge Specialist",
            description = "Tailors worker scripts for Cloudflare Workers API, KV cache, and low-latency edge routing.",
            promptDirective = "You are an Edge Worker specialist. Generate compliant Service Worker syntax or ES Module fetch handlers with CORS headers.",
            isEnabled = true
        )
    )

    fun getSkills(): List<CustomSkill> = skills.toList()

    fun toggleSkill(id: String) {
        val idx = skills.indexOfFirst { it.id == id }
        if (idx != -1) {
            val item = skills[idx]
            skills[idx] = item.copy(isEnabled = !item.isEnabled)
        }
    }

    fun compileSkillsPrompt(): String {
        val enabled = skills.filter { it.isEnabled }
        if (enabled.isEmpty()) return ""
        val sb = StringBuilder("ACTIVE AGENT SKILLS & CAPABILITIES:\n")
        for (s in enabled) {
            sb.append("- [${s.name}]: ${s.promptDirective}\n")
        }
        return sb.toString()
    }
}
