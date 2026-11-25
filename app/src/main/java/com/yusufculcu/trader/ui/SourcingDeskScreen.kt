package com.yusufculcu.trader.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
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
import com.yusufculcu.trader.data.BuyerRequirementEntity
import com.yusufculcu.trader.data.BuyerRequirementLineItemEntity
import com.yusufculcu.trader.data.BuyerRequirementRepository
import com.yusufculcu.trader.data.OrganizationEntity
import com.yusufculcu.trader.data.OrganizationRepository
import com.yusufculcu.trader.data.SourcingMatchEngine
import com.yusufculcu.trader.data.SourcingReviewEntity
import com.yusufculcu.trader.data.SourcingReviewStatus
import com.yusufculcu.trader.data.SupplierOfferDraft
import com.yusufculcu.trader.data.SupplierOfferEntity
import com.yusufculcu.trader.data.SupplierOfferLineItemEntity
import com.yusufculcu.trader.data.SupplierOfferLineItemDraft
import com.yusufculcu.trader.data.SupplierOfferRepository
import com.yusufculcu.trader.data.SupplierOfferStatus
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun SourcingDeskScreen(
    buyerRepository: BuyerRequirementRepository,
    supplierRepository: SupplierOfferRepository,
    organizationRepository: OrganizationRepository,
    onOpenCompanies: () -> Unit,
    modifier: Modifier = Modifier
) {
    val requirements by buyerRepository.observeAll().collectAsState(initial = emptyList())
    val offers by supplierRepository.observeOffers().collectAsState(initial = emptyList())
    val deletedRequirements by buyerRepository.observeDeleted().collectAsState(initial = emptyList())
    val deletedOffers by supplierRepository.observeDeletedOffers().collectAsState(initial = emptyList())
    val organizations by organizationRepository.observeOrganizations().collectAsState(initial = emptyList())
    var selectedRequirementId by rememberSaveable { mutableStateOf<String?>(null) }
    var showOfferDialog by remember { mutableStateOf(false) }
    var reviewTarget by remember { mutableStateOf<SupplierOfferEntity?>(null) }
    var editOfferTarget by remember { mutableStateOf<SupplierOfferEntity?>(null) }
    var itemEditorTarget by remember { mutableStateOf<Pair<SupplierOfferEntity, SupplierOfferLineItemEntity?>?>(null) }
    var evidenceEditorTarget by remember { mutableStateOf<SupplierOfferEntity?>(null) }
    var removeItemTarget by remember { mutableStateOf<Pair<SupplierOfferEntity, SupplierOfferLineItemEntity>?>(null) }
    var showTrash by rememberSaveable { mutableStateOf(false) }
    val selectedRequirement = requirements.firstOrNull { it.id == selectedRequirementId }
    LaunchedEffect(requirements) {
        if (selectedRequirementId == null || requirements.none { it.id == selectedRequirementId }) {
            selectedRequirementId = requirements.firstOrNull()?.id
        }
    }
    val reviewFlow = remember(selectedRequirement?.id) {
        selectedRequirement?.let { supplierRepository.observeReviews(it.id) } ?: flowOf(emptyList())
    }
    val reviews by reviewFlow.collectAsState(initial = emptyList())
    val buyerItemsFlow = remember(selectedRequirement?.id) {
        selectedRequirement?.let { buyerRepository.observeItems(it.id) } ?: flowOf(emptyList())
    }
    val buyerItems by buyerItemsFlow.collectAsState(initial = emptyList())
    val organizationById = remember(organizations) { organizations.associateBy { it.id } }
    val shortlisted = reviews.filter { it.status == SourcingReviewStatus.SHORTLISTED }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.sourcing_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.sourcing_intro), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryPill(stringResource(R.string.sourcing_requests), requirements.size.toString())
            SummaryPill(stringResource(R.string.sourcing_offers), offers.size.toString())
            SummaryPill(stringResource(R.string.sourcing_shortlisted), shortlisted.size.toString())
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { showTrash = !showTrash }) {
                Text(stringResource(if (showTrash) R.string.sourcing_back_to_active else R.string.sourcing_recently_deleted))
            }
        }
        if (showTrash) {
            Text(stringResource(R.string.sourcing_recently_deleted), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (deletedRequirements.isEmpty() && deletedOffers.isEmpty()) {
                Text(stringResource(R.string.sourcing_trash_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(deletedRequirements, key = { "requirement-${it.id}" }) { requirement ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(requirement.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(stringResource(R.string.sourcing_deleted_requirement), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                TextButton(onClick = {
                                    scope.launch {
                                        runCatching { buyerRepository.restoreRequirement(requirement.id) }
                                            .onSuccess { selectedRequirementId = requirement.id; Toast.makeText(context, R.string.sourcing_record_restored, Toast.LENGTH_SHORT).show() }
                                            .onFailure { Toast.makeText(context, it.message ?: "Could not restore requirement.", Toast.LENGTH_LONG).show() }
                                    }
                                }) { Text(stringResource(R.string.sourcing_restore_record)) }
                            }
                        }
                    }
                    items(deletedOffers, key = { "offer-${it.id}" }) { offer ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(offer.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(stringResource(R.string.sourcing_deleted_offer), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                TextButton(onClick = {
                                    scope.launch {
                                        runCatching { supplierRepository.restoreOffer(offer.id) }
                                            .onSuccess { Toast.makeText(context, R.string.sourcing_record_restored, Toast.LENGTH_SHORT).show() }
                                            .onFailure { Toast.makeText(context, it.message ?: "Could not restore offer.", Toast.LENGTH_LONG).show() }
                                    }
                                }) { Text(stringResource(R.string.sourcing_restore_record)) }
                            }
                        }
                    }
                }
            }
        } else if (requirements.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.sourcing_no_requests), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.sourcing_create_request_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onOpenCompanies) { Text(stringResource(R.string.sourcing_open_companies)) }
                }
            }
        } else {
            var requestMenu by remember { mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.sourcing_active_request), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                TextButton(onClick = { requestMenu = true }) {
                    Text(selectedRequirement?.title ?: stringResource(R.string.sourcing_choose_request))
                }
                DropdownMenu(expanded = requestMenu, onDismissRequest = { requestMenu = false }) {
                    requirements.forEach { requirement ->
                        DropdownMenuItem(
                            text = { Text(requirement.title) },
                            onClick = { selectedRequirementId = requirement.id; requestMenu = false }
                        )
                    }
                }
                selectedRequirement?.let { request ->
                    val buyerName = organizationById[request.organizationId]?.canonicalName.orEmpty()
                    Text(
                        listOfNotNull(buyerName.takeIf { it.isNotBlank() }, request.destinationCity, request.destinationCountry, request.requiredByText).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(stringResource(R.string.sourcing_line_items_count, buyerItems.size), style = MaterialTheme.typography.labelSmall)
                    TextButton(onClick = {
                        scope.launch {
                            runCatching { buyerRepository.deleteRequirement(request.id) }
                                .onSuccess { selectedRequirementId = null; Toast.makeText(context, R.string.sourcing_requirement_archived, Toast.LENGTH_SHORT).show() }
                                .onFailure { Toast.makeText(context, it.message ?: "Could not move requirement to trash.", Toast.LENGTH_LONG).show() }
                        }
                    }) { Text(stringResource(R.string.sourcing_move_to_trash)) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.sourcing_supplier_offers), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = { showOfferDialog = true }) { Text(stringResource(R.string.sourcing_add_offer)) }
            }
            if (offers.isEmpty()) {
                Text(stringResource(R.string.sourcing_no_offers), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onOpenCompanies) { Text(stringResource(R.string.sourcing_open_companies)) }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(offers, key = { it.id }) { offer ->
                        SupplierOfferComparisonCard(
                            offer = offer,
                            supplierName = organizationById[offer.organizationId]?.canonicalName ?: stringResource(R.string.sourcing_unknown_supplier),
                            buyerRequirement = selectedRequirement!!,
                            buyerItems = buyerItems,
                            review = reviews.firstOrNull { it.supplierOfferId == offer.id },
                            repository = supplierRepository,
                            organizationRepository = organizationRepository,
                            onReview = { reviewTarget = offer },
                            onSetStatus = { status ->
                                scope.launch {
                                    runCatching { supplierRepository.setStatus(offer.id, status) }
                                        .onFailure { Toast.makeText(context, it.message ?: "Could not update offer status.", Toast.LENGTH_LONG).show() }
                                }
                            },
                            onEditOffer = { editOfferTarget = offer },
                            onAddItem = { itemEditorTarget = offer to null },
                            onEditItem = { item -> itemEditorTarget = offer to item },
                            onDeleteItem = { item -> removeItemTarget = offer to item },
                            onManageEvidence = { evidenceEditorTarget = offer },
                            onArchiveOffer = {
                                scope.launch {
                                    runCatching { supplierRepository.deleteOffer(offer.id) }
                                        .onSuccess { Toast.makeText(context, R.string.sourcing_offer_archived, Toast.LENGTH_SHORT).show() }
                                        .onFailure { Toast.makeText(context, it.message ?: "Could not move offer to trash.", Toast.LENGTH_LONG).show() }
                                }
                            }
                        )
                    }
                }
            }
            if (shortlisted.isNotEmpty() && selectedRequirement != null) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val lines = buildList {
                            add("Trader · Supplier shortlist")
                            add("Buyer requirement: ${selectedRequirement.title}")
                            selectedRequirement.description?.let { add("Context: $it") }
                            add("")
                            shortlisted.forEachIndexed { index, review ->
                                val offer = offers.firstOrNull { it.id == review.supplierOfferId } ?: return@forEachIndexed
                                val supplier = organizationById[offer.organizationId]?.canonicalName ?: "Supplier"
                                add("${index + 1}. $supplier — ${offer.title}")
                                review.rationale?.let { add("Review note: $it") }
                                add("Decision: Shortlisted for human follow-up; not a verified qualification.")
                                add("")
                            }
                            add("Prepared locally in Trader. Supplier statements and technical equivalence require independent verification.")
                        }.joinToString("\n")
                        runCatching {
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Supplier shortlist — ${selectedRequirement.title}")
                                putExtra(Intent.EXTRA_TEXT, lines)
                            }, context.getString(R.string.sourcing_share_shortlist)))
                        }.onFailure { Toast.makeText(context, R.string.sourcing_share_failed, Toast.LENGTH_SHORT).show() }
                    }
                ) { Text(stringResource(R.string.sourcing_share_shortlist)) }
            }
        }
    }

    if (showOfferDialog) {
        SupplierOfferCreateDialog(
            organizations = organizations,
            organizationRepository = organizationRepository,
            onDismiss = { showOfferDialog = false },
            onSave = { organizationId, draft, item, sourceIds ->
                supplierRepository.create(organizationId, draft, item, sourceIds)
            },
            onSaved = { showOfferDialog = false }
        )
    }
    editOfferTarget?.let { offer ->
        SupplierOfferEditDialog(
            offer = offer,
            onDismiss = { editOfferTarget = null },
            onSave = { draft -> supplierRepository.updateOffer(offer.id, draft) },
            onSaved = {
                editOfferTarget = null
                Toast.makeText(context, context.getString(R.string.sourcing_offer_updated), Toast.LENGTH_SHORT).show()
            }
        )
    }
    itemEditorTarget?.let { (offer, item) ->
        SupplierOfferLineItemDialog(
            editorKey = offer.id + (item?.id ?: "new"),
            initial = item,
            onDismiss = { itemEditorTarget = null },
            onSave = { draft ->
                if (item == null) supplierRepository.addLineItem(offer.id, draft)
                else supplierRepository.updateLineItem(item.id, draft)
            },
            onSaved = {
                itemEditorTarget = null
                Toast.makeText(context, context.getString(R.string.sourcing_line_item_saved), Toast.LENGTH_SHORT).show()
            }
        )
    }
    evidenceEditorTarget?.let { offer ->
        SupplierOfferEvidenceDialog(
            offer = offer,
            repository = supplierRepository,
            organizationRepository = organizationRepository,
            onDismiss = { evidenceEditorTarget = null },
            onSave = { selected -> supplierRepository.setEvidenceLinks(offer.id, selected) },
            onSaved = {
                evidenceEditorTarget = null
                Toast.makeText(context, context.getString(R.string.sourcing_evidence_updated), Toast.LENGTH_SHORT).show()
            }
        )
    }
    removeItemTarget?.let { (_, item) ->
        AlertDialog(
            onDismissRequest = { removeItemTarget = null },
            title = { Text(stringResource(R.string.sourcing_remove_line_item)) },
            text = { Text(stringResource(R.string.sourcing_remove_line_item_confirm)) },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        runCatching { supplierRepository.deleteLineItem(item.id) }
                            .onSuccess {
                                removeItemTarget = null
                                Toast.makeText(context, context.getString(R.string.sourcing_line_item_removed), Toast.LENGTH_SHORT).show()
                            }
                            .onFailure {
                                removeItemTarget = null
                                Toast.makeText(context, it.message ?: "Could not remove line item.", Toast.LENGTH_LONG).show()
                            }
                    }
                }) { Text(stringResource(R.string.sourcing_remove_line_item)) }
            },
            dismissButton = { TextButton(onClick = { removeItemTarget = null }) { Text(stringResource(R.string.company_cancel)) } }
        )
    }
    reviewTarget?.let { offer ->
        SourcingReviewDialog(
            offerTitle = offer.title,
            initial = reviews.firstOrNull { it.supplierOfferId == offer.id },
            onDismiss = { reviewTarget = null },
            onSave = { status, note ->
                scope.launch {
                    runCatching { supplierRepository.review(selectedRequirementId ?: error("Select a buyer requirement."), offer.id, status, note) }
                        .onFailure { Toast.makeText(context, it.message ?: "Could not save review.", Toast.LENGTH_LONG).show() }
                    reviewTarget = null
                }
            }
        )
    }
}

@Composable
private fun supplierOfferStatusLabel(status: String): Int = when (status) {
    SupplierOfferStatus.DRAFT -> R.string.offer_status_draft
    SupplierOfferStatus.AVAILABLE -> R.string.offer_status_available
    SupplierOfferStatus.ON_HOLD -> R.string.offer_status_on_hold
    SupplierOfferStatus.CLOSED -> R.string.offer_status_closed
    else -> R.string.offer_status_unknown
}

@Composable
private fun SummaryPill(label: String, value: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SupplierOfferComparisonCard(
    offer: SupplierOfferEntity,
    supplierName: String,
    buyerRequirement: BuyerRequirementEntity,
    buyerItems: List<BuyerRequirementLineItemEntity>,
    review: SourcingReviewEntity?,
    repository: SupplierOfferRepository,
    organizationRepository: OrganizationRepository,
    onReview: () -> Unit,
    onSetStatus: (String) -> Unit,
    onEditOffer: () -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (SupplierOfferLineItemEntity) -> Unit,
    onDeleteItem: (SupplierOfferLineItemEntity) -> Unit,
    onManageEvidence: () -> Unit,
    onArchiveOffer: () -> Unit
) {
    var statusMenu by remember(offer.id) { mutableStateOf(false) }
    val items by repository.observeItems(offer.id).collectAsState(initial = emptyList())
    val sources by organizationRepository.observeSources(offer.organizationId).collectAsState(initial = emptyList())
    val evidence by repository.observeEvidence(offer.id).collectAsState(initial = emptyList())
    val sourceMap = remember(sources) { sources.associateBy { it.id } }
    val evidenceTitles = evidence.mapNotNull { sourceMap[it.sourceId]?.title }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(supplierName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(offer.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
                AssistChip(onClick = { statusMenu = true }, label = { Text(stringResource(supplierOfferStatusLabel(offer.status))) })
                DropdownMenu(expanded = statusMenu, onDismissRequest = { statusMenu = false }) {
                    listOf(SupplierOfferStatus.DRAFT, SupplierOfferStatus.AVAILABLE, SupplierOfferStatus.ON_HOLD, SupplierOfferStatus.CLOSED).forEach { status ->
                        DropdownMenuItem(
                            text = { Text(stringResource(supplierOfferStatusLabel(status))) },
                            onClick = { statusMenu = false; onSetStatus(status) }
                        )
                    }
                }
            }
            offer.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            val location = listOfNotNull(offer.originCity, offer.originCountry).joinToString(" · ")
            if (location.isNotBlank()) Text(location, style = MaterialTheme.typography.labelSmall)
            if (buyerItems.isEmpty()) {
                Text(stringResource(R.string.sourcing_add_buyer_line_item_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (items.isEmpty()) {
                Text(stringResource(R.string.sourcing_offer_has_no_items), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                buyerItems.forEach { buyerItem ->
                    Text(buyerItem.productName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    items.forEach { offerItem ->
                        val assessment = SourcingMatchEngine.compare(requirement = buyerRequirement, buyer = buyerItem, supplier = offerItem)
                        Column(Modifier.fillMaxWidth().padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(stringResource(R.string.sourcing_candidate_item, offerItem.productName), style = MaterialTheme.typography.bodySmall)
                            Text(
                                when (assessment.disposition) {
                                    "CONSTRAINT_CONFLICT" -> stringResource(R.string.sourcing_conflict)
                                    "MORE_INFORMATION_NEEDED" -> stringResource(R.string.sourcing_more_info)
                                    else -> stringResource(R.string.sourcing_reviewable)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when (assessment.disposition) {
                                    "CONSTRAINT_CONFLICT" -> MaterialTheme.colorScheme.error
                                    "MORE_INFORMATION_NEEDED" -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                            assessment.checks.forEach { check ->
                                val state = when (check.result) {
                                    SourcingMatchEngine.SUPPORTED -> stringResource(R.string.sourcing_check_supported)
                                    SourcingMatchEngine.CONTRADICTED -> stringResource(R.string.sourcing_check_conflict)
                                    else -> stringResource(R.string.sourcing_check_unknown)
                                }
                                Text("• ${check.field}: $state — ${check.explanation}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            Text(stringResource(R.string.sourcing_offer_line_items), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            items.forEach { offerItem ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(offerItem.productName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        val facts = listOfNotNull(
                            offerItem.material,
                            offerItem.quantityAvailable?.let { "$it " + offerItem.quantityUnit.orEmpty() },
                            offerItem.minimumOrderQuantity?.let { "MOQ " + it },
                            offerItem.unitPriceMinorUnits?.let { it.toString() + " " + offerItem.currency.orEmpty() }
                        ).joinToString(" · ")
                        if (facts.isNotBlank()) Text(facts, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { onEditItem(offerItem) }) { Text(stringResource(R.string.sourcing_edit_line_item)) }
                    TextButton(enabled = items.size > 1, onClick = { onDeleteItem(offerItem) }) { Text(stringResource(R.string.sourcing_remove_line_item)) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onAddItem) { Text(stringResource(R.string.sourcing_add_line_item)) }
                TextButton(onClick = onEditOffer) { Text(stringResource(R.string.sourcing_edit_offer)) }
                TextButton(onClick = onArchiveOffer) { Text(stringResource(R.string.sourcing_move_to_trash)) }
            }
            if (evidenceTitles.isNotEmpty()) {
                Text(stringResource(R.string.sourcing_evidence), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                evidenceTitles.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            } else {
                Text(stringResource(R.string.sourcing_no_offer_evidence), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            }
            TextButton(onClick = onManageEvidence) { Text(stringResource(R.string.sourcing_manage_evidence)) }
            review?.let {
                Text(
                    stringResource(R.string.sourcing_review_state, when (it.status) {
                        SourcingReviewStatus.SHORTLISTED -> stringResource(R.string.sourcing_shortlisted_state)
                        SourcingReviewStatus.NOT_SUITABLE -> stringResource(R.string.sourcing_not_suitable_state)
                        else -> stringResource(R.string.sourcing_needs_information_state)
                    }),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                it.rationale?.let { note -> Text(note, style = MaterialTheme.typography.bodySmall) }
            }
            TextButton(onClick = onReview) { Text(stringResource(R.string.sourcing_review_decision)) }
        }
    }
}

@Composable
private fun SupplierOfferCreateDialog(
    organizations: List<OrganizationEntity>,
    organizationRepository: OrganizationRepository,
    onDismiss: () -> Unit,
    onSave: suspend (String, SupplierOfferDraft, SupplierOfferLineItemDraft, List<String>) -> SupplierOfferEntity,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedOrganizationId by remember { mutableStateOf(organizations.firstOrNull()?.id) }
    val sourcesFlow = remember(selectedOrganizationId) {
        selectedOrganizationId?.let { organizationRepository.observeSources(it) } ?: flowOf(emptyList())
    }
    val sources by sourcesFlow.collectAsState(initial = emptyList())
    var organizationMenu by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var product by remember { mutableStateOf("") }
    var specification by remember { mutableStateOf("") }
    var material by remember { mutableStateOf("") }
    var available by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("pcs") }
    var moq by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("EUR") }
    var leadTime by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var selectedSources by remember { mutableStateOf(setOf<String>()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val selectedOrganization = organizations.firstOrNull { it.id == selectedOrganizationId }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(stringResource(R.string.sourcing_add_offer)) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 580.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                TextButton(onClick = { organizationMenu = true }) { Text(selectedOrganization?.canonicalName ?: stringResource(R.string.sourcing_choose_supplier)) }
                DropdownMenu(expanded = organizationMenu, onDismissRequest = { organizationMenu = false }) {
                    organizations.forEach { org ->
                        DropdownMenuItem(text = { Text(org.canonicalName) }, onClick = { selectedOrganizationId = org.id; selectedSources = emptySet(); organizationMenu = false })
                    }
                }
                OutlinedTextField(title, { title = it.take(200) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sourcing_offer_title)) }, singleLine = true)
                OutlinedTextField(description, { description = it.take(4000) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sourcing_offer_description)) }, minLines = 2, maxLines = 3)
                Text(stringResource(R.string.sourcing_product_details), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(product, { product = it.take(200) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sourcing_product_name)) }, singleLine = true)
                OutlinedTextField(specification, { specification = it.take(2000) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sourcing_specification)) }, minLines = 2, maxLines = 3)
                OutlinedTextField(material, { material = it.take(120) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sourcing_material)) }, singleLine = true)
                OutlinedTextField(available, { available = it.take(40) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sourcing_available_quantity)) }, singleLine = true)
                OutlinedTextField(unit, { unit = it.take(40) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sourcing_quantity_unit)) }, singleLine = true)
                OutlinedTextField(moq, { moq = it.take(40) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sourcing_moq)) }, singleLine = true)
                OutlinedTextField(price, { price = it.take(18) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sourcing_unit_price_minor)) }, singleLine = true)
                OutlinedTextField(currency, { currency = it.take(3) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sourcing_currency)) }, singleLine = true)
                OutlinedTextField(leadTime, { leadTime = it.take(4) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sourcing_lead_time_days)) }, singleLine = true)
                OutlinedTextField(country, { country = it.take(120) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sourcing_origin_country)) }, singleLine = true)
                OutlinedTextField(city, { city = it.take(120) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sourcing_origin_city)) }, singleLine = true)
                if (sources.isNotEmpty()) {
                    Text(stringResource(R.string.sourcing_attach_evidence), style = MaterialTheme.typography.titleSmall)
                    sources.forEach { source ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = source.id in selectedSources, onCheckedChange = { checked -> selectedSources = if (checked) selectedSources + source.id else selectedSources - source.id })
                            Column(Modifier.weight(1f)) {
                                Text(source.title, style = MaterialTheme.typography.bodySmall)
                                Text(source.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    Text(stringResource(R.string.sourcing_save_source_first), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(enabled = !saving && selectedOrganizationId != null && title.isNotBlank() && product.isNotBlank(), onClick = {
                val parsedPrice = price.trim().takeIf { it.isNotBlank() }?.toLongOrNull()
                val parsedLead = leadTime.trim().takeIf { it.isNotBlank() }?.toIntOrNull()
                if (price.isNotBlank() && parsedPrice == null) { error = "Enter price as integer minor units."; return@Button }
                if (leadTime.isNotBlank() && parsedLead == null) { error = "Enter lead time as whole days."; return@Button }
                scope.launch {
                    saving = true
                    val result = runCatching {
                        onSave(
                            selectedOrganizationId!!,
                            SupplierOfferDraft(title, description, currency = currency, originCountry = country, originCity = city),
                            SupplierOfferLineItemDraft(product, specification, material, available, unit, moq, parsedPrice, currency, parsedLead),
                            selectedSources.toList()
                        )
                    }
                    saving = false
                    result.onSuccess { onSaved() }.onFailure { error = it.message ?: "Could not save supplier offer." }
                }
            }) { Text(stringResource(R.string.sourcing_save_offer)) }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text(stringResource(R.string.company_cancel)) } }
    )
}


@Composable
private fun SupplierOfferEditDialog(
    offer: SupplierOfferEntity,
    onDismiss: () -> Unit,
    onSave: suspend (SupplierOfferDraft) -> Unit,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var title by remember(offer.id) { mutableStateOf(offer.title) }
    var description by remember(offer.id) { mutableStateOf(offer.description.orEmpty()) }
    var status by remember(offer.id) { mutableStateOf(offer.status) }
    var currency by remember(offer.id) { mutableStateOf(offer.currency.orEmpty()) }
    var country by remember(offer.id) { mutableStateOf(offer.originCountry.orEmpty()) }
    var city by remember(offer.id) { mutableStateOf(offer.originCity.orEmpty()) }
    var notes by remember(offer.id) { mutableStateOf(offer.notes.orEmpty()) }
    var statusMenu by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(stringResource(R.string.sourcing_edit_offer)) },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                OutlinedTextField(title, { title = it.take(200) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sourcing_offer_title)) }, singleLine = true)
                OutlinedTextField(description, { description = it.take(4000) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sourcing_offer_description)) }, minLines = 2, maxLines = 3)
                TextButton(onClick = { statusMenu = true }) { Text(stringResource(supplierOfferStatusLabel(status))) }
                DropdownMenu(expanded = statusMenu, onDismissRequest = { statusMenu = false }) {
                    listOf(SupplierOfferStatus.DRAFT, SupplierOfferStatus.AVAILABLE, SupplierOfferStatus.ON_HOLD, SupplierOfferStatus.CLOSED).forEach { value ->
                        DropdownMenuItem(
                            text = { Text(stringResource(supplierOfferStatusLabel(value))) },
                            onClick = { status = value; statusMenu = false }
                        )
                    }
                }
                OutlinedTextField(currency, { currency = it.take(3) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sourcing_currency)) }, singleLine = true)
                OutlinedTextField(country, { country = it.take(120) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sourcing_origin_country)) }, singleLine = true)
                OutlinedTextField(city, { city = it.take(120) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sourcing_origin_city)) }, singleLine = true)
                OutlinedTextField(notes, { notes = it.take(2000) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.company_notes)) }, minLines = 2, maxLines = 3)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(enabled = !saving && title.isNotBlank(), onClick = {
                scope.launch {
                    saving = true
                    val result = runCatching {
                        onSave(SupplierOfferDraft(title, description, status, currency, country, city, notes))
                    }
                    saving = false
                    result.onSuccess { onSaved() }.onFailure { error = it.message ?: "Could not update supplier offer." }
                }
            }) { Text(stringResource(R.string.sourcing_save_changes)) }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text(stringResource(R.string.company_cancel)) } }
    )
}

@Composable
private fun SupplierOfferLineItemDialog(
    editorKey: String,
    initial: SupplierOfferLineItemEntity?,
    onDismiss: () -> Unit,
    onSave: suspend (SupplierOfferLineItemDraft) -> Unit,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var product by remember(editorKey) { mutableStateOf(initial?.productName.orEmpty()) }
    var specification by remember(editorKey) { mutableStateOf(initial?.specification.orEmpty()) }
    var material by remember(editorKey) { mutableStateOf(initial?.material.orEmpty()) }
    var available by remember(editorKey) { mutableStateOf(initial?.quantityAvailable.orEmpty()) }
    var unit by remember(editorKey) { mutableStateOf(initial?.quantityUnit ?: "pcs") }
    var moq by remember(editorKey) { mutableStateOf(initial?.minimumOrderQuantity.orEmpty()) }
    var price by remember(editorKey) { mutableStateOf(initial?.unitPriceMinorUnits?.toString().orEmpty()) }
    var currency by remember(editorKey) { mutableStateOf(initial?.currency ?: "EUR") }
    var leadTime by remember(editorKey) { mutableStateOf(initial?.leadTimeDays?.toString().orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(stringResource(if (initial == null) R.string.sourcing_add_line_item else R.string.sourcing_edit_line_item)) },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                OutlinedTextField(product, { product = it.take(200) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sourcing_product_name)) }, singleLine = true)
                OutlinedTextField(specification, { specification = it.take(2000) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sourcing_specification)) }, minLines = 2, maxLines = 3)
                OutlinedTextField(material, { material = it.take(120) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sourcing_material)) }, singleLine = true)
                OutlinedTextField(available, { available = it.take(40) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sourcing_available_quantity)) }, singleLine = true)
                OutlinedTextField(unit, { unit = it.take(40) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sourcing_quantity_unit)) }, singleLine = true)
                OutlinedTextField(moq, { moq = it.take(40) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sourcing_moq)) }, singleLine = true)
                OutlinedTextField(price, { price = it.take(18) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sourcing_unit_price_minor)) }, singleLine = true)
                OutlinedTextField(currency, { currency = it.take(3) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sourcing_currency)) }, singleLine = true)
                OutlinedTextField(leadTime, { leadTime = it.take(4) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sourcing_lead_time_days)) }, singleLine = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(enabled = !saving && product.isNotBlank(), onClick = {
                val parsedPrice = price.trim().takeIf { it.isNotBlank() }?.toLongOrNull()
                val parsedLead = leadTime.trim().takeIf { it.isNotBlank() }?.toIntOrNull()
                if (price.isNotBlank() && parsedPrice == null) { error = "Enter price as integer minor units."; return@Button }
                if (leadTime.isNotBlank() && parsedLead == null) { error = "Enter lead time as whole days."; return@Button }
                scope.launch {
                    saving = true
                    val result = runCatching {
                        onSave(
                            SupplierOfferLineItemDraft(
                                productName = product, specification = specification, material = material,
                                quantityAvailable = available, quantityUnit = unit, minimumOrderQuantity = moq,
                                unitPriceMinorUnits = parsedPrice, currency = currency, leadTimeDays = parsedLead
                            )
                        )
                    }
                    saving = false
                    result.onSuccess { onSaved() }.onFailure { error = it.message ?: "Could not save line item." }
                }
            }) { Text(stringResource(R.string.sourcing_save_changes)) }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text(stringResource(R.string.company_cancel)) } }
    )
}

@Composable
private fun SupplierOfferEvidenceDialog(
    offer: SupplierOfferEntity,
    repository: SupplierOfferRepository,
    organizationRepository: OrganizationRepository,
    onDismiss: () -> Unit,
    onSave: suspend (Set<String>) -> Unit,
    onSaved: () -> Unit
) {
    val sources by organizationRepository.observeSources(offer.organizationId).collectAsState(initial = emptyList())
    val evidence by repository.observeEvidence(offer.id).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var selected by remember(offer.id) { mutableStateOf(emptySet<String>()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(evidence) { selected = evidence.map { it.sourceId }.toSet() }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(stringResource(R.string.sourcing_manage_evidence)) },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                if (sources.isEmpty()) {
                    Text(stringResource(R.string.sourcing_save_source_first), color = MaterialTheme.colorScheme.tertiary)
                } else {
                    sources.forEach { source ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = source.id in selected,
                                onCheckedChange = { checked ->
                                    selected = if (checked) selected + source.id else selected - source.id
                                }
                            )
                            Column(Modifier.weight(1f)) {
                                Text(source.title, style = MaterialTheme.typography.bodySmall)
                                Text(source.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(enabled = !saving, onClick = {
                scope.launch {
                    saving = true
                    val result = runCatching { onSave(selected) }
                    saving = false
                    result.onSuccess { onSaved() }.onFailure { error = it.message ?: "Could not update offer evidence." }
                }
            }) { Text(stringResource(R.string.sourcing_save_changes)) }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text(stringResource(R.string.company_cancel)) } }
    )
}

@Composable
private fun SourcingReviewDialog(
    offerTitle: String,
    initial: SourcingReviewEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var status by remember(offerTitle) { mutableStateOf(initial?.status ?: SourcingReviewStatus.NEEDS_INFORMATION) }
    var rationale by remember(offerTitle) { mutableStateOf(initial?.rationale.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sourcing_review_decision)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(offerTitle, style = MaterialTheme.typography.titleSmall)
                listOf(
                    SourcingReviewStatus.SHORTLISTED to R.string.sourcing_shortlisted_state,
                    SourcingReviewStatus.NEEDS_INFORMATION to R.string.sourcing_needs_information_state,
                    SourcingReviewStatus.NOT_SUITABLE to R.string.sourcing_not_suitable_state
                ).forEach { (value, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.RadioButton(selected = status == value, onClick = { status = value })
                        Text(stringResource(label))
                    }
                }
                OutlinedTextField(rationale, { rationale = it.take(2000) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.sourcing_review_note)) }, minLines = 2, maxLines = 4)
                Text(stringResource(R.string.sourcing_review_warning), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            }
        },
        confirmButton = { Button(onClick = { onSave(status, rationale) }) { Text(stringResource(R.string.company_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.company_cancel)) } }
    )
}
