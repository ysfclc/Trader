package com.yusufculcu.trader.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yusufculcu.trader.domain.FitAssessmentDisposition
import com.yusufculcu.trader.domain.FitAssessmentOverrideDecision
import com.yusufculcu.trader.domain.FitAssessmentOutcome
import com.yusufculcu.trader.domain.FitCompanyClaim
import com.yusufculcu.trader.domain.FitEvidenceLink
import com.yusufculcu.trader.domain.FitEvidenceRelationship
import com.yusufculcu.trader.domain.ProofAsset
import com.yusufculcu.trader.domain.ProofAssetKind
import com.yusufculcu.trader.domain.ProofClaim
import com.yusufculcu.trader.domain.ProofClaimStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FitAssessmentRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val database = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val repository = FitAssessmentRepository(database)

    @After
    fun closeDatabase() { database.close() }

    @Test
    fun savesDeterministicSnapshotAndKeepsReviewerOverridesAppendOnly() = runBlocking {
        val ids = seedAssessmentData()
        val link = FitEvidenceLink(
            organizationClaimId = ids.companyClaimId,
            proofClaimId = ids.proofClaimId,
            relationship = FitEvidenceRelationship.SUPPORTS,
            rationale = "The reviewed company requirement explicitly matches this documented capability.",
            createdAt = 300
        )

        val computed = repository.assess(ids.organizationId, ids.proofAssetId, listOf(link), evaluatedAt = 400)
        assertEquals(FitAssessmentOutcome.SUPPORTED, computed.computed.outcome)

        val row = database.fitAssessmentDao()
            .findForOrganizationAndProofAsset(ids.organizationId, ids.proofAssetId)
        assertNotNull(row)
        assertEquals(FitAssessmentOutcome.SUPPORTED.name, row?.computedOutcome)
        assertNotNull(row?.computedSnapshotJson)
        assertEquals(computed.computed, repository.getView(requireNotNull(row).id)?.computed)
        assertEquals(1, database.fitAssessmentEvidenceDao().getForAssessment(row!!.id).size)

        val reviewed = repository.assess(
            ids.organizationId,
            ids.proofAssetId,
            listOf(link),
            reviewerOverride = FitReviewerOverrideDraft(
                FitAssessmentOverrideDecision.ACCEPT_FIT,
                "Reviewed the source material and accept this fit for a pilot.",
                "Operator"
            ),
            evaluatedAt = 500
        )
        assertEquals(FitAssessmentOutcome.SUPPORTED, reviewed.computed.outcome)
        assertEquals(FitAssessmentDisposition.ACCEPTED_BY_REVIEWER, reviewed.effectiveDisposition)
        assertEquals(1, database.fitAssessmentOverrideDao().observeHistory(row.id).first().size)
        assertEquals(reviewed.computed, repository.getView(row.id)?.computed)
        assertEquals(FitAssessmentDisposition.ACCEPTED_BY_REVIEWER, repository.getView(row.id)?.effectiveDisposition)

        val changedEvidence = repository.assess(
            ids.organizationId,
            ids.proofAssetId,
            listOf(link.copy(
                relationship = FitEvidenceRelationship.CONTRADICTS,
                rationale = "A later reviewed company fact contradicts this capability."
            )),
            evaluatedAt = 600
        )
        assertEquals(FitAssessmentOutcome.CONTRADICTED, changedEvidence.computed.outcome)
        assertEquals(FitAssessmentDisposition.CONTRADICTED, changedEvidence.effectiveDisposition)
        assertEquals(1, database.fitAssessmentOverrideDao().observeHistory(row.id).first().size)
    }

    @Test
    fun unreviewedCompanyClaimsRemainUnknownAndCrossCompanyLinksFailAtomically() = runBlocking {
        val ids = seedAssessmentData(companyReviewState = OrganizationClaimState.UNREVIEWED)
        val link = FitEvidenceLink(
            ids.companyClaimId, ids.proofClaimId, FitEvidenceRelationship.SUPPORTS,
            "The claim is not yet reviewed, so this relationship must remain unknown.", 300
        )
        val result = repository.assess(ids.organizationId, ids.proofAssetId, listOf(link), evaluatedAt = 400)
        assertEquals(FitAssessmentOutcome.UNKNOWN, result.computed.outcome)

        val otherOrganizationId = "other-organization"
        database.organizationDao().insert(
            OrganizationEntity(otherOrganizationId, "Other Company", null, null, null, null, null, null, 1, 1)
        )
        database.organizationSourceDao().insert(
            OrganizationSourceEntity("other-source", otherOrganizationId, OrganizationSourceKind.MANUAL_REFERENCE,
                "Other source", "https://other.example/reference", "", 2)
        )
        database.organizationClaimDao().insert(
            OrganizationClaimEntity("other-claim", otherOrganizationId, "other-source", "Capability",
                "A claim belonging to another company.", OrganizationClaimState.SUPPORTED, null, 3, 2, 3)
        )
        var rejected = false
        try {
            repository.assess(
                ids.organizationId,
                ids.proofAssetId,
                listOf(link.copy(organizationClaimId = "other-claim")),
                evaluatedAt = 500
            )
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
        assertEquals(1, database.fitAssessmentDao().observeForOrganization(ids.organizationId).first().size)
        assertEquals(0, database.fitAssessmentOverrideDao().getAll().size) // No reviewer override was created.
    }

    @Test
    fun assessmentWithoutSavedSnapshotIsNotPresentedAsAValidView() = runBlocking {
        val ids = seedAssessmentData()
        database.fitAssessmentDao().insert(
            FitAssessmentEntity(
                id = "legacy-assessment",
                organizationId = ids.organizationId,
                proofAssetId = ids.proofAssetId,
                computedOutcome = FitAssessmentOutcome.UNKNOWN.name,
                computedSnapshotJson = null,
                evaluatedAt = 10,
                createdAt = 10,
                updatedAt = 10
            )
        )
        assertNull(repository.getView("legacy-assessment"))
    }

    private suspend fun seedAssessmentData(
        companyReviewState: String = OrganizationClaimState.SUPPORTED
    ): SeedIds {
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
        val organizationId = "organization-1"
        database.organizationDao().insert(
            OrganizationEntity(organizationId, "Example Manufacturing", null, null, null, null, null, null, 1, 1)
        )
        database.organizationSourceDao().insert(
            OrganizationSourceEntity(
                "company-source", organizationId, OrganizationSourceKind.MANUAL_REFERENCE,
                "Company capability page", "https://company.example/capabilities", "Company source excerpt", 2
            )
        )
        val companyClaimId = "company-claim"
        database.organizationClaimDao().insert(
            OrganizationClaimEntity(
                companyClaimId, organizationId, "company-source", "Operational need",
                "The company is seeking workflow automation.", companyReviewState, null,
                if (companyReviewState == OrganizationClaimState.SUPPORTED) 3 else null, 2, 3
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
        return SeedIds(organizationId, asset.id, companyClaimId, asset.claims.single().id)
    }

    private data class SeedIds(
        val organizationId: String,
        val proofAssetId: String,
        val companyClaimId: String,
        val proofClaimId: String
    )
}
