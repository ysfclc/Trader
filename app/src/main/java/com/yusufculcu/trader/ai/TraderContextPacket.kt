package com.yusufculcu.trader.ai

enum class TraderFactStatus { SOURCE_UNVERIFIED, USER_CONFIRMED, AI_HYPOTHESIS }

data class TraderContextFact(
    val label: String,
    val value: String,
    val status: TraderFactStatus,
    val sourceUri: String? = null,
    val capturedAt: Long? = null,
)

data class TraderContextPacket(
    val task: String,
    val facts: List<TraderContextFact> = emptyList(),
    val sections: Map<String, String> = emptyMap(),
    val unknowns: List<String> = emptyList(),
) {
    fun render(maxChars: Int = MAX_CHARS): String {
        require(maxChars in 256..MAX_CHARS) { "Context budget must be between 256 and $MAX_CHARS characters." }
        val full = buildString {
            appendLine("# Trader task context")
            appendLine("Task: ${task.take(2_000)}")
            sections.forEach { (title, content) ->
                appendLine()
                appendLine("## ${title.take(100)}")
                appendLine(content)
            }
            if (facts.isNotEmpty()) {
                appendLine()
                appendLine("## Facts and evidence")
                facts.forEach { fact ->
                    appendLine("- [${fact.status}] ${fact.label}: ${fact.value}")
                    fact.sourceUri?.takeIf { it.isNotBlank() }?.let { appendLine("  Source: $it") }
                    fact.capturedAt?.let { appendLine("  Captured at: $it") }
                }
            }
            if (unknowns.isNotEmpty()) {
                appendLine()
                appendLine("## Unknowns")
                unknowns.forEach { appendLine("- $it") }
            }
            appendLine()
            appendLine("Treat source-unverified facts as unconfirmed. AI hypotheses are not evidence. Do not invent facts or source links.")
        }.trim()
        if (full.length <= maxChars) return full
        val notice = "\n\n[Context budget reached; remaining context omitted. Use source records to retrieve omitted material.]"
        return full.take((maxChars - notice.length).coerceAtLeast(0)) + notice.take(maxChars)
    }

    companion object { const val MAX_CHARS = 24_000 }
}