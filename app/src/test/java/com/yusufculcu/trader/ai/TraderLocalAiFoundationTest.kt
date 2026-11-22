package com.yusufculcu.trader.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

class TraderLocalAiFoundationTest {
    @Test fun catalogHasValidUniquePinnedModels() {
        assertTrue(TraderModelCatalog.isCatalogValid())
        assertEquals(TraderModelCatalog.entries.size, TraderModelCatalog.entries.map { it.id }.distinct().size)
        assertTrue(TraderModelCatalog.entries.all { it.pinnedUrl.startsWith("https://") && it.sha256.length == 64 })
        assertEquals(TraderModelRole.FAST, TraderModelCatalog.defaultModel().role)
        assertTrue(TraderModelCatalog.entries.all { it.publisher.isNotBlank() && it.modelPageUrl.startsWith("https://") })
        assertTrue(TraderModelCatalog.entries.all { it.sourceRevision.isNotBlank() && it.licenseStatus.isNotBlank() })
        assertTrue(TraderModelCatalog.entries.all { it.strengths.isNotEmpty() && it.limitations.isNotEmpty() && it.deviceGuidance.isNotBlank() })
    }

    @Test fun sourceUpdatePolicyRequiresArtifactSizeAndDigestReview() {
        val model = TraderModelCatalog.defaultModel()
        val now = 1234L
        val pinnedArtifact = ModelArtifactMetadata(model.fileName, model.sizeBytes, model.sha256)
        val current = TraderModelUpdatePolicy.evaluate(model, ModelSourceSnapshot(model.sourceRevision.padEnd(40, '0'), pinnedArtifact), now)
        assertEquals(ModelSourceStatus.CURRENT, current.status)
        val candidateArtifact = ModelArtifactMetadata(model.fileName, model.sizeBytes + 1L, "f".repeat(64))
        val candidate = TraderModelUpdatePolicy.evaluate(model, ModelSourceSnapshot("f".repeat(40), candidateArtifact), now)
        assertEquals(ModelSourceStatus.CANDIDATE_REVIEW_REQUIRED, candidate.status)
        assertTrue(candidate.detail.contains("not installable"))
        assertEquals(model.sizeBytes + 1L, candidate.candidateSizeBytes)
        assertEquals("f".repeat(64), candidate.candidateSha256)
        val missing = TraderModelUpdatePolicy.evaluate(model, ModelSourceSnapshot("f".repeat(40), null), now)
        assertEquals(ModelSourceStatus.ARTIFACT_MISSING, missing.status)
        val malformedMetadata = TraderModelUpdatePolicy.evaluate(
            model,
            ModelSourceSnapshot("f".repeat(40), null, ModelArtifactLookupStatus.UNVERIFIED),
            now
        )
        assertEquals(ModelSourceStatus.ARTIFACT_UNVERIFIED, malformedMetadata.status)
        val invalid = TraderModelUpdatePolicy.evaluate(model, ModelSourceSnapshot("f".repeat(40), ModelArtifactMetadata(model.fileName, -1L, "bad")), now)
        assertEquals(ModelSourceStatus.ARTIFACT_UNVERIFIED, invalid.status)
    }

    @Test fun metadataResponseReaderAcceptsWithinLimitAndRejectsOversizedBody() {
        assertEquals("hello", readBoundedUtf8(ByteArrayInputStream("hello".toByteArray()), 5))
        val oversized = runCatching { readBoundedUtf8(ByteArrayInputStream("hello!".toByteArray()), 5) }
        assertTrue(oversized.isFailure)
        assertTrue(oversized.exceptionOrNull()?.message.orEmpty().contains("safety limit"))
    }

    @Test fun candidateByteVerifierCopiesAndHashesExactBytes() {
        val bytes = "verified candidate bytes".toByteArray()
        val expected = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        val output = ByteArrayOutputStream()
        val actual = copyCandidateAndHash(
            ByteArrayInputStream(bytes), output, bytes.size.toLong(), expected, shouldCancel = { false }
        )
        assertEquals(expected, actual)
        assertTrue(output.toByteArray().contentEquals(bytes))
    }

    @Test fun candidateByteVerifierRejectsDigestMismatchAndWrongSize() {
        val bytes = "candidate".toByteArray()
        val wrongDigest = runCatching {
            copyCandidateAndHash(
                ByteArrayInputStream(bytes), ByteArrayOutputStream(), bytes.size.toLong(), "0".repeat(64),
                shouldCancel = { false }
            )
        }
        assertTrue(wrongDigest.isFailure)
        assertTrue(wrongDigest.exceptionOrNull()?.message.orEmpty().contains("SHA-256"))

        val tooLong = runCatching {
            copyCandidateAndHash(
                ByteArrayInputStream(bytes), ByteArrayOutputStream(), bytes.size.toLong() - 1L,
                "0".repeat(64), shouldCancel = { false }
            )
        }
        assertTrue(tooLong.isFailure)
        assertTrue(tooLong.exceptionOrNull()?.message.orEmpty().contains("exceeded"))
        val tooShort = runCatching {
            copyCandidateAndHash(
                ByteArrayInputStream(bytes), ByteArrayOutputStream(), bytes.size.toLong() + 1L,
                "0".repeat(64), shouldCancel = { false }
            )
        }
        assertTrue(tooShort.isFailure)
        assertTrue(tooShort.exceptionOrNull()?.message.orEmpty().contains("Incomplete"))
    }

    @Test fun candidateByteVerifierHonorsCancellation() {
        val cancelled = runCatching {
            copyCandidateAndHash(
                ByteArrayInputStream("candidate".toByteArray()), ByteArrayOutputStream(), 9L,
                "0".repeat(64), shouldCancel = { true }
            )
        }
        assertTrue(cancelled.exceptionOrNull() is CandidateVerificationCancelledException)
    }

    @Test fun contextPacketLabelsHypothesesAndPreservesSourceLinks() {
        val rendered = TraderContextPacket(
            task = "Assess company",
            facts = listOf(
                TraderContextFact("Hiring signal", "Automation role", TraderFactStatus.SOURCE_UNVERIFIED, "https://example.com/jobs"),
                TraderContextFact("Potential pain", "Manual processes", TraderFactStatus.AI_HYPOTHESIS),
            ),
            unknowns = listOf("Current process unknown"),
        ).render()
        assertTrue(rendered.contains("SOURCE_UNVERIFIED"))
        assertTrue(rendered.contains("AI_HYPOTHESIS"))
        assertTrue(rendered.contains("https://example.com/jobs"))
        assertTrue(rendered.contains("Current process unknown"))
    }

    @Test fun contextPacketHonorsBoundedBudget() {
        val rendered = TraderContextPacket(task = "x", sections = mapOf("Large" to "a".repeat(5000))).render(256)
        assertTrue(rendered.length <= 256)
        assertTrue(rendered.contains("Context budget reached"))
    }
    private fun validDiscoveryDraftJson(adapterId: String = "duckduckgo_html"): String = """
        {
          "schemaVersion": 1,
          "problemSignatures": [{
            "problem": "Needs custom precision machining",
            "observableSignals": ["tight tolerance"],
            "buyerRoles": ["engineering"],
            "searchPhrases": ["precision machining supplier"],
            "mustHave": ["ISO certification"]
          }],
          "target": {
            "countries": ["Poland"],
            "regions": [],
            "languages": ["English"],
            "organizationTypes": ["manufacturer"],
            "companySize": null,
            "excludedDomains": []
          },
          "queries": [{
            "sourceAdapterId": "$adapterId",
            "query": "precision machining supplier Poland",
            "language": "English",
            "region": "Poland",
            "resultLimit": 10
          }],
          "disqualifiers": ["No machining capability"],
          "maxResultsPerSource": 10
        }
    """.trimIndent()

    private fun parserProofAsset() = com.yusufculcu.trader.domain.ProofAsset(
        id = "proof-1",
        kind = com.yusufculcu.trader.domain.ProofAssetKind.SERVICE_CAPABILITY,
        name = "Machining",
        description = "Precision machining",
        capabilities = listOf("Machining"),
        claims = emptyList(),
        createdAt = 1L,
        updatedAt = 1L
    )

    @Test fun discoveryStrategyParserAcceptsBoundedDraftAndAppAssignsIdentity() {
        val result = TraderDiscoveryStrategyOutputParser.parse(
            validDiscoveryDraftJson(), parserProofAsset(), setOf("duckduckgo_html", "rss_atom")
        )
        assertTrue(result.valid)
        val draft = result.draft!!
        assertEquals("Needs custom precision machining", draft.problemSignatures.single().problem)
        assertEquals("duckduckgo_html", draft.queries.single().sourceAdapterId)
        val savedShape = draft.toStrategy("strategy-real-id", "proof-1", 1234L)
        assertEquals("strategy-real-id", savedShape.id)
        assertEquals("proof-1", savedShape.proofAssetId)
        assertEquals(1234L, savedShape.createdAt)
    }

    @Test fun discoveryStrategyParserAcceptsSingleJsonFence() {
        val fenced = "```json\n" + validDiscoveryDraftJson() + "\n```"
        val result = TraderDiscoveryStrategyOutputParser.parse(fenced, parserProofAsset(), setOf("duckduckgo_html"))
        assertTrue(result.valid)
    }

    @Test fun discoveryStrategyParserRejectsUnregisteredAdapterAndUnknownFields() {
        val unknownAdapter = TraderDiscoveryStrategyOutputParser.parse(
            validDiscoveryDraftJson("not_registered"), parserProofAsset(), setOf("duckduckgo_html")
        )
        assertTrue(!unknownAdapter.valid)
        assertTrue(unknownAdapter.issues.any { it.contains("unregistered source adapter") })
        val unknownFieldJson = validDiscoveryDraftJson().replace(
            "\"maxResultsPerSource\": 10",
            "\"maxResultsPerSource\": 10, \"executeImmediately\": true"
        )
        val unknownField = TraderDiscoveryStrategyOutputParser.parse(
            unknownFieldJson, parserProofAsset(), setOf("duckduckgo_html")
        )
        assertTrue(!unknownField.valid)
        assertTrue(unknownField.issues.any { it.contains("Unexpected root field") })
    }

    @Test fun discoveryStrategyParserRejectsMalformedOrOverBudgetOutput() {
        val malformed = TraderDiscoveryStrategyOutputParser.parse(
            "Here is the plan: {}", parserProofAsset(), setOf("duckduckgo_html")
        )
        assertTrue(!malformed.valid)
        val oversized = TraderDiscoveryStrategyOutputParser.parse(
            "x".repeat(TraderDiscoveryStrategyOutputParser.MAX_OUTPUT_CHARS + 1),
            parserProofAsset(), setOf("duckduckgo_html")
        )
        assertTrue(!oversized.valid)
        assertTrue(oversized.issues.single().contains("size limit"))
    }

}