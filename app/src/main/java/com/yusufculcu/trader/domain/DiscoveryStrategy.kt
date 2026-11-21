package com.yusufculcu.trader.domain

/**
 * Versioned, user-owned proof that Trader may use to explain why a discovery
 * strategy is relevant. Claims remain separate from their evidence and
 * verification state; a description alone is never treated as verified proof.
 */
enum class ProofAssetKind {
    PRODUCT, WORKING_PROJECT, SERVICE_CAPABILITY, DOMAIN_SKILL, DOCUMENTED_RESULT, OTHER
}

enum class ProofClaimStatus {
    UNVERIFIED, SUPPORTED, CONTRADICTED, EXPIRED
}

data class ProofClaim(
    val id: String,
    val statement: String,
    val status: ProofClaimStatus = ProofClaimStatus.UNVERIFIED,
    val evidenceIds: List<String> = emptyList(),
    val verifiedAt: Long? = null
)

data class ProofAsset(
    val id: String,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val kind: ProofAssetKind,
    val name: String,
    val description: String,
    val capabilities: List<String>,
    val claims: List<ProofClaim>,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

data class ProblemSignature(
    val id: String,
    val problem: String,
    val observableSignals: List<String>,
    val buyerRoles: List<String> = emptyList(),
    val searchPhrases: List<String> = emptyList(),
    val mustHave: List<String> = emptyList()
)

data class DiscoveryTarget(
    val countries: List<String> = emptyList(),
    val regions: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val organizationTypes: List<String> = emptyList(),
    val companySize: String? = null,
    val excludedDomains: List<String> = emptyList()
)

data class DiscoveryQuery(
    val sourceAdapterId: String,
    val query: String,
    val language: String? = null,
    val region: String? = null,
    val resultLimit: Int = 10
)

/**
 * Editable discovery intent. This is a plan, not permission to crawl or
 * contact anyone. Each source adapter still enforces its own access policy.
 */
data class DiscoveryStrategy(
    val id: String,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val proofAssetId: String,
    val signatures: List<ProblemSignature>,
    val target: DiscoveryTarget = DiscoveryTarget(),
    val queries: List<DiscoveryQuery>,
    val disqualifiers: List<String> = emptyList(),
    val maxResultsPerSource: Int = 25,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

object DiscoveryStrategyRules {
    private const val MAX_TEXT = 4_000
    private const val MAX_LABEL = 120
    private const val MAX_LIST_ITEMS = 50
    private const val MAX_QUERIES = 50
    private const val MAX_RESULTS_PER_QUERY = 50

    fun validate(asset: ProofAsset): List<String> = buildList {
        if (asset.id.isBlank() || asset.id.length > 128) add("Proof asset identifier is invalid.")
        if (asset.schemaVersion != ProofAsset.CURRENT_SCHEMA_VERSION) add("Proof asset schema version is unsupported.")
        if (asset.name.clean(MAX_LABEL).isBlank()) add("Proof asset name is required.")
        if (asset.description.length > MAX_TEXT) add("Proof asset description is too long.")
        if (asset.capabilities.size > MAX_LIST_ITEMS) add("Too many proof asset capabilities.")
        if (asset.capabilities.any { it.clean(MAX_LABEL).isBlank() }) add("Proof asset capabilities cannot be blank.")
        if (asset.claims.size > MAX_LIST_ITEMS) add("Too many proof claims.")
        if (asset.claims.any { it.id.isBlank() || it.id.length > 128 || it.statement.clean(MAX_TEXT).isBlank() }) {
            add("Each proof claim needs a valid identifier and statement.")
        }
        if (asset.claims.map { it.id }.distinct().size != asset.claims.size) add("Proof claim identifiers must be unique.")
        if (asset.claims.any { it.evidenceIds.size > MAX_LIST_ITEMS || it.evidenceIds.any(String::isBlank) }) {
            add("Proof claim evidence references are invalid.")
        }
        if (asset.claims.any { it.status == ProofClaimStatus.SUPPORTED && (it.evidenceIds.isEmpty() || it.verifiedAt == null) }) {
            add("A supported proof claim needs evidence and a verification timestamp.")
        }
        if (asset.createdAt < 0 || asset.updatedAt < asset.createdAt) add("Proof asset timestamps are invalid.")
    }

    fun validate(strategy: DiscoveryStrategy, assets: Collection<ProofAsset>): List<String> = buildList {
        if (strategy.id.isBlank() || strategy.id.length > 128) add("Discovery strategy identifier is invalid.")
        if (strategy.schemaVersion != DiscoveryStrategy.CURRENT_SCHEMA_VERSION) add("Discovery strategy schema version is unsupported.")
        if (assets.none { it.id == strategy.proofAssetId }) add("The selected proof asset does not exist.")
        if (strategy.signatures.isEmpty() || strategy.signatures.size > MAX_LIST_ITEMS) add("A strategy needs 1 to 50 problem signatures.")
        if (strategy.signatures.any { it.id.isBlank() || it.problem.clean(MAX_TEXT).isBlank() }) {
            add("Each problem signature needs an identifier and problem statement.")
        }
        if (strategy.signatures.map { it.id }.distinct().size != strategy.signatures.size) add("Problem signature identifiers must be unique.")
        strategy.signatures.forEach { signature ->
            val groups = listOf(signature.observableSignals, signature.buyerRoles, signature.searchPhrases, signature.mustHave)
            if (groups.any { it.size > MAX_LIST_ITEMS || it.any { value -> value.clean(MAX_LABEL).isBlank() } }) {
                add("Problem signature lists contain blank or excessive values.")
            }
        }
        if (strategy.queries.isEmpty() || strategy.queries.size > MAX_QUERIES) add("A strategy needs 1 to 50 bounded source queries.")
        if (strategy.queries.any {
                it.sourceAdapterId.isBlank() || it.sourceAdapterId.length > MAX_LABEL ||
                    it.query.clean(MAX_TEXT).isBlank() || it.resultLimit !in 1..MAX_RESULTS_PER_QUERY
            }) add("A source query is invalid or exceeds its result limit.")
        if (strategy.disqualifiers.size > MAX_LIST_ITEMS || strategy.disqualifiers.any { it.clean(MAX_LABEL).isBlank() }) {
            add("Strategy disqualifiers contain blank or excessive values.")
        }
        if (strategy.maxResultsPerSource !in 1..MAX_RESULTS_PER_QUERY) add("Per-source result limit must be between 1 and 50.")
        val targetGroups = listOf(
            strategy.target.countries, strategy.target.regions, strategy.target.languages,
            strategy.target.organizationTypes, strategy.target.excludedDomains
        )
        if (targetGroups.any { it.size > MAX_LIST_ITEMS || it.any { value -> value.clean(MAX_LABEL).isBlank() } }) {
            add("Target constraints contain blank or excessive values.")
        }
        if (strategy.target.companySize?.length ?: 0 > MAX_LABEL) add("Company-size constraint is too long.")
        if (strategy.createdAt < 0 || strategy.updatedAt < strategy.createdAt) add("Discovery strategy timestamps are invalid.")
    }

    private fun String.clean(limit: Int): String = trim().take(limit)
}
