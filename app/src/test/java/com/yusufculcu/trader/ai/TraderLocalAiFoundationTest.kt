package com.yusufculcu.trader.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

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
        val invalid = TraderModelUpdatePolicy.evaluate(model, ModelSourceSnapshot("f".repeat(40), ModelArtifactMetadata(model.fileName, -1L, "bad")), now)
        assertEquals(ModelSourceStatus.ARTIFACT_UNVERIFIED, invalid.status)
    }

    @Test fun metadataResponseReaderAcceptsWithinLimitAndRejectsOversizedBody() {
        assertEquals("hello", readBoundedUtf8(ByteArrayInputStream("hello".toByteArray()), 5))
        val oversized = runCatching { readBoundedUtf8(ByteArrayInputStream("hello!".toByteArray()), 5) }
        assertTrue(oversized.isFailure)
        assertTrue(oversized.exceptionOrNull()?.message.orEmpty().contains("safety limit"))
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
}