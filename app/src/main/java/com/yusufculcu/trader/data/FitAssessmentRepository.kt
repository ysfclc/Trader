package com.yusufculcu.trader.data

import androidx.room.withTransaction
import com.yusufculcu.trader.domain.FitAssessmentComponent
import com.yusufculcu.trader.domain.FitAssessmentCounts
import com.yusufculcu.trader.domain.FitAssessmentEvidenceFinding
import com.yusufculcu.trader.domain.FitAssessmentEvaluator
import com.yusufculcu.trader.domain.FitAssessmentOverride
import com.yusufculcu.trader.domain.FitAssessmentOverrideDecision
import com.yusufculcu.trader.domain.FitAssessmentResult
import com.yusufculcu.trader.domain.FitAssessmentView
import com.yusufculcu.trader.domain.FitCompanyClaim
import com.yusufculcu.trader.domain.FitCompanyClaimReviewState
import com.yusufculcu.trader.domain.FitComponentOutcome
import com.yusufculcu.trader.domain.FitEvidenceFindingState
import com.yusufculcu.trader.domain.FitEvidenceLink
import com.yusufculcu.trader.domain.FitEvidenceRelationship
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class FitReviewerOverrideDraft(
    val decision: FitAssessmentOverrideDecision,
    val rationale: String,
    val reviewerLabel: String = ""
)

/**
 * Coordinates evidence-backed fit assessment persistence.
 *
 * The computed result is a deterministic snapshot; reviewer decisions are
 * appended to their own table and never overwrite that snapshot.
 */
class FitAssessmentRepository(private val database: TraderDatabase) {
    fun observeForOrganization(organizationId: String): Flow<List<FitAssessmentEntity>> =
        database.fitAssessmentDao().observeForOrganization(organizationId)

    fun observeEvidence(assessmentId: String): Flow<List<FitAssessmentEvidenceEntity>> =
        database.fitAssessmentEvidenceDao().observeForAssessment(assessmentId)

    fun observeOverrideHistory(assessmentId: String): Flow<List<FitAssessmentOverrideEntity>> =
        database.fitAssessmentOverrideDao().observeHistory(assessmentId)

    suspend fun assess(
        organizationId: String,
        proofAssetId: String,
        evidenceLinks: List<FitEvidenceLink>,
        reviewerOverride: FitReviewerOverrideDraft? = null,
        evaluatedAt: Long = System.currentTimeMillis()
    ): FitAssessmentView = database.withTransaction {
        database.organizationDao().getById(organizationId)
            ?: throw IllegalArgumentException("Company dossier does not exist.")
        val proofRow = database.proofAssetDao().getById(proofAssetId)
            ?: throw IllegalArgumentException("Proof asset does not exist.")
        require(proofRow.schemaVersion == 1 && proofRow.id == proofAssetId) {
            "The selected proof asset version is not supported."
        }
        val asset = runCatching { DiscoveryStrategyCodec.decodeAsset(proofRow.payloadJson) }
            .getOrElse { throw IllegalArgumentException("The saved proof asset is invalid.", it) }
        require(asset.id == proofRow.id && asset.schemaVersion == proofRow.schemaVersion &&
            asset.kind.name == proofRow.kind && asset.createdAt == proofRow.createdAt &&
            asset.updatedAt == proofRow.updatedAt) {
            "Proof asset metadata does not match its saved payload."
        }

        val claimRows = database.organizationClaimDao().getAll()
            .filter { it.organizationId == organizationId }
        val sourceIds = database.organizationSourceDao().getAll()
            .filter { it.organizationId == organizationId }
            .map { it.id }
            .toSet()
        require(claimRows.all { it.sourceId in sourceIds }) {
            "A company claim references a source outside its dossier."
        }
        val companyClaims = claimRows.map { row ->
            val reviewState = when (row.reviewState) {
                OrganizationClaimState.UNREVIEWED -> FitCompanyClaimReviewState.UNREVIEWED
                OrganizationClaimState.SUPPORTED -> FitCompanyClaimReviewState.SUPPORTED
                OrganizationClaimState.CONTRADICTED -> FitCompanyClaimReviewState.CONTRADICTED
                OrganizationClaimState.REJECTED -> FitCompanyClaimReviewState.REJECTED
                else -> throw IllegalArgumentException("A company claim has an unsupported review state.")
            }
            FitCompanyClaim(row.id, row.organizationId, row.field, row.statement, reviewState)
        }

        val validProofEvidenceIds = database.evidenceDao().getAll().map { it.id }.toSet()
        require(asset.claims.flatMap { it.evidenceIds }.all { it in validProofEvidenceIds }) {
            "A proof claim references evidence that no longer exists."
        }

        val existing = database.fitAssessmentDao()
            .findForOrganizationAndProofAsset(organizationId, proofAssetId)
        require(existing == null || evaluatedAt >= existing.updatedAt) {
            "Assessment timestamps must not move backwards."
        }
        require(existing == null || existing.updatedAt < Long.MAX_VALUE) {
            "The assessment timestamp cannot be advanced safely."
        }
        val assessmentId = existing?.id ?: UUID.randomUUID().toString()
        val now = existing?.updatedAt?.let { previous -> maxOf(evaluatedAt, previous + 1L) } ?: evaluatedAt
        val errors = com.yusufculcu.trader.domain.FitAssessmentRules.validate(
            organizationId, asset, companyClaims, evidenceLinks, now
        )
        require(errors.isEmpty()) { errors.joinToString(" ") }

        val latestOverrideRow = reviewerOverride?.let { draft ->
            require(draft.reviewerLabel.trim().length <= 120) { "Reviewer label is too long." }
            FitAssessmentOverrideEntity(
                id = UUID.randomUUID().toString(),
                assessmentId = assessmentId,
                decision = draft.decision.name,
                rationale = draft.rationale.trim(),
                reviewerLabel = draft.reviewerLabel.trim().ifBlank { null },
                reviewedAt = now
            )
        }
        val currentOverrideRow = latestOverrideRow
            ?: database.fitAssessmentOverrideDao().latestForAssessment(assessmentId)
        val currentOverride = currentOverrideRow?.takeIf { it.reviewedAt >= now }?.let(::toDomainOverride)
        val view = FitAssessmentEvaluator.evaluate(
            organizationId = organizationId,
            asset = asset,
            companyClaims = companyClaims,
            evidenceLinks = evidenceLinks,
            evaluatedAt = now,
            reviewerOverride = currentOverride
        )
        val snapshot = FitAssessmentSnapshotCodec.encode(view.computed)
        require(snapshot.length <= MAX_SNAPSHOT_CHARS) {
            "The computed fit assessment is too large to persist safely."
        }

        val assessment = FitAssessmentEntity(
            id = assessmentId,
            organizationId = organizationId,
            proofAssetId = proofAssetId,
            computedOutcome = view.computed.outcome.name,
            computedSnapshotJson = snapshot,
            evaluatedAt = now,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        if (existing == null) database.fitAssessmentDao().insert(assessment)
        else database.fitAssessmentDao().update(assessment)

        database.fitAssessmentEvidenceDao().deleteForAssessment(assessmentId)
        evidenceLinks.forEach { link ->
            database.fitAssessmentEvidenceDao().insert(
                FitAssessmentEvidenceEntity(
                    id = UUID.randomUUID().toString(),
                    assessmentId = assessmentId,
                    organizationClaimId = link.organizationClaimId,
                    proofClaimId = link.proofClaimId,
                    relationship = link.relationship.name,
                    rationale = link.rationale,
                    createdAt = link.createdAt
                )
            )
        }
        if (latestOverrideRow != null) database.fitAssessmentOverrideDao().append(latestOverrideRow)
        database.organizationDao().touch(organizationId, now)
        view
    }

    suspend fun getView(assessmentId: String): FitAssessmentView? =
        database.withTransaction { readViewInTransaction(assessmentId) }

    suspend fun getForOrganizationAndProofAsset(
        organizationId: String,
        proofAssetId: String
    ): FitAssessmentView? = database.withTransaction {
        val row = database.fitAssessmentDao()
            .findForOrganizationAndProofAsset(organizationId, proofAssetId) ?: return@withTransaction null
        readViewInTransaction(row.id)
    }

    private suspend fun readViewInTransaction(assessmentId: String): FitAssessmentView? {
        val row = database.fitAssessmentDao().getById(assessmentId) ?: return null
        val snapshot = row.computedSnapshotJson ?: return null
        val computed = FitAssessmentSnapshotCodec.decode(snapshot)
        val override = database.fitAssessmentOverrideDao()
            .latestForAssessment(assessmentId)?.takeIf { it.reviewedAt >= row.evaluatedAt }?.let(::toDomainOverride)
        return FitAssessmentView(computed, override)
    }

    private fun toDomainOverride(row: FitAssessmentOverrideEntity): FitAssessmentOverride {
        val decision = runCatching { FitAssessmentOverrideDecision.valueOf(row.decision) }
            .getOrElse { throw IllegalStateException("A saved reviewer override has an unsupported decision.", it) }
        return FitAssessmentOverride(row.id, decision, row.rationale, row.reviewedAt)
    }

    private companion object {
        const val MAX_SNAPSHOT_CHARS = 1_000_000
    }
}

/** Strict, version-stable JSON representation of the computed result shown to the operator. */
internal object FitAssessmentSnapshotCodec {
    fun encode(result: FitAssessmentResult): String = JSONObject()
        .put("schemaVersion", 1)
        .put("organizationId", result.organizationId)
        .put("proofAssetId", result.proofAssetId)
        .put("evaluatedAt", result.evaluatedAt)
        .put("outcome", result.outcome.name)
        .put("counts", JSONObject()
            .put("supported", result.counts.supported)
            .put("contradicted", result.counts.contradicted)
            .put("conflicted", result.counts.conflicted)
            .put("unknown", result.counts.unknown))
        .put("components", JSONArray().apply {
            result.components.forEach { component ->
                put(JSONObject()
                    .put("proofClaimId", component.proofClaimId)
                    .put("proofClaimStatement", component.proofClaimStatement)
                    .put("outcome", component.outcome.name)
                    .put("supportingEvidence", findings(component.supportingEvidence))
                    .put("contradictingEvidence", findings(component.contradictingEvidence))
                    .put("unknownEvidence", findings(component.unknownEvidence))
                    .put("unknownReason", component.unknownReason ?: JSONObject.NULL))
            }
        })
        .toString()

    fun decode(value: String): FitAssessmentResult {
        val root = JSONObject(value)
        require(root.getInt("schemaVersion") == 1) { "Unsupported fit assessment snapshot version." }
        val counts = root.getJSONObject("counts")
        val components = root.getJSONArray("components")
        return FitAssessmentResult(
            organizationId = root.getString("organizationId"),
            proofAssetId = root.getString("proofAssetId"),
            evaluatedAt = root.getLong("evaluatedAt"),
            outcome = enumValue(root.getString("outcome"), com.yusufculcu.trader.domain.FitAssessmentOutcome.values()),
            components = List(components.length()) { index ->
                val item = components.getJSONObject(index)
                FitAssessmentComponent(
                    proofClaimId = item.getString("proofClaimId"),
                    proofClaimStatement = item.getString("proofClaimStatement"),
                    outcome = enumValue(item.getString("outcome"), FitComponentOutcome.values()),
                    supportingEvidence = readFindings(item.getJSONArray("supportingEvidence")),
                    contradictingEvidence = readFindings(item.getJSONArray("contradictingEvidence")),
                    unknownEvidence = readFindings(item.getJSONArray("unknownEvidence")),
                    unknownReason = item.stringOrNull("unknownReason")
                )
            },
            counts = FitAssessmentCounts(
                supported = counts.getInt("supported"),
                contradicted = counts.getInt("contradicted"),
                conflicted = counts.getInt("conflicted"),
                unknown = counts.getInt("unknown")
            )
        )
    }

    private fun findings(items: List<FitAssessmentEvidenceFinding>) = JSONArray().apply {
        items.forEach { finding ->
            put(JSONObject()
                .put("organizationClaimId", finding.organizationClaimId)
                .put("organizationClaimField", finding.organizationClaimField)
                .put("organizationClaimStatement", finding.organizationClaimStatement)
                .put("proofClaimId", finding.proofClaimId)
                .put("proofClaimStatement", finding.proofClaimStatement)
                .put("declaredRelationship", finding.declaredRelationship.name)
                .put("effectiveState", finding.effectiveState.name)
                .put("eligibilityReason", finding.eligibilityReason ?: JSONObject.NULL))
        }
    }

    private fun readFindings(items: JSONArray): List<FitAssessmentEvidenceFinding> =
        List(items.length()) { index ->
            val item = items.getJSONObject(index)
            FitAssessmentEvidenceFinding(
                organizationClaimId = item.getString("organizationClaimId"),
                organizationClaimField = item.getString("organizationClaimField"),
                organizationClaimStatement = item.getString("organizationClaimStatement"),
                proofClaimId = item.getString("proofClaimId"),
                proofClaimStatement = item.getString("proofClaimStatement"),
                declaredRelationship = enumValue(item.getString("declaredRelationship"), FitEvidenceRelationship.values()),
                effectiveState = enumValue(item.getString("effectiveState"), FitEvidenceFindingState.values()),
                eligibilityReason = item.stringOrNull("eligibilityReason")
            )
        }

    private fun JSONObject.stringOrNull(key: String): String? =
        if (isNull(key)) null else getString(key)

    private fun <T : Enum<T>> enumValue(value: String, values: Array<T>): T =
        values.firstOrNull { it.name == value }
            ?: throw IllegalArgumentException("Unsupported fit assessment enum value.")
}
