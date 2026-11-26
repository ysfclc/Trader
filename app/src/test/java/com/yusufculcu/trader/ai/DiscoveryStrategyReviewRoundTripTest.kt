package com.yusufculcu.trader.ai

import com.yusufculcu.trader.data.DiscoveryStrategyCodec
import com.yusufculcu.trader.domain.DiscoveryQuery
import com.yusufculcu.trader.domain.DiscoveryTarget
import com.yusufculcu.trader.domain.ProblemSignature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryStrategyReviewRoundTripTest {
    @Test fun materializedReviewedDraftPreservesEveryEditableFieldThroughPersistenceCodec() {
        val proof = com.yusufculcu.trader.domain.ProofAsset(
            id = "proof-review",
            kind = com.yusufculcu.trader.domain.ProofAssetKind.SERVICE_CAPABILITY,
            name = "Precision machining",
            description = "Small batch precision components",
            capabilities = listOf("CNC", "inspection"),
            claims = emptyList(),
            createdAt = 100L,
            updatedAt = 100L
        )
        val draft = TraderDiscoveryStrategyDraft(
            problemSignatures = listOf(
                ProblemSignature(
                    id = "sig-1",
                    problem = "Supplier needs tighter tolerances",
                    observableSignals = listOf("scrap rate", "rework"),
                    buyerRoles = listOf("Quality director", "Production manager"),
                    searchPhrases = listOf("precision machining supplier"),
                    mustHave = listOf("CNC", "inspection capability")
                ),
                ProblemSignature(
                    id = "sig-2",
                    problem = "Capacity shortage",
                    observableSignals = listOf("long lead times"),
                    buyerRoles = listOf("Purchasing"),
                    searchPhrases = listOf("overflow machining capacity"),
                    mustHave = listOf("small batch")
                )
            ),
            target = DiscoveryTarget(
                countries = listOf("Poland", "Germany"),
                regions = listOf("Silesia", "Bavaria"),
                languages = listOf("Polish", "English"),
                organizationTypes = listOf("manufacturer", "tier-1 supplier"),
                companySize = "50-500 employees",
                excludedDomains = listOf("example.invalid")
            ),
            queries = listOf(
                DiscoveryQuery("duckduckgo_html", "precision machining Poland", "English", "Poland", 20),
                DiscoveryQuery("rss_atom", "manufacturing capacity", "Polish", "Silesia", 15)
            ),
            disqualifiers = listOf("No machining equipment", "No business website"),
            maxResultsPerSource = 35
        )

        val materialized = draft.toStrategy("strategy-review", proof.id, 200L)
        assertTrue(com.yusufculcu.trader.domain.DiscoveryStrategyRules.validate(materialized, listOf(proof)).isEmpty())

        val decoded = DiscoveryStrategyCodec.decodeStrategy(DiscoveryStrategyCodec.encode(materialized))
        assertEquals(materialized.signatures, decoded.signatures)
        assertEquals(materialized.target, decoded.target)
        assertEquals(materialized.queries, decoded.queries)
        assertEquals(materialized.disqualifiers, decoded.disqualifiers)
        assertEquals(materialized.maxResultsPerSource, decoded.maxResultsPerSource)
    }
}
