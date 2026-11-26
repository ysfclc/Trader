package com.yusufculcu.trader.domain

enum class FitCompanyClaimReviewState {
    UNREVIEWED, SUPPORTED, CONTRADICTED, REJECTED
}

enum class FitEvidenceRelationship {
    SUPPORTS, CONTRADICTS, UNKNOWN
}

enum class FitEvidenceFindingState {
    SUPPORTS, CONTRADICTS, UNKNOWN
}

enum class FitComponentOutcome {
    SUPPORTED, CONTRADICTED, CONFLICTED, UNKNOWN
}

enum class FitAssessmentOutcome {
    SUPPORTED, PARTIAL, CONTRADICTED, CONFLICTED, UNKNOWN
}

enum class FitAssessmentOverrideDecision {
    ACCEPT_FIT, REJECT_FIT, REQUEST_MORE_EVIDENCE
}

enum class FitAssessmentDisposition {
    SUPPORTED, PARTIAL, CONTRADICTED, CONFLICTED, UNKNOWN,
    ACCEPTED_BY_REVIEWER, REJECTED_BY_REVIEWER, MORE_EVIDENCE_REQUESTED
}

enum class FitAssessmentDispositionSource {
    COMPUTED, REVIEWER_OVERRIDE
}

/**
 * The repository maps source-linked OrganizationClaimEntity rows into this
 * domain input. A claim is eligible only after its own source review is SUPPORTED.
 */
data class FitCompanyClaim(
    val id: String,
    val organizationId: String,
    val field: String,
    val statement: String,
    val reviewState: FitCompanyClaimReviewState
)

/**
 * The operator explicitly labels how a reviewed company claim relates to a
 * verified proof claim. Trader never infers semantic compatibility from text.
 */
data class FitEvidenceLink(
    val organizationClaimId: String,
    val proofClaimId: String,
    val relationship: FitEvidenceRelationship,
    val rationale: String = "",
    val createdAt: Long
)

/** A reviewer decision is independent of, and never overwrites, the computed result. */
data class FitAssessmentOverride(
    val id: String,
    val decision: FitAssessmentOverrideDecision,
    val rationale: String,
    val reviewedAt: Long
)

data class FitAssessmentEvidenceFinding(
    val organizationClaimId: String,
    val organizationClaimField: String,
    val organizationClaimStatement: String,
    val proofClaimId: String,
    val proofClaimStatement: String,
    val declaredRelationship: FitEvidenceRelationship,
    val effectiveState: FitEvidenceFindingState,
    val eligibilityReason: String?
)

data class FitAssessmentComponent(
    val proofClaimId: String,
    val proofClaimStatement: String,
    val outcome: FitComponentOutcome,
    val supportingEvidence: List<FitAssessmentEvidenceFinding>,
    val contradictingEvidence: List<FitAssessmentEvidenceFinding>,
    val unknownEvidence: List<FitAssessmentEvidenceFinding>,
    val unknownReason: String?
)

data class FitAssessmentCounts(
    val supported: Int,
    val contradicted: Int,
    val conflicted: Int,
    val unknown: Int
)

data class FitAssessmentResult(
    val organizationId: String,
    val proofAssetId: String,
    val evaluatedAt: Long,
    val outcome: FitAssessmentOutcome,
    val components: List<FitAssessmentComponent>,
    val counts: FitAssessmentCounts
)

/**
 * The override remains visibly separate from deterministic computation.
 * Persistence and UI are intentionally deferred to the next T4.9 slice.
 */
data class FitAssessmentView(
    val computed: FitAssessmentResult,
    val reviewerOverride: FitAssessmentOverride?
) {
    val effectiveDisposition: FitAssessmentDisposition
        get() = when (reviewerOverride?.decision) {
            FitAssessmentOverrideDecision.ACCEPT_FIT -> FitAssessmentDisposition.ACCEPTED_BY_REVIEWER
            FitAssessmentOverrideDecision.REJECT_FIT -> FitAssessmentDisposition.REJECTED_BY_REVIEWER
            FitAssessmentOverrideDecision.REQUEST_MORE_EVIDENCE -> FitAssessmentDisposition.MORE_EVIDENCE_REQUESTED
            null -> when (computed.outcome) {
                FitAssessmentOutcome.SUPPORTED -> FitAssessmentDisposition.SUPPORTED
                FitAssessmentOutcome.PARTIAL -> FitAssessmentDisposition.PARTIAL
                FitAssessmentOutcome.CONTRADICTED -> FitAssessmentDisposition.CONTRADICTED
                FitAssessmentOutcome.CONFLICTED -> FitAssessmentDisposition.CONFLICTED
                FitAssessmentOutcome.UNKNOWN -> FitAssessmentDisposition.UNKNOWN
            }
        }

    val dispositionSource: FitAssessmentDispositionSource
        get() = if (reviewerOverride == null) {
            FitAssessmentDispositionSource.COMPUTED
        } else {
            FitAssessmentDispositionSource.REVIEWER_OVERRIDE
        }
}

object FitAssessmentRules {
    private const val MAX_COMPANY_CLAIMS = 500
    private const val MAX_EVIDENCE_LINKS = 250
    private const val MAX_RATIONALE_LENGTH = 1_000

    fun validate(
        organizationId: String,
        asset: ProofAsset,
        companyClaims: Collection<FitCompanyClaim>,
        evidenceLinks: Collection<FitEvidenceLink>,
        evaluatedAt: Long,
        reviewerOverride: FitAssessmentOverride? = null
    ): List<String> = buildList {
        if (organizationId.isBlank() || organizationId.length > 128) {
            add("Company identifier is invalid.")
        }
        addAll(DiscoveryStrategyRules.validate(asset))
        if (asset.claims.isEmpty()) add("A fit assessment requires at least one proof claim.")
        if (companyClaims.size > MAX_COMPANY_CLAIMS) add("Too many company claims for one fit assessment.")
        if (companyClaims.map { it.id }.distinct().size != companyClaims.size) {
            add("Company claim identifiers must be unique.")
        }
        companyClaims.forEach { claim ->
            if (claim.id.isBlank() || claim.id.length > 128) add("A company claim identifier is invalid.")
            if (claim.organizationId != organizationId) add("A company claim belongs to a different company.")
            if (claim.field.isBlank() || claim.field.length > 80) add("A company claim field is invalid.")
            if (claim.statement.isBlank() || claim.statement.length > 2_000) add("A company claim statement is invalid.")
        }
        if (evidenceLinks.size > MAX_EVIDENCE_LINKS) add("Too many evidence links for one fit assessment.")
        val validCompanyIds = companyClaims.map { it.id }.toSet()
        val validProofIds = asset.claims.map { it.id }.toSet()
        val seenPairs = mutableSetOf<Pair<String, String>>()
        evidenceLinks.forEach { link ->
            if (link.organizationClaimId !in validCompanyIds) {
                add("An evidence link references a company claim outside this assessment.")
            }
            if (link.proofClaimId !in validProofIds) {
                add("An evidence link references a proof claim outside this proof asset.")
            }
            if (!seenPairs.add(link.organizationClaimId to link.proofClaimId)) {
                add("Duplicate company-claim and proof-claim evidence links are not allowed.")
            }
            if (link.rationale.length > MAX_RATIONALE_LENGTH) add("An evidence-link rationale is too long.")
            if (link.relationship != FitEvidenceRelationship.UNKNOWN && link.rationale.isBlank()) {
                add("Supporting or contradicting evidence needs a reviewer rationale.")
            }
            if (link.createdAt < 0) add("An evidence-link timestamp is invalid.")
        }
        if (evaluatedAt < 0) add("Fit assessment timestamp is invalid.")
        reviewerOverride?.let { override ->
            if (override.id.isBlank() || override.id.length > 128) add("Reviewer override identifier is invalid.")
            if (override.rationale.isBlank() || override.rationale.length > MAX_RATIONALE_LENGTH) {
                add("A reviewer override needs a rationale of at most 1,000 characters.")
            }
            if (override.reviewedAt < 0) add("Reviewer override timestamp is invalid.")
        }
    }
}

object FitAssessmentEvaluator {
    fun evaluate(
        organizationId: String,
        asset: ProofAsset,
        companyClaims: Collection<FitCompanyClaim>,
        evidenceLinks: Collection<FitEvidenceLink>,
        evaluatedAt: Long,
        reviewerOverride: FitAssessmentOverride? = null
    ): FitAssessmentView {
        val errors = FitAssessmentRules.validate(
            organizationId, asset, companyClaims, evidenceLinks, evaluatedAt, reviewerOverride
        )
        require(errors.isEmpty()) { "Fit assessment input is invalid: ${errors.joinToString("; ")}" }

        val companyById = companyClaims.associateBy { it.id }
        val proofById = asset.claims.associateBy { it.id }
        val findings = evidenceLinks
            .sortedWith(compareBy<FitEvidenceLink>({ it.proofClaimId }, { it.organizationClaimId }))
            .map { link ->
                val companyClaim = requireNotNull(companyById[link.organizationClaimId])
                val proofClaim = requireNotNull(proofById[link.proofClaimId])
                val ineligibilityReason = when {
                    companyClaim.reviewState != FitCompanyClaimReviewState.SUPPORTED ->
                        "Company claim is not source-supported."
                    proofClaim.status != ProofClaimStatus.SUPPORTED ||
                        proofClaim.evidenceIds.isEmpty() || proofClaim.verifiedAt == null ->
                        "Proof claim is not currently evidence-supported."
                    link.relationship == FitEvidenceRelationship.UNKNOWN ->
                        "The evidence relationship has not been determined."
                    else -> null
                }
                val state = if (ineligibilityReason != null) {
                    FitEvidenceFindingState.UNKNOWN
                } else {
                    when (link.relationship) {
                        FitEvidenceRelationship.SUPPORTS -> FitEvidenceFindingState.SUPPORTS
                        FitEvidenceRelationship.CONTRADICTS -> FitEvidenceFindingState.CONTRADICTS
                        FitEvidenceRelationship.UNKNOWN -> FitEvidenceFindingState.UNKNOWN
                    }
                }
                FitAssessmentEvidenceFinding(
                    organizationClaimId = companyClaim.id,
                    organizationClaimField = companyClaim.field,
                    organizationClaimStatement = companyClaim.statement,
                    proofClaimId = proofClaim.id,
                    proofClaimStatement = proofClaim.statement,
                    declaredRelationship = link.relationship,
                    effectiveState = state,
                    eligibilityReason = ineligibilityReason
                )
            }

        val components = asset.claims.sortedBy { it.id }.map { proofClaim ->
            val linkedFindings = findings.filter { it.proofClaimId == proofClaim.id }
            val supporting = linkedFindings.filter { it.effectiveState == FitEvidenceFindingState.SUPPORTS }
            val contradicting = linkedFindings.filter { it.effectiveState == FitEvidenceFindingState.CONTRADICTS }
            val unknown = linkedFindings.filter { it.effectiveState == FitEvidenceFindingState.UNKNOWN }
            val outcome = when {
                supporting.isNotEmpty() && contradicting.isNotEmpty() -> FitComponentOutcome.CONFLICTED
                contradicting.isNotEmpty() -> FitComponentOutcome.CONTRADICTED
                supporting.isNotEmpty() -> FitComponentOutcome.SUPPORTED
                else -> FitComponentOutcome.UNKNOWN
            }
            FitAssessmentComponent(
                proofClaimId = proofClaim.id,
                proofClaimStatement = proofClaim.statement,
                outcome = outcome,
                supportingEvidence = supporting,
                contradictingEvidence = contradicting,
                unknownEvidence = unknown,
                unknownReason = if (linkedFindings.isEmpty()) {
                    "No company evidence is linked to this proof claim."
                } else {
                    null
                }
            )
        }

        val counts = FitAssessmentCounts(
            supported = components.count { it.outcome == FitComponentOutcome.SUPPORTED },
            contradicted = components.count { it.outcome == FitComponentOutcome.CONTRADICTED },
            conflicted = components.count { it.outcome == FitComponentOutcome.CONFLICTED },
            unknown = components.count { it.outcome == FitComponentOutcome.UNKNOWN }
        )
        val outcome = when {
            counts.conflicted > 0 -> FitAssessmentOutcome.CONFLICTED
            counts.contradicted > 0 -> FitAssessmentOutcome.CONTRADICTED
            counts.unknown == 0 && counts.supported == components.size -> FitAssessmentOutcome.SUPPORTED
            counts.supported > 0 -> FitAssessmentOutcome.PARTIAL
            else -> FitAssessmentOutcome.UNKNOWN
        }

        return FitAssessmentView(
            computed = FitAssessmentResult(
                organizationId = organizationId,
                proofAssetId = asset.id,
                evaluatedAt = evaluatedAt,
                outcome = outcome,
                components = components,
                counts = counts
            ),
            reviewerOverride = reviewerOverride
        )
    }
}
