package com.yusufculcu.trader.ai

enum class TraderModelRole { FAST, ANALYST, DEEP }

data class TraderModelSpec(
    val id: String,
    val name: String,
    val role: TraderModelRole,
    val publisher: String,
    val repository: String,
    val modelPageUrl: String,
    val sourceRevision: String,
    val fileName: String,
    val pinnedUrl: String,
    val sha256: String,
    val sizeBytes: Long,
    val minimumFreeRamBytes: Long,
    val maxOutputTokens: Int,
    val format: String,
    val quantization: String,
    val licenseStatus: String,
    val description: String,
    val strengths: List<String>,
    val limitations: List<String>,
    val deviceGuidance: String,
    val updatePolicy: String = "Pinned snapshot. Updates are reviewed and repinned by Trader; no unverified model file is installed automatically.",
) {
    fun isValidCatalogEntry(): Boolean =
        id.isNotBlank() && name.isNotBlank() && publisher.isNotBlank() &&
            repository.isNotBlank() && modelPageUrl.startsWith("https://") &&
            sourceRevision.isNotBlank() && fileName.isNotBlank() &&
            pinnedUrl.startsWith("https://") &&
            sha256.length == 64 && sha256.all { it in '0'..'9' || it in 'a'..'f' } &&
            sizeBytes > 0 && minimumFreeRamBytes >= 0 && maxOutputTokens > 0 &&
            format.isNotBlank() && quantization.isNotBlank() &&
            licenseStatus.isNotBlank() && description.isNotBlank() &&
            strengths.isNotEmpty() && limitations.isNotEmpty() && deviceGuidance.isNotBlank()
}

/** Curated immutable releases: source revision, artifact size and digest are pinned together. */
object TraderModelCatalog {
    const val CATALOG_VERSION = 2
    const val UPDATE_CHANNEL = "reviewed-pinned"
    val entries = listOf(
        TraderModelSpec(
            id = "qwen3-0.6b", name = "Qwen3 0.6B", role = TraderModelRole.FAST,
            publisher = "LiteRT Community · upstream model family: Qwen",
            repository = "litert-community/Qwen3-0.6B",
            modelPageUrl = "https://huggingface.co/litert-community/Qwen3-0.6B",
            sourceRevision = "e668717",
            fileName = "Qwen3-0.6B_dynamic_wi4b32_afp32.litertlm",
            pinnedUrl = "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/e668717/Qwen3-0.6B_dynamic_wi4b32_afp32.litertlm?download=true",
            sha256 = "03e7da1eb1108b50dffaa9bb52cc7bcbad2eb0c66ca990267f480c1e545d2856",
            sizeBytes = 344_671_744L, minimumFreeRamBytes = 0L, maxOutputTokens = 700,
            format = "LiteRT-LM (.litertlm)", quantization = "WI4B32 / AFP32 (artifact naming)",
            licenseStatus = "Review the upstream model card and license before commercial redistribution or deployment.",
            description = "Small, fast model intended for lightweight local assistance and orchestration. It is the sensible first model to benchmark on memory-constrained phones, not a guarantee of high-quality complex reasoning.",
            strengths = listOf("Lowest download footprint in this curated set", "Candidate for short extraction and classification prompts", "Designed as a local on-device artifact"),
            limitations = listOf("Limited capacity for multi-step analysis and nuanced qualification", "Structured output must be validated by Trader"),
            deviceGuidance = "Recommended starting point for mid-range Android devices. Actual speed and memory use must be measured on the target device."
        ),
        TraderModelSpec(
            id = "qwen3-1.7b", name = "Qwen3 1.7B", role = TraderModelRole.ANALYST,
            publisher = "LiteRT Community · upstream model family: Qwen",
            repository = "litert-community/Qwen3-1.7B",
            modelPageUrl = "https://huggingface.co/litert-community/Qwen3-1.7B",
            sourceRevision = "87603f0",
            fileName = "Qwen3-1.7B_dynamic_wi4b32_afp32.litertlm",
            pinnedUrl = "https://huggingface.co/litert-community/Qwen3-1.7B/resolve/87603f0/Qwen3-1.7B_dynamic_wi4b32_afp32.litertlm?download=true",
            sha256 = "2a72330b504a6596d53a2c00923c57173b10f1b83e6a541213ffec08d1fc5de8",
            sizeBytes = 977_184_032L, minimumFreeRamBytes = 2_400_000_000L, maxOutputTokens = 1200,
            format = "LiteRT-LM (.litertlm)", quantization = "WI4B32 / AFP32 (artifact naming)",
            licenseStatus = "Review the upstream model card and license before commercial redistribution or deployment.",
            description = "A larger local model for tasks that need more context handling and analysis than the smallest profile. Its extra capacity comes with higher memory pressure and latency.",
            strengths = listOf("More capacity than the 0.6B profile", "Candidate for evidence summarization and draft analysis"),
            limitations = listOf("Higher RAM use and slower generation are expected", "RAM threshold is only a preflight guard, not a runtime guarantee"),
            deviceGuidance = "Try only after the 0.6B profile is stable. Benchmark sustained generation before making it the default."
        ),
        TraderModelSpec(
            id = "qwen3-4b-thinking", name = "Qwen3 4B Thinking", role = TraderModelRole.DEEP,
            publisher = "LiteRT Community · upstream model family: Qwen",
            repository = "litert-community/Qwen3-4B-Thinking-2507",
            modelPageUrl = "https://huggingface.co/litert-community/Qwen3-4B-Thinking-2507",
            sourceRevision = "f8ce479",
            fileName = "model.litertlm",
            pinnedUrl = "https://huggingface.co/litert-community/Qwen3-4B-Thinking-2507/resolve/f8ce479/model.litertlm?download=true",
            sha256 = "8a8a6a419a5570b3f4814d358fb795c0dcb6931206c7dae29480f47564c3f04d",
            sizeBytes = 2_477_192_112L, minimumFreeRamBytes = 4_300_000_000L, maxOutputTokens = 2048,
            format = "LiteRT-LM (.litertlm)", quantization = "Upstream artifact variant; verify model card for exact details",
            licenseStatus = "Review the upstream model card and license before commercial redistribution or deployment.",
            description = "A larger reasoning-oriented option for deliberate, multi-step tasks. It is an optional experiment, not the default model for everyday phone use.",
            strengths = listOf("Reasoning-oriented model profile", "Largest output budget in this curated set"),
            limitations = listOf("Largest download and highest memory pressure here", "May be impractical on phones with limited available RAM", "Long reasoning can increase latency and battery use"),
            deviceGuidance = "Do not install by default on an 8 GB device. Validate real peak memory, thermal behaviour and latency."
        )
    )
    fun find(id: String): TraderModelSpec? = entries.firstOrNull { it.id == id }
    fun defaultModel(): TraderModelSpec = entries.first { it.role == TraderModelRole.FAST }
    fun isCatalogValid(): Boolean = entries.size == entries.map { it.id }.distinct().size && entries.all { it.isValidCatalogEntry() }
}
