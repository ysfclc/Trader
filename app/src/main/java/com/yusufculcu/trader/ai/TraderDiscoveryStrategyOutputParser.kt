package com.yusufculcu.trader.ai

import com.yusufculcu.trader.domain.DiscoveryQuery
import com.yusufculcu.trader.domain.DiscoveryStrategy
import com.yusufculcu.trader.domain.DiscoveryStrategyRules
import com.yusufculcu.trader.domain.DiscoveryTarget
import com.yusufculcu.trader.domain.ProofAsset
import com.yusufculcu.trader.domain.ProblemSignature
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** AI-authored discovery intent only. IDs and timestamps are assigned by Trader, not the model. */
data class TraderDiscoveryStrategyDraft(
    val problemSignatures: List<ProblemSignature>,
    val target: DiscoveryTarget,
    val queries: List<DiscoveryQuery>,
    val disqualifiers: List<String>,
    val maxResultsPerSource: Int,
    val schemaVersion: Int = SCHEMA_VERSION
) {
    fun toStrategy(id: String, proofAssetId: String, createdAt: Long): DiscoveryStrategy {
        require(id.isNotBlank() && proofAssetId.isNotBlank() && createdAt >= 0)
        return DiscoveryStrategy(
            id = id,
            schemaVersion = DiscoveryStrategy.CURRENT_SCHEMA_VERSION,
            proofAssetId = proofAssetId,
            signatures = problemSignatures,
            target = target,
            queries = queries,
            disqualifiers = disqualifiers,
            maxResultsPerSource = maxResultsPerSource,
            createdAt = createdAt,
            updatedAt = createdAt
        )
    }

    companion object { const val SCHEMA_VERSION = 1 }
}

data class TraderDiscoveryStrategyParseResult(
    val draft: TraderDiscoveryStrategyDraft?,
    val issues: List<String>
) {
    val valid: Boolean get() = draft != null && issues.isEmpty()
}

/**
 * Strict parser for local-model DiscoveryStrategy drafts. It accepts raw JSON or a
 * single JSON markdown fence, rejects unknown keys and unregistered source adapters,
 * and never persists or executes the model's output.
 */
object TraderDiscoveryStrategyOutputParser {
    const val MAX_OUTPUT_CHARS = 20_000
    private const val MAX_TEXT = 4_000
    private const val MAX_LABEL = 120
    private const val MAX_LIST_ITEMS = 50
    private const val MAX_QUERIES = 50
    private const val MAX_RESULTS = 50

    private val rootKeys = setOf(
        "schemaVersion", "problemSignatures", "target", "queries", "disqualifiers", "maxResultsPerSource"
    )
    private val signatureKeys = setOf(
        "problem", "observableSignals", "buyerRoles", "searchPhrases", "mustHave"
    )
    private val targetKeys = setOf(
        "countries", "regions", "languages", "organizationTypes", "companySize", "excludedDomains"
    )
    private val queryKeys = setOf("sourceAdapterId", "query", "language", "region", "resultLimit")

    fun parse(
        raw: String,
        proofAsset: ProofAsset,
        allowedAdapterIds: Set<String>
    ): TraderDiscoveryStrategyParseResult {
        if (raw.length > MAX_OUTPUT_CHARS) return invalid("Model output exceeded the parser size limit.")
        if (allowedAdapterIds.isEmpty()) return invalid("No installed source adapters are available for strategy validation.")
        return runCatching {
            val jsonText = unwrapJsonFence(raw)
            val root = JSONObject(jsonText)
            val issues = mutableListOf<String>()
            checkKeys(root, rootKeys, "root", issues)
            val version = root.requireInt("schemaVersion")
            if (version != TraderDiscoveryStrategyDraft.SCHEMA_VERSION) issues += "Unsupported draft schema version."
            val signaturesJson = root.requireArray("problemSignatures")
            if (signaturesJson.length() !in 1..MAX_LIST_ITEMS) issues += "Draft needs 1 to 50 problem signatures."
            val signatures = List(signaturesJson.length()) { index ->
                val obj = signaturesJson.getJSONObject(index)
                checkKeys(obj, signatureKeys, "problemSignatures[$index]", issues)
                ProblemSignature(
                    id = "ai-signature-$index",
                    problem = obj.requireString("problem", MAX_TEXT),
                    observableSignals = obj.requireStringArray("observableSignals", MAX_LIST_ITEMS, MAX_LABEL),
                    buyerRoles = obj.requireStringArray("buyerRoles", MAX_LIST_ITEMS, MAX_LABEL),
                    searchPhrases = obj.requireStringArray("searchPhrases", MAX_LIST_ITEMS, MAX_LABEL),
                    mustHave = obj.requireStringArray("mustHave", MAX_LIST_ITEMS, MAX_LABEL)
                )
            }
            val targetJson = root.requireObject("target")
            checkKeys(targetJson, targetKeys, "target", issues)
            val target = DiscoveryTarget(
                countries = targetJson.requireStringArray("countries", MAX_LIST_ITEMS, MAX_LABEL),
                regions = targetJson.requireStringArray("regions", MAX_LIST_ITEMS, MAX_LABEL),
                languages = targetJson.requireStringArray("languages", MAX_LIST_ITEMS, MAX_LABEL),
                organizationTypes = targetJson.requireStringArray("organizationTypes", MAX_LIST_ITEMS, MAX_LABEL),
                companySize = targetJson.optionalString("companySize", MAX_LABEL),
                excludedDomains = targetJson.requireStringArray("excludedDomains", MAX_LIST_ITEMS, MAX_LABEL)
            )
            val queryJson = root.requireArray("queries")
            if (queryJson.length() !in 1..MAX_QUERIES) issues += "Draft needs 1 to 50 source queries."
            val queries = List(queryJson.length()) { index ->
                val obj = queryJson.getJSONObject(index)
                checkKeys(obj, queryKeys, "queries[$index]", issues)
                val adapterId = obj.requireString("sourceAdapterId", MAX_LABEL)
                if (adapterId !in allowedAdapterIds) issues += "Query[$index] uses an unregistered source adapter: $adapterId"
                DiscoveryQuery(
                    sourceAdapterId = adapterId,
                    query = obj.requireString("query", MAX_TEXT),
                    language = obj.optionalString("language", MAX_LABEL),
                    region = obj.optionalString("region", MAX_LABEL),
                    resultLimit = obj.requireInt("resultLimit")
                )
            }
            val disqualifiers = root.requireStringArray("disqualifiers", MAX_LIST_ITEMS, MAX_LABEL)
            val maxResults = root.requireInt("maxResultsPerSource")
            val draft = TraderDiscoveryStrategyDraft(
                problemSignatures = signatures,
                target = target,
                queries = queries,
                disqualifiers = disqualifiers,
                maxResultsPerSource = maxResults,
                schemaVersion = version
            )
            if (issues.isEmpty()) {
                issues += DiscoveryStrategyRules.validate(proofAsset)
                issues += DiscoveryStrategyRules.validate(
                    draft.toStrategy("ai-draft-validation", proofAsset.id, 0L),
                    listOf(proofAsset)
                )
            }
            if (issues.isNotEmpty()) TraderDiscoveryStrategyParseResult(null, issues.distinct())
            else TraderDiscoveryStrategyParseResult(draft, emptyList())
        }.getOrElse { error ->
            invalid(error.message ?: "Model output could not be parsed.")
        }
    }

    private fun unwrapJsonFence(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("```")) {
            val lines = trimmed.lines()
            require(lines.size >= 3) { "Malformed JSON markdown fence." }
            val opening = lines.first().trim().lowercase(Locale.ROOT)
            require(opening == "```json" || opening == "```") { "Only a JSON markdown fence is accepted." }
            require(lines.last().trim() == "```") { "Malformed JSON markdown fence." }
            return lines.subList(1, lines.lastIndex).joinToString("\n").trim()
        }
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            throw IllegalArgumentException("Expected one JSON object; prose and partial output are not accepted.")
        }
        return trimmed
    }
    private fun checkKeys(obj: JSONObject, allowed: Set<String>, label: String, issues: MutableList<String>) {
        val keys = obj.keys().asSequence().toSet()
        (keys - allowed).sorted().forEach { issues += "Unexpected $label field: $it" }
        (allowed - keys).sorted().forEach { issues += "Missing $label field: $it" }
    }

    private fun invalid(message: String) = TraderDiscoveryStrategyParseResult(null, listOf(message))

    private fun JSONObject.requireString(key: String, max: Int): String {
        val value = get(key)
        require(value is String) { "$key must be a string." }
        val clean = value.trim()
        require(clean.isNotEmpty() && clean.length <= max) { "$key is blank or exceeds its length limit." }
        return clean
    }

    private fun JSONObject.optionalString(key: String, max: Int): String? {
        if (!has(key) || isNull(key)) return null
        return requireString(key, max)
    }

    private fun JSONObject.requireInt(key: String): Int {
        val value = get(key)
        require(value is Number) { "$key must be an integer." }
        val number = value.toDouble()
        require(number.isFinite() && number % 1.0 == 0.0 && number in Int.MIN_VALUE.toDouble()..Int.MAX_VALUE.toDouble()) {
            "$key must be an integer."
        }
        return number.toInt()
    }

    private fun JSONObject.requireArray(key: String): JSONArray {
        val value = get(key)
        require(value is JSONArray) { "$key must be an array." }
        return value
    }

    private fun JSONObject.requireObject(key: String): JSONObject {
        val value = get(key)
        require(value is JSONObject) { "$key must be an object." }
        return value
    }

    private fun JSONObject.requireStringArray(key: String, maxItems: Int, maxLength: Int): List<String> {
        val array = requireArray(key)
        require(array.length() <= maxItems) { "$key contains too many items." }
        return List(array.length()) { index ->
            val value = array.get(index)
            require(value is String) { "$key[$index] must be a string." }
            val clean = value.trim()
            require(clean.isNotEmpty() && clean.length <= maxLength) { "$key[$index] is blank or too long." }
            clean
        }
    }
}
