package com.yusufculcu.trader.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryStrategyRulesTest {
    private val asset = ProofAsset(
        id = "asset-1",
        kind = ProofAssetKind.SERVICE_CAPABILITY,
        name = "Industrial automation",
        description = "Workflow automation and manufacturing systems integration.",
        capabilities = listOf("n8n", "Kotlin", "manufacturing workflows"),
        claims = listOf(
            ProofClaim(
                id = "claim-1",
                statement = "Built an automated prospect research workflow",
                status = ProofClaimStatus.SUPPORTED,
                evidenceIds = listOf("evidence-1"),
                verifiedAt = 1_700_000_000_000
            )
        ),
        createdAt = 1_700_000_000_000,
        updatedAt = 1_700_000_000_100
    )

    private val strategy = DiscoveryStrategy(
        id = "strategy-1",
        proofAssetId = "asset-1",
        signatures = listOf(
            ProblemSignature(
                id = "signature-1",
                problem = "Manual, fragmented supplier or sales operations",
                observableSignals = listOf("manual data entry", "spreadsheet-heavy process"),
                buyerRoles = listOf("Operations Director"),
                searchPhrases = listOf("looking to automate supplier onboarding"),
                mustHave = listOf("public evidence of the problem")
            )
        ),
        target = DiscoveryTarget(countries = listOf("PL", "DE"), languages = listOf("en", "pl")),
        queries = listOf(DiscoveryQuery("public.search", "supplier onboarding automation Poland", resultLimit = 10)),
        disqualifiers = listOf("No public business contact route"),
        maxResultsPerSource = 20,
        createdAt = 1_700_000_000_000,
        updatedAt = 1_700_000_000_100
    )

    @Test fun acceptsEvidenceBackedAssetAndBoundedStrategy() {
        assertTrue(DiscoveryStrategyRules.validate(asset).isEmpty())
        assertTrue(DiscoveryStrategyRules.validate(strategy, listOf(asset)).isEmpty())
    }

    @Test fun supportedClaimMustHaveEvidenceAndVerificationTime() {
        val invalid = asset.copy(
            claims = listOf(ProofClaim("claim-1", "A supported claim without evidence", ProofClaimStatus.SUPPORTED))
        )
        assertTrue(DiscoveryStrategyRules.validate(invalid).any { it.contains("evidence") })
    }

    @Test fun rejectsUnknownProofAssetAndUnboundedQuery() {
        val invalid = strategy.copy(
            proofAssetId = "missing",
            queries = listOf(DiscoveryQuery("public.search", "query", resultLimit = 51))
        )
        val errors = DiscoveryStrategyRules.validate(invalid, listOf(asset))
        assertTrue(errors.any { it.contains("does not exist") })
        assertTrue(errors.any { it.contains("source query") })
    }

    @Test fun rejectsDuplicateProblemSignatureIds() {
        val duplicate = strategy.copy(signatures = listOf(strategy.signatures.single(), strategy.signatures.single()))
        assertEquals(true, DiscoveryStrategyRules.validate(duplicate, listOf(asset)).any { it.contains("unique") })
    }

    @Test fun schemaVersionsAreExplicitAndRejectUnknownVersions() {
        assertTrue(DiscoveryStrategyRules.validate(asset.copy(schemaVersion = 99)).any { it.contains("schema version") })
        assertTrue(DiscoveryStrategyRules.validate(strategy.copy(schemaVersion = 99), listOf(asset)).any { it.contains("schema version") })
    }
}
