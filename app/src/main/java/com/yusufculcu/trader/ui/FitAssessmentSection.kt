package com.yusufculcu.trader.ui

import java.text.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yusufculcu.trader.R
import com.yusufculcu.trader.data.DiscoveryStrategyRepository
import com.yusufculcu.trader.data.FitAssessmentEntity
import com.yusufculcu.trader.data.FitAssessmentEvidenceEntity
import com.yusufculcu.trader.data.FitAssessmentRepository
import com.yusufculcu.trader.data.FitReviewerOverrideDraft
import com.yusufculcu.trader.data.OrganizationClaimEntity
import com.yusufculcu.trader.data.OrganizationClaimState
import com.yusufculcu.trader.data.OrganizationSourceEntity
import com.yusufculcu.trader.domain.FitAssessmentDisposition
import com.yusufculcu.trader.domain.FitAssessmentOutcome
import com.yusufculcu.trader.domain.FitAssessmentOverrideDecision
import com.yusufculcu.trader.domain.FitAssessmentView
import com.yusufculcu.trader.domain.FitComponentOutcome
import com.yusufculcu.trader.domain.FitEvidenceFindingState
import com.yusufculcu.trader.domain.FitEvidenceLink
import com.yusufculcu.trader.domain.FitEvidenceRelationship
import com.yusufculcu.trader.domain.ProofAsset
import com.yusufculcu.trader.domain.ProofClaim
import com.yusufculcu.trader.domain.ProofClaimStatus
import kotlinx.coroutines.launch
import java.util.Date

@Composable
fun FitAssessmentSection(
    organizationId: String,
    organizationUpdatedAt: Long,
    claims: List<OrganizationClaimEntity>,
    sources: List<OrganizationSourceEntity>,
    strategyRepository: DiscoveryStrategyRepository,
    fitRepository: FitAssessmentRepository,
    onOpenSource: (String) -> Unit
) {
    val proofAssets by strategyRepository.observeProofAssetModels().collectAsState(initial = emptyList())
    val assessments by fitRepository.observeForOrganization(organizationId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedAssetId by rememberSaveable(organizationId) { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(proofAssets, selectedAssetId) {
        if (proofAssets.none { it.id == selectedAssetId }) {
            selectedAssetId = proofAssets.firstOrNull()?.id
        }
    }

    val selectedAsset = proofAssets.firstOrNull { it.id == selectedAssetId }
    val assessment = assessments.firstOrNull { it.proofAssetId == selectedAsset?.id }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                stringResource(R.string.fit_assessment_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                stringResource(R.string.fit_assessment_intro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (proofAssets.isEmpty()) {
                Text(
                    stringResource(R.string.fit_no_proof_assets),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FitDropdownSelector(
                    label = stringResource(R.string.fit_select_proof_asset),
                    selectedId = selectedAsset?.id.orEmpty(),
                    options = proofAssets.map { it.id to it.name },
                    onSelect = { selectedAssetId = it }
                )

                if (selectedAsset != null && selectedAsset.claims.isEmpty()) {
                    Text(
                        stringResource(R.string.fit_asset_no_claims),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                } else if (selectedAsset != null && assessment == null) {
                    Button(
                        enabled = !creating,
                        onClick = {
                            scope.launch {
                                creating = true
                                error = null
                                runCatching {
                                    fitRepository.assess(organizationId, selectedAsset.id, emptyList())
                                }.onSuccess {
                                    notice = context.getString(R.string.fit_action_saved)
                                }.onFailure {
                                    error = it.message ?: context.getString(R.string.fit_action_error)
                                }
                                creating = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.fit_create_assessment))
                    }
                } else if (selectedAsset != null && assessment != null) {
                    FitAssessmentDetails(
                        organizationId = organizationId,
                        organizationUpdatedAt = organizationUpdatedAt,
                        claims = claims,
                        sources = sources,
                        asset = selectedAsset,
                        assessment = assessment,
                        repository = fitRepository,
                        onOpenSource = onOpenSource
                    )
                }
            }

            notice?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun FitAssessmentDetails(
    organizationId: String,
    organizationUpdatedAt: Long,
    claims: List<OrganizationClaimEntity>,
    sources: List<OrganizationSourceEntity>,
    asset: ProofAsset,
    assessment: FitAssessmentEntity,
    repository: FitAssessmentRepository,
    onOpenSource: (String) -> Unit
) {
    val evidence by repository.observeEvidence(assessment.id).collectAsState(initial = emptyList())
    val overrideHistory by repository.observeOverrideHistory(assessment.id).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var view by remember(assessment.id) { mutableStateOf<FitAssessmentView?>(null) }
    var showLinkDialog by rememberSaveable(assessment.id) { mutableStateOf(false) }
    var editingLink by remember { mutableStateOf<FitAssessmentEvidenceEntity?>(null) }
    var showOverrideDialog by rememberSaveable(assessment.id) { mutableStateOf(false) }
    var pendingRemoval by remember { mutableStateOf<FitAssessmentEvidenceEntity?>(null) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(assessment.id, assessment.computedSnapshotJson, overrideHistory.firstOrNull()?.id) {
        view = runCatching { repository.getView(assessment.id) }
            .onFailure { error = it.message ?: context.getString(R.string.fit_action_error) }
            .getOrNull()
    }

    fun persistLinks(next: List<FitEvidenceLink>, reviewerOverride: FitReviewerOverrideDraft? = null) {
        scope.launch {
            saving = true
            error = null
            runCatching {
                repository.assess(
                    organizationId = organizationId,
                    proofAssetId = asset.id,
                    evidenceLinks = next,
                    reviewerOverride = reviewerOverride
                )
            }.onSuccess {
                notice = context.getString(R.string.fit_action_saved)
                if (reviewerOverride != null) showOverrideDialog = false
                else {
                    showLinkDialog = false
                    editingLink = null
                }
            }.onFailure {
                error = it.message ?: context.getString(R.string.fit_action_error)
            }
            saving = false
        }
    }

    val companyById = remember(claims) { claims.associateBy { it.id } }
    val sourceById = remember(sources) { sources.associateBy { it.id } }
    val proofById = remember(asset.claims) { asset.claims.associateBy { it.id } }
    val currentView = view
    val latestEffectiveOverrideId = currentView?.reviewerOverride?.id
    val isStale = organizationUpdatedAt > assessment.evaluatedAt || asset.updatedAt > assessment.evaluatedAt

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        if (currentView != null) {
            val result = currentView.computed
            Text(stringResource(R.string.fit_computed_result), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AssistChip(onClick = {}, label = { Text(fitOutcomeLabel(result.outcome)) })
                AssistChip(
                    onClick = {},
                    label = {
                        Text(stringResource(
                            R.string.fit_counts,
                            result.counts.supported,
                            result.counts.contradicted,
                            result.counts.conflicted,
                            result.counts.unknown
                        ))
                    }
                )
            }
            Text(
                stringResource(R.string.fit_effective_decision, fitDispositionLabel(currentView.effectiveDisposition)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            currentView.reviewerOverride?.let {
                Text(stringResource(R.string.fit_override_effective), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                Text(it.rationale, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            Text(stringResource(R.string.fit_computed_result), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.fit_snapshot_unavailable), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        if (isStale) {
            Text(stringResource(R.string.fit_stale_warning), color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
        }

        if (claims.none { it.reviewState == OrganizationClaimState.SUPPORTED }) {
            Text(stringResource(R.string.fit_no_supported_claims), color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = !saving && claims.any { it.reviewState == OrganizationClaimState.SUPPORTED },
                onClick = { editingLink = null; showLinkDialog = true },
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.fit_add_mapping)) }
            TextButton(
                enabled = !saving,
                onClick = { persistLinks(evidence.map { it.toDomain() }) }
            ) { Text(stringResource(R.string.fit_recalculate)) }
        }
        TextButton(
            enabled = !saving && currentView != null,
            onClick = { showOverrideDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.fit_override_button)) }

        Text(stringResource(R.string.fit_evidence_mappings), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        if (evidence.isEmpty()) {
            Text(stringResource(R.string.fit_no_mappings), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            evidence.forEach { link ->
                val company = companyById[link.organizationClaimId]
                val proof = proofById[link.proofClaimId]
                val source = company?.let { sourceById[it.sourceId] }
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(company?.field ?: stringResource(R.string.fit_missing_company_claim), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        Text(company?.statement ?: link.organizationClaimId, style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.fit_mapping_arrow), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(proof?.statement ?: link.proofClaimId, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text(
                            fitRelationshipLabel(runCatching { FitEvidenceRelationship.valueOf(link.relationship) }.getOrDefault(FitEvidenceRelationship.UNKNOWN)),
                            style = MaterialTheme.typography.labelSmall
                        )
                        if (link.rationale.isNotBlank()) Text(link.rationale, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        source?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.fit_finding_source, it.title), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                                TextButton(onClick = { onOpenSource(it.url) }) { Text(stringResource(R.string.fit_open_source)) }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { editingLink = link; showLinkDialog = true }) { Text(stringResource(R.string.fit_edit_link)) }
                            TextButton(onClick = { pendingRemoval = link }) { Text(stringResource(R.string.fit_remove_mapping)) }
                        }
                    }
                }
            }
        }

        Text(stringResource(R.string.fit_components), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        currentView?.computed?.components?.forEach { component ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(component.proofClaimStatement, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    AssistChip(onClick = {}, label = { Text(fitComponentOutcomeLabel(component.outcome)) })
                    if (component.supportingEvidence.isNotEmpty()) {
                        Text(stringResource(R.string.fit_supporting), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        component.supportingEvidence.forEach { finding ->
                            FitEvidenceFindingRow(finding, companyById, sourceById, onOpenSource)
                        }
                    }
                    if (component.contradictingEvidence.isNotEmpty()) {
                        Text(stringResource(R.string.fit_contradicting), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                        component.contradictingEvidence.forEach { finding ->
                            FitEvidenceFindingRow(finding, companyById, sourceById, onOpenSource)
                        }
                    }
                    if (component.unknownEvidence.isNotEmpty()) {
                        Text(stringResource(R.string.fit_unknown_evidence), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                        component.unknownEvidence.forEach { finding ->
                            FitEvidenceFindingRow(finding, companyById, sourceById, onOpenSource)
                        }
                    }
                    component.unknownReason?.let {
                        Text(stringResource(R.string.fit_unknown_reason, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Text(stringResource(R.string.fit_override_history), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        if (overrideHistory.isEmpty()) {
            Text(stringResource(R.string.fit_no_override_history), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            overrideHistory.forEach { override ->
                val effective = override.id == latestEffectiveOverrideId
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(fitOverrideDecisionLabel(override.decision), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(R.string.fit_override_reviewed_at, DateFormat.getDateTimeInstance().format(Date(override.reviewedAt))),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        override.reviewerLabel?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                        Text(override.rationale, style = MaterialTheme.typography.bodySmall)
                        Text(
                            stringResource(if (effective) R.string.fit_override_effective else R.string.fit_override_historical),
                            color = if (effective) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }

    if (showLinkDialog) {
        FitEvidenceLinkDialog(
            claims = claims,
            proofClaims = asset.claims,
            existingLinks = evidence,
            initial = editingLink,
            saving = saving,
            onDismiss = { showLinkDialog = false; editingLink = null },
            onSave = { companyId, proofId, relationship, rationale ->
                val previous = editingLink
                val links = evidence.map { it.toDomain() }.filterNot {
                    previous != null && it.organizationClaimId == previous.organizationClaimId &&
                        it.proofClaimId == previous.proofClaimId
                } + FitEvidenceLink(
                    organizationClaimId = companyId,
                    proofClaimId = proofId,
                    relationship = relationship,
                    rationale = rationale.trim(),
                    createdAt = previous?.createdAt ?: System.currentTimeMillis()
                )
                persistLinks(links)
            }
        )
    }

    if (showOverrideDialog) {
        FitReviewerOverrideDialog(
            saving = saving,
            onDismiss = { showOverrideDialog = false },
            onSave = { decision, rationale, reviewer ->
                persistLinks(
                    evidence.map { it.toDomain() },
                    FitReviewerOverrideDraft(decision, rationale, reviewer)
                )
            }
        )
    }

    pendingRemoval?.let { link ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text(stringResource(R.string.fit_remove_mapping)) },
            text = { Text(stringResource(R.string.fit_remove_mapping_confirm)) },
            confirmButton = {
                TextButton(
                    enabled = !saving,
                    onClick = {
                        persistLinks(evidence.filterNot { it.id == link.id }.map { it.toDomain() })
                        pendingRemoval = null
                    }
                ) { Text(stringResource(R.string.fit_remove_mapping)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) { Text(stringResource(R.string.fit_cancel)) }
            }
        )
    }

    notice?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun FitEvidenceFindingRow(
    finding: com.yusufculcu.trader.domain.FitAssessmentEvidenceFinding,
    companyById: Map<String, OrganizationClaimEntity>,
    sourceById: Map<String, OrganizationSourceEntity>,
    onOpenSource: (String) -> Unit
) {
    val source = companyById[finding.organizationClaimId]?.let { sourceById[it.sourceId] }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            stringResource(R.string.fit_company_finding, finding.organizationClaimField, finding.organizationClaimStatement),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            stringResource(R.string.fit_declared_relationship, fitRelationshipLabel(finding.declaredRelationship)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (finding.effectiveState == FitEvidenceFindingState.UNKNOWN && finding.eligibilityReason != null) {
            Text(finding.eligibilityReason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
        }
        source?.let {
            TextButton(onClick = { onOpenSource(it.url) }) {
                Text(stringResource(R.string.fit_finding_source, it.title))
            }
        }
    }
}

@Composable
private fun FitEvidenceLinkDialog(
    claims: List<OrganizationClaimEntity>,
    proofClaims: List<ProofClaim>,
    existingLinks: List<FitAssessmentEvidenceEntity>,
    initial: FitAssessmentEvidenceEntity?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, FitEvidenceRelationship, String) -> Unit
) {
    val supportedLabel = stringResource(R.string.fit_claim_supported)
    val notSupportedLabel = stringResource(R.string.fit_claim_not_supported)
    val proofStatusLabels = mapOf(
        ProofClaimStatus.SUPPORTED to stringResource(R.string.fit_proof_supported),
        ProofClaimStatus.UNVERIFIED to stringResource(R.string.fit_proof_unverified),
        ProofClaimStatus.CONTRADICTED to stringResource(R.string.fit_proof_contradicted),
        ProofClaimStatus.EXPIRED to stringResource(R.string.fit_proof_expired)
    )
    val companyOptions = claims
        .filter { it.reviewState == OrganizationClaimState.SUPPORTED || it.id == initial?.organizationClaimId }
        .map { claim ->
            val status = if (claim.reviewState == OrganizationClaimState.SUPPORTED) supportedLabel else notSupportedLabel
            claim.id to (claim.field + ": " + claim.statement.take(90) + " · " + status)
        }
    val proofOptions = proofClaims.map { claim ->
        claim.id to (claim.statement.take(110) + " · " + proofStatusLabels.getValue(claim.status))
    }
    var companyId by remember(initial?.id) { mutableStateOf(initial?.organizationClaimId ?: companyOptions.firstOrNull()?.first.orEmpty()) }
    var proofId by remember(initial?.id) { mutableStateOf(initial?.proofClaimId ?: proofOptions.firstOrNull()?.first.orEmpty()) }
    var relationship by remember(initial?.id) {
        mutableStateOf(runCatching { FitEvidenceRelationship.valueOf(initial?.relationship ?: "") }.getOrDefault(FitEvidenceRelationship.UNKNOWN))
    }
    var rationale by remember(initial?.id) { mutableStateOf(initial?.rationale.orEmpty()) }
    var validation by remember { mutableStateOf<String?>(null) }
    val chooseBothMessage = stringResource(R.string.fit_choose_both_claims)
    val duplicateMessage = stringResource(R.string.fit_duplicate_link)
    val rationaleMessage = stringResource(R.string.fit_rationale_required)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.fit_mapping_title else R.string.fit_edit_link)) },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 540.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(stringResource(R.string.fit_mapping_notice), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FitDropdownSelector(
                    label = stringResource(R.string.fit_company_claim),
                    selectedId = companyId,
                    options = companyOptions,
                    onSelect = { companyId = it }
                )
                FitDropdownSelector(
                    label = stringResource(R.string.fit_proof_claim),
                    selectedId = proofId,
                    options = proofOptions,
                    onSelect = { proofId = it }
                )
                Text(stringResource(R.string.fit_relationship), style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    FitEvidenceRelationship.values().forEach { option ->
                        FilterChip(
                            selected = relationship == option,
                            onClick = { relationship = option },
                            label = { Text(fitRelationshipLabel(option)) }
                        )
                    }
                }
                OutlinedTextField(
                    value = rationale,
                    onValueChange = { rationale = it.take(1000) },
                    label = { Text(stringResource(R.string.fit_rationale)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
                Text(stringResource(R.string.fit_relationship_notice), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                validation?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && companyOptions.isNotEmpty() && proofOptions.isNotEmpty(),
                onClick = {
                    val duplicate = existingLinks.any {
                        it.id != initial?.id && it.organizationClaimId == companyId && it.proofClaimId == proofId
                    }
                    when {
                        companyId.isBlank() || proofId.isBlank() -> validation = chooseBothMessage
                        duplicate -> validation = duplicateMessage
                        relationship != FitEvidenceRelationship.UNKNOWN && rationale.isBlank() -> validation = rationaleMessage
                        else -> onSave(companyId, proofId, relationship, rationale)
                    }
                }
            ) { Text(stringResource(R.string.fit_save_mapping)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.fit_cancel)) }
        }
    )
}

@Composable
private fun FitReviewerOverrideDialog(
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (FitAssessmentOverrideDecision, String, String) -> Unit
) {
    var decision by remember { mutableStateOf(FitAssessmentOverrideDecision.REQUEST_MORE_EVIDENCE) }
    var rationale by remember { mutableStateOf("") }
    var reviewer by remember { mutableStateOf("") }
    var validation by remember { mutableStateOf<String?>(null) }
    val rationaleRequired = stringResource(R.string.fit_override_rationale_required)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.fit_override_title)) },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(stringResource(R.string.fit_override_notice), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf(
                    FitAssessmentOverrideDecision.ACCEPT_FIT,
                    FitAssessmentOverrideDecision.REJECT_FIT,
                    FitAssessmentOverrideDecision.REQUEST_MORE_EVIDENCE
                ).forEach { option ->
                    FilterChip(
                        selected = decision == option,
                        onClick = { decision = option },
                        label = { Text(fitOverrideDecisionLabel(option.name)) }
                    )
                }
                OutlinedTextField(
                    value = rationale,
                    onValueChange = { rationale = it.take(1000) },
                    label = { Text(stringResource(R.string.fit_override_rationale)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
                OutlinedTextField(
                    value = reviewer,
                    onValueChange = { reviewer = it.take(120) },
                    label = { Text(stringResource(R.string.fit_reviewer_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                validation?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(enabled = !saving, onClick = {
                if (rationale.isBlank()) validation = rationaleRequired
                else onSave(decision, rationale.trim(), reviewer.trim())
            }) { Text(stringResource(R.string.fit_save_override)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.fit_cancel)) }
        }
    )
}

@Composable
private fun FitDropdownSelector(
    label: String,
    selectedId: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selectedId }?.second
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = options.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedLabel ?: stringResource(R.string.fit_choose_option))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.second) },
                        onClick = { onSelect(option.first); expanded = false }
                    )
                }
            }
        }
    }
}

private fun FitAssessmentEvidenceEntity.toDomain() = FitEvidenceLink(
    organizationClaimId = organizationClaimId,
    proofClaimId = proofClaimId,
    relationship = FitEvidenceRelationship.valueOf(relationship),
    rationale = rationale,
    createdAt = createdAt
)

@Composable
private fun fitOutcomeLabel(outcome: FitAssessmentOutcome): String = stringResource(
    when (outcome) {
        FitAssessmentOutcome.SUPPORTED -> R.string.fit_outcome_supported
        FitAssessmentOutcome.PARTIAL -> R.string.fit_outcome_partial
        FitAssessmentOutcome.CONTRADICTED -> R.string.fit_outcome_contradicted
        FitAssessmentOutcome.CONFLICTED -> R.string.fit_outcome_conflicted
        FitAssessmentOutcome.UNKNOWN -> R.string.fit_outcome_unknown
    }
)

@Composable
private fun fitComponentOutcomeLabel(outcome: FitComponentOutcome): String = stringResource(
    when (outcome) {
        FitComponentOutcome.SUPPORTED -> R.string.fit_outcome_supported
        FitComponentOutcome.CONTRADICTED -> R.string.fit_outcome_contradicted
        FitComponentOutcome.CONFLICTED -> R.string.fit_outcome_conflicted
        FitComponentOutcome.UNKNOWN -> R.string.fit_outcome_unknown
    }
)

@Composable
private fun fitRelationshipLabel(relationship: FitEvidenceRelationship): String = stringResource(
    when (relationship) {
        FitEvidenceRelationship.SUPPORTS -> R.string.fit_relationship_supports
        FitEvidenceRelationship.CONTRADICTS -> R.string.fit_relationship_contradicts
        FitEvidenceRelationship.UNKNOWN -> R.string.fit_relationship_unknown
    }
)

@Composable
private fun fitDispositionLabel(disposition: FitAssessmentDisposition): String = stringResource(
    when (disposition) {
        FitAssessmentDisposition.SUPPORTED -> R.string.fit_outcome_supported
        FitAssessmentDisposition.PARTIAL -> R.string.fit_outcome_partial
        FitAssessmentDisposition.CONTRADICTED -> R.string.fit_outcome_contradicted
        FitAssessmentDisposition.CONFLICTED -> R.string.fit_outcome_conflicted
        FitAssessmentDisposition.UNKNOWN -> R.string.fit_outcome_unknown
        FitAssessmentDisposition.ACCEPTED_BY_REVIEWER -> R.string.fit_override_accept
        FitAssessmentDisposition.REJECTED_BY_REVIEWER -> R.string.fit_override_reject
        FitAssessmentDisposition.MORE_EVIDENCE_REQUESTED -> R.string.fit_override_more_evidence
    }
)

@Composable
private fun fitOverrideDecisionLabel(value: String): String = stringResource(
    when (runCatching { FitAssessmentOverrideDecision.valueOf(value) }.getOrNull()) {
        FitAssessmentOverrideDecision.ACCEPT_FIT -> R.string.fit_override_accept
        FitAssessmentOverrideDecision.REJECT_FIT -> R.string.fit_override_reject
        FitAssessmentOverrideDecision.REQUEST_MORE_EVIDENCE -> R.string.fit_override_more_evidence
        null -> R.string.fit_outcome_unknown
    }
)

@Composable
private fun proofStatusLabel(status: ProofClaimStatus): String = stringResource(
    when (status) {
        ProofClaimStatus.SUPPORTED -> R.string.fit_proof_supported
        ProofClaimStatus.UNVERIFIED -> R.string.fit_proof_unverified
        ProofClaimStatus.CONTRADICTED -> R.string.fit_proof_contradicted
        ProofClaimStatus.EXPIRED -> R.string.fit_proof_expired
    }
)
