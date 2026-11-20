package com.yusufculcu.trader.ai

enum class TraderModelRole { FAST, ANALYST, DEEP }

data class TraderModelSpec(
    val id: String,
    val name: String,
    val role: TraderModelRole,
    val repository: String,
    val fileName: String,
    val pinnedUrl: String,
    val sha256: String,
    val sizeBytes: Long,
    val minimumFreeRamBytes: Long,
    val maxOutputTokens: Int,
    val description: String,
) {
    fun isValidCatalogEntry(): Boolean =
        id.isNotBlank() && fileName.isNotBlank() &&
            pinnedUrl.startsWith("https://") &&
            sha256.length == 64 && sha256.all { it in '0'..'9' || it in 'a'..'f' } &&
            sizeBytes > 0 && minimumFreeRamBytes >= 0 && maxOutputTokens > 0
}

/** Trader-owned, pinned model metadata. Model files are not bundled or downloaded on first launch. */
object TraderModelCatalog {
    val entries = listOf(
        TraderModelSpec(
            id = "qwen3-0.6b", name = "Qwen3 0.6B", role = TraderModelRole.FAST,
            repository = "litert-community/Qwen3-0.6B",
            fileName = "Qwen3-0.6B_dynamic_wi4b32_afp32.litertlm",
            pinnedUrl = "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/e668717/Qwen3-0.6B_dynamic_wi4b32_afp32.litertlm?download=true",
            sha256 = "03e7da1eb1108b50dffaa9bb52cc7bcbad2eb0c66ca990267f480c1e545d2856",
            sizeBytes = 344_671_744L, minimumFreeRamBytes = 0L, maxOutputTokens = 700,
            description = "Small local orchestrator for routine extraction and planning."
        ),
        TraderModelSpec(
            id = "qwen3-1.7b", name = "Qwen3 1.7B", role = TraderModelRole.ANALYST,
            repository = "litert-community/Qwen3-1.7B",
            fileName = "Qwen3-1.7B_dynamic_wi4b32_afp32.litertlm",
            pinnedUrl = "https://huggingface.co/litert-community/Qwen3-1.7B/resolve/87603f0/Qwen3-1.7B_dynamic_wi4b32_afp32.litertlm?download=true",
            sha256 = "2a72330b504a6596d53a2c00923c57173b10f1b83e6a541213ffec08d1fc5de8",
            sizeBytes = 977_184_032L, minimumFreeRamBytes = 2_400_000_000L, maxOutputTokens = 1200,
            description = "Richer local analysis when the device has enough memory headroom."
        ),
        TraderModelSpec(
            id = "qwen3-4b-thinking", name = "Qwen3 4B Thinking", role = TraderModelRole.DEEP,
            repository = "litert-community/Qwen3-4B-Thinking-2507",
            fileName = "model.litertlm",
            pinnedUrl = "https://huggingface.co/litert-community/Qwen3-4B-Thinking-2507/resolve/f8ce479/model.litertlm?download=true",
            sha256 = "8a8a6a419a5570b3f4814d358fb795c0dcb6931206c7dae29480f47564c3f04d",
            sizeBytes = 2_477_192_112L, minimumFreeRamBytes = 4_300_000_000L, maxOutputTokens = 2048,
            description = "Optional deep reasoning model; load only after runtime memory preflight."
        )
    )

    fun find(id: String): TraderModelSpec? = entries.firstOrNull { it.id == id }
    fun defaultModel(): TraderModelSpec = entries.first { it.role == TraderModelRole.FAST }
    fun isCatalogValid(): Boolean = entries.size == entries.map { it.id }.distinct().size && entries.all { it.isValidCatalogEntry() }
}