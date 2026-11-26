package com.yusufculcu.trader.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yusufculcu.trader.domain.FitAssessmentDisposition
import com.yusufculcu.trader.domain.FitAssessmentOverrideDecision
import com.yusufculcu.trader.domain.FitEvidenceLink
import com.yusufculcu.trader.domain.FitEvidenceRelationship
import com.yusufculcu.trader.domain.ProofAsset
import com.yusufculcu.trader.domain.ProofAssetKind
import com.yusufculcu.trader.domain.ProofClaim
import com.yusufculcu.trader.domain.ProofClaimStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
class FitAssessmentBackupTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun encryptedSnapshotV12PreservesComputedFitEvidenceAndAppendOnlyReviewerHistory() = runBlocking {
        val source = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java)
            .allowMainThreadQueries().build()
        val target = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            seed(source)
            val sourceRepository = FitAssessmentRepository(source)
            val link = FitEvidenceLink(
                organizationClaimId = "company-claim",
                proofClaimId = "proof-claim",
                relationship = FitEvidenceRelationship.SUPPORTS,
                rationale = "The source-backed company requirement matches the documented capability.",
                createdAt = 30
            )
            val before = sourceRepository.assess(
                "organization-1",
                "proof-asset",
                listOf(link),
                reviewerOverride = FitReviewerOverrideDraft(
                    FitAssessmentOverrideDecision.ACCEPT_FIT,
                    "Human reviewer accepts this fit for a scoped pilot.",
                    "Operator"
                ),
                evaluatedAt = 40
            )
            assertEquals(FitAssessmentDisposition.ACCEPTED_BY_REVIEWER, before.effectiveDisposition)

            val output = ByteArrayOutputStream()
            TraderBackupManager(source).exportEncrypted(output, "correct horse battery".toCharArray())
            TraderBackupManager(target).restoreEncrypted(
                ByteArrayInputStream(output.toByteArray()),
                "correct horse battery".toCharArray()
            )

            val restoredAssessment = target.fitAssessmentDao().getAll().single()
            assertEquals("organization-1", restoredAssessment.organizationId)
            assertEquals("proof-asset", restoredAssessment.proofAssetId)
            assertNotNull(restoredAssessment.computedSnapshotJson)
            assertEquals(1, target.fitAssessmentEvidenceDao().getForAssessment(restoredAssessment.id).size)
            assertEquals(1, target.fitAssessmentOverrideDao().getAll().size)
            val after = FitAssessmentRepository(target).getView(restoredAssessment.id)
            assertEquals(before.computed, after?.computed)
            assertEquals(FitAssessmentDisposition.ACCEPTED_BY_REVIEWER, after?.effectiveDisposition)
        } finally {
            source.close()
            target.close()
        }
    }

    private suspend fun seed(database: TraderDatabase) {
        database.problemDao().insert(
            ProblemEntity(
                id = "proof-case", title = "Proof evidence", description = "", desiredOutcome = "",
                constraintsText = "", category = "Research", status = "OPEN", inputKind = "manual",
                originalContent = "", sourceUri = null, outcomeNote = "", createdAt = 1, updatedAt = 1
            )
        )
        database.evidenceDao().insert(
            EvidenceEntity(
                id = "proof-evidence", problemId = "proof-case", title = "Verified project record",
                content = "A source-backed project record.", sourceUri = "https://proof.example/evidence",
                sourceKind = "URL", verificationState = "VERIFIED", capturedAt = 2
            )
        )
        database.organizationDao().insert(
            OrganizationEntity("organization-1", "Example Manufacturing", null, null, null, null, null, null, 1, 1)
        )
        database.organizationSourceDao().insert(
            OrganizationSourceEntity(
                "company-source", "organization-1", OrganizationSourceKind.MANUAL_REFERENCE,
                "Company capability page", "https://company.example/capabilities", "Company source excerpt", 2
            )
        )
        database.organizationClaimDao().insert(
            OrganizationClaimEntity(
                "company-claim", "organization-1", "company-source", "Operational need",
                "The company is seeking workflow automation.", OrganizationClaimState.SUPPORTED, null, 3, 2, 3
            )
        )
        val asset = ProofAsset(
            id = "proof-asset",
            kind = ProofAssetKind.SERVICE_CAPABILITY,
            name = "Workflow automation",
            description = "Documented workflow automation capability.",
            capabilities = listOf("Workflow automation"),
            claims = listOf(
                ProofClaim("proof-claim", "Built a workflow automation system.",
                    ProofClaimStatus.SUPPORTED, listOf("proof-evidence"), 4)
            ),
            createdAt = 1,
            updatedAt = 2
        )
        database.proofAssetDao().insert(
            ProofAssetEntity(
                id = asset.id, schemaVersion = asset.schemaVersion, kind = asset.kind.name,
                payloadJson = DiscoveryStrategyCodec.encode(asset), createdAt = asset.createdAt,
                updatedAt = asset.updatedAt
            )
        )
    }
}
