package com.yusufculcu.trader.data

import com.yusufculcu.trader.domain.DiscoveryQuery
import com.yusufculcu.trader.domain.DiscoveryStrategy
import com.yusufculcu.trader.domain.DiscoveryTarget
import com.yusufculcu.trader.domain.ProblemSignature
import com.yusufculcu.trader.domain.ProofAsset
import com.yusufculcu.trader.domain.ProofAssetKind
import com.yusufculcu.trader.domain.ProofClaim
import com.yusufculcu.trader.domain.ProofClaimStatus
import org.json.JSONArray
import org.json.JSONObject

/** Stable JSON boundary for Room rows and encrypted workspace snapshots. */
object DiscoveryStrategyCodec {
    fun encode(asset: ProofAsset): String = JSONObject()
        .put("id", asset.id)
        .put("schemaVersion", asset.schemaVersion)
        .put("kind", asset.kind.name)
        .put("name", asset.name)
        .put("description", asset.description)
        .put("capabilities", strings(asset.capabilities))
        .put("claims", JSONArray().apply { asset.claims.forEach { claim ->
            put(JSONObject()
                .put("id", claim.id)
                .put("statement", claim.statement)
                .put("status", claim.status.name)
                .put("evidenceIds", strings(claim.evidenceIds))
                .put("verifiedAt", claim.verifiedAt ?: JSONObject.NULL))
        } })
        .put("createdAt", asset.createdAt)
        .put("updatedAt", asset.updatedAt)
        .toString()

    fun decodeAsset(value: String): ProofAsset {
        val o = JSONObject(value)
        val claims = o.getJSONArray("claims")
        return ProofAsset(
            id = o.getString("id"),
            schemaVersion = o.getInt("schemaVersion"),
            kind = ProofAssetKind.valueOf(o.getString("kind")),
            name = o.getString("name"),
            description = o.getString("description"),
            capabilities = readStrings(o.getJSONArray("capabilities")),
            claims = List(claims.length()) { index ->
                val c = claims.getJSONObject(index)
                ProofClaim(
                    id = c.getString("id"),
                    statement = c.getString("statement"),
                    status = ProofClaimStatus.valueOf(c.getString("status")),
                    evidenceIds = readStrings(c.getJSONArray("evidenceIds")),
                    verifiedAt = c.longOrNull("verifiedAt")
                )
            },
            createdAt = o.getLong("createdAt"),
            updatedAt = o.getLong("updatedAt")
        )
    }

    fun encode(strategy: DiscoveryStrategy): String = JSONObject()
        .put("id", strategy.id)
        .put("schemaVersion", strategy.schemaVersion)
        .put("proofAssetId", strategy.proofAssetId)
        .put("signatures", JSONArray().apply { strategy.signatures.forEach { signature ->
            put(JSONObject()
                .put("id", signature.id)
                .put("problem", signature.problem)
                .put("observableSignals", strings(signature.observableSignals))
                .put("buyerRoles", strings(signature.buyerRoles))
                .put("searchPhrases", strings(signature.searchPhrases))
                .put("mustHave", strings(signature.mustHave)))
        } })
        .put("target", JSONObject()
            .put("countries", strings(strategy.target.countries))
            .put("regions", strings(strategy.target.regions))
            .put("languages", strings(strategy.target.languages))
            .put("organizationTypes", strings(strategy.target.organizationTypes))
            .put("companySize", strategy.target.companySize ?: JSONObject.NULL)
            .put("excludedDomains", strings(strategy.target.excludedDomains)))
        .put("queries", JSONArray().apply { strategy.queries.forEach { query ->
            put(JSONObject()
                .put("sourceAdapterId", query.sourceAdapterId)
                .put("query", query.query)
                .put("language", query.language ?: JSONObject.NULL)
                .put("region", query.region ?: JSONObject.NULL)
                .put("resultLimit", query.resultLimit))
        } })
        .put("disqualifiers", strings(strategy.disqualifiers))
        .put("maxResultsPerSource", strategy.maxResultsPerSource)
        .put("createdAt", strategy.createdAt)
        .put("updatedAt", strategy.updatedAt)
        .toString()

    fun decodeStrategy(value: String): DiscoveryStrategy {
        val o = JSONObject(value)
        val signatures = o.getJSONArray("signatures")
        val target = o.getJSONObject("target")
        val queries = o.getJSONArray("queries")
        return DiscoveryStrategy(
            id = o.getString("id"),
            schemaVersion = o.getInt("schemaVersion"),
            proofAssetId = o.getString("proofAssetId"),
            signatures = List(signatures.length()) { index ->
                val s = signatures.getJSONObject(index)
                ProblemSignature(
                    id = s.getString("id"),
                    problem = s.getString("problem"),
                    observableSignals = readStrings(s.getJSONArray("observableSignals")),
                    buyerRoles = readStrings(s.getJSONArray("buyerRoles")),
                    searchPhrases = readStrings(s.getJSONArray("searchPhrases")),
                    mustHave = readStrings(s.getJSONArray("mustHave"))
                )
            },
            target = DiscoveryTarget(
                countries = readStrings(target.getJSONArray("countries")),
                regions = readStrings(target.getJSONArray("regions")),
                languages = readStrings(target.getJSONArray("languages")),
                organizationTypes = readStrings(target.getJSONArray("organizationTypes")),
                companySize = target.stringOrNull("companySize"),
                excludedDomains = readStrings(target.getJSONArray("excludedDomains"))
            ),
            queries = List(queries.length()) { index ->
                val q = queries.getJSONObject(index)
                DiscoveryQuery(
                    sourceAdapterId = q.getString("sourceAdapterId"),
                    query = q.getString("query"),
                    language = q.stringOrNull("language"),
                    region = q.stringOrNull("region"),
                    resultLimit = q.getInt("resultLimit")
                )
            },
            disqualifiers = readStrings(o.getJSONArray("disqualifiers")),
            maxResultsPerSource = o.getInt("maxResultsPerSource"),
            createdAt = o.getLong("createdAt"),
            updatedAt = o.getLong("updatedAt")
        )
    }

    private fun strings(values: List<String>) = JSONArray().apply { values.forEach { put(it) } }
    private fun readStrings(values: JSONArray) = List(values.length()) { values.getString(it) }
    private fun JSONObject.stringOrNull(key: String): String? = if (isNull(key)) null else getString(key)
    private fun JSONObject.longOrNull(key: String): Long? = if (isNull(key)) null else getLong(key)
}
