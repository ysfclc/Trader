package com.yusufculcu.trader.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FitAssessmentTest {
    private val organizationId = "company-1"
    private val asset = ProofAsset(
        id = "proof-asset-1",
        kind = ProofAssetKind.SERVICE_CAPABILITY,
        name = "Manufacturing workflow automation",
        description = "Documented process automation capability.",
        capabilities = listOf("Workflow automation", "Manufacturing operations"),
        claims = listOf(
            ProofClaim("proof-claim-1", "Built a workflow automation system", ProofClaimStatus.SUPPORTED, listOf("evidence-1"), 100),
            ProofClaim("proof-claim-2", "Integrated manufacturing data systems", ProofClaimStatus.SUPPORTED, listOf("evidence-2"), 100)
        ),
        createdAt = 10,
        updatedAt = 20
    )
    private val companyClaim = FitCompanyClaim(
        "company-claim-1", organizationId, "Operational need",
        "The company is seeking workflow automation.", FitCompanyClaimReviewState.SUPPORTED
    )
    private val secondCompanyClaim = FitCompanyClaim(
        "company-claim-2", organizationId, "Systems",
        "The company has fragmented manufacturing data systems.", FitCompanyClaimReviewState.SUPPORTED
    )

    private fun link(
        companyClaimId: String = companyClaim.id,
        proofClaimId: String = "proof-claim-1",
        relationship: FitEvidenceRelationship = FitEvidenceRelationship.SUPPORTS,
        rationale: String = "The reviewed company evidence explicitly describes this need."
    ) = FitEvidenceLink(companyClaimId, proofClaimId, relationship, rationale, createdAt = 200)

    @Test
    fun fullySupportedAssessmentRequiresVerifiedClaimsOnBothSides() {
        val result = FitAssessmentEvaluator.evaluate(
            organizationId, asset, listOf(companyClaim, secondCompanyClaim),
            listOf(link(), link(secondCompanyClaim.id, "proof-claim-2")), 300
        )
        assertEquals(FitAssessmentOutcome.SUPPORTED, result.computed.outcome)
        assertEquals(FitAssessmentCounts(2, 0, 0, 0), result.computed.counts)
        assertEquals(FitAssessmentDispositionSource.COMPUTED, result.dispositionSource)
        assertEquals(FitAssessmentDisposition.SUPPORTED, result.effectiveDisposition)
    }

    @Test
    fun missingEvidenceRemainsUnknownAndProducesPartialOutcome() {
        val result = FitAssessmentEvaluator.evaluate(
            organizationId, asset, listOf(companyClaim), listOf(link()), 300
        )
        assertEquals(FitAssessmentOutcome.PARTIAL, result.computed.outcome)
        assertEquals(FitComponentOutcome.UNKNOWN, result.computed.components.last().outcome)
        assertEquals("No company evidence is linked to this proof claim.", result.computed.components.last().unknownReason)
    }

    @Test
    fun contradictedEvidenceIsNotCancelledOutByASeparateSupportedComponent() {
        val result = FitAssessmentEvaluator.evaluate(
            organizationId, asset, listOf(companyClaim, secondCompanyClaim),
            listOf(
                link(),
                link(secondCompanyClaim.id, "proof-claim-2", FitEvidenceRelationship.CONTRADICTS,
                    "The reviewed company requirement explicitly conflicts with this proof claim.")
            ), 300
        )
        assertEquals(FitAssessmentOutcome.CONTRADICTED, result.computed.outcome)
        assertEquals(1, result.computed.counts.supported)
        assertEquals(1, result.computed.counts.contradicted)
    }

    @Test
    fun supportingAndContradictingEvidenceForSameProofClaimIsConflict() {
        val oneClaimAsset = asset.copy(claims = asset.claims.take(1))
        val result = FitAssessmentEvaluator.evaluate(
            organizationId, oneClaimAsset, listOf(companyClaim, secondCompanyClaim),
            listOf(
                link(),
                link(secondCompanyClaim.id, relationship = FitEvidenceRelationship.CONTRADICTS,
                    rationale = "A second reviewed company fact conflicts with the proposed fit.")
            ), 300
        )
        assertEquals(FitAssessmentOutcome.CONFLICTED, result.computed.outcome)
        assertEquals(FitComponentOutcome.CONFLICTED, result.computed.components.single().outcome)
        assertEquals(1, result.computed.components.single().supportingEvidence.size)
        assertEquals(1, result.computed.components.single().contradictingEvidence.size)
    }

    @Test
    fun unsupportedCompanyClaimCannotBecomePositiveOrNegativeFitEvidence() {
        val unreviewed = companyClaim.copy(reviewState = FitCompanyClaimReviewState.UNREVIEWED)
        val result = FitAssessmentEvaluator.evaluate(
            organizationId, asset.copy(claims = asset.claims.take(1)), listOf(unreviewed), listOf(link()), 300
        )
        assertEquals(FitAssessmentOutcome.UNKNOWN, result.computed.outcome)
        val finding = result.computed.components.single().unknownEvidence.single()
        assertEquals(FitEvidenceFindingState.UNKNOWN, finding.effectiveState)
        assertEquals("Company claim is not source-supported.", finding.eligibilityReason)
    }

    @Test
    fun unverifiedOrExpiredProofClaimCannotEstablishFit() {
        val unverifiedAsset = asset.copy(
            claims = listOf(asset.claims.first().copy(status = ProofClaimStatus.EXPIRED, verifiedAt = null))
        )
        val result = FitAssessmentEvaluator.evaluate(
            organizationId, unverifiedAsset, listOf(companyClaim), listOf(link()), 300
        )
        assertEquals(FitAssessmentOutcome.UNKNOWN, result.computed.outcome)
        assertEquals(
            "Proof claim is not currently evidence-supported.",
            result.computed.components.single().unknownEvidence.single().eligibilityReason
        )
    }

    @Test
    fun reviewerOverrideIsSeparateAndDoesNotMutateComputedOutcome() {
        val override = FitAssessmentOverride(
            "review-1", FitAssessmentOverrideDecision.ACCEPT_FIT,
            "I reviewed the underlying sources and accept this partial fit for follow-up.", 400
        )
        val result = FitAssessmentEvaluator.evaluate(
            organizationId, asset, listOf(companyClaim), listOf(link()), 300, override
        )
        assertEquals(FitAssessmentOutcome.PARTIAL, result.computed.outcome)
        assertEquals(override, result.reviewerOverride)
        assertEquals(FitAssessmentDispositionSource.REVIEWER_OVERRIDE, result.dispositionSource)
        assertEquals(FitAssessmentDisposition.ACCEPTED_BY_REVIEWER, result.effectiveDisposition)
    }

    @Test
    fun evaluatorOutputIsStableWhenInputsAreReordered() {
        val claims = listOf(companyClaim, secondCompanyClaim)
        val links = listOf(
            link(),
            link(secondCompanyClaim.id, "proof-claim-2", FitEvidenceRelationship.UNKNOWN, "")
        )
        val first = FitAssessmentEvaluator.evaluate(organizationId, asset, claims, links, 300)
        val second = FitAssessmentEvaluator.evaluate(organizationId, asset, claims.reversed(), links.reversed(), 300)
        assertEquals(first, second)
    }

    @Test
    fun invalidCrossCompanyDuplicateAndUnexplainedEvidenceLinksFailClosed() {
        val crossCompany = companyClaim.copy(organizationId = "another-company")
        val duplicate = link()
        val errors = FitAssessmentRules.validate(
            organizationId, asset, listOf(crossCompany),
            listOf(duplicate, duplicate, duplicate.copy(rationale = "")), 300
        )
        assertTrue(errors.any { it.contains("different company") })
        assertTrue(errors.any { it.contains("Duplicate") })
        assertTrue(errors.any { it.contains("rationale") })
    }

    @Test
    fun invalidReviewerOverrideIsRejected() {
        val errors = FitAssessmentRules.validate(
            organizationId, asset, listOf(companyClaim), listOf(link()), 300,
            FitAssessmentOverride("review-1", FitAssessmentOverrideDecision.REJECT_FIT, " ", 400)
        )
        assertTrue(errors.any { it.contains("override needs a rationale") })
    }
}
