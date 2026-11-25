package com.yusufculcu.trader.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import com.yusufculcu.trader.data.OrganizationClaimDraft
import com.yusufculcu.trader.data.OrganizationClaimEntity
import com.yusufculcu.trader.data.OrganizationClaimState
import com.yusufculcu.trader.data.OrganizationDraft
import com.yusufculcu.trader.data.OrganizationEntity
import com.yusufculcu.trader.data.OrganizationRepository
import com.yusufculcu.trader.data.OrganizationPersonDraft
import com.yusufculcu.trader.data.OrganizationPersonEntity
import com.yusufculcu.trader.data.BuyerRequirementRepository
import com.yusufculcu.trader.data.BuyerRequirementDraft
import com.yusufculcu.trader.data.BuyerRequirementLineItemDraft
import com.yusufculcu.trader.data.BuyerRequirementLineItemEntity
import com.yusufculcu.trader.data.BuyerRequirementEntity
import com.yusufculcu.trader.data.BuyerRequirementState
import com.yusufculcu.trader.data.OrganizationSourceDraft
import com.yusufculcu.trader.data.OrganizationSourceEntity
import com.yusufculcu.trader.data.OrganizationSourceKind
import com.yusufculcu.trader.discovery.DiscoveryDocument
import kotlinx.coroutines.launch

@Composable
fun OrganizationsScreen(
    repository: OrganizationRepository,
    incomingDocument: DiscoveryDocument? = null,
    onIncomingDocumentHandled: () -> Unit = {},
    onOpenSource: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val organizations by repository.observeOrganizations().collectAsState(initial = emptyList())
    var selectedOrganizationId by rememberSaveable { mutableStateOf<String?>(null) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var showDiscoveryDialog by remember { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(incomingDocument?.id, incomingDocument?.url) {
        if (incomingDocument != null) {
            selectedOrganizationId = null
            showDiscoveryDialog = true
        }
    }

    if (selectedOrganizationId != null) {
        OrganizationDossierScreen(
            organizationId = selectedOrganizationId!!,
            repository = repository,
            onBack = { selectedOrganizationId = null },
            onOpenSource = onOpenSource,
            modifier = modifier
        )
    } else {
        val query = search.trim()
        val visible = remember(organizations, query) {
            organizations.filter { organization ->
                query.isBlank() || listOfNotNull(
                    organization.canonicalName, organization.domain, organization.country,
                    organization.city, organization.industry
                ).any { it.contains(query, ignoreCase = true) }
            }
        }
        Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.companies_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.companies_intro), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { showCreateDialog = true }, modifier = Modifier.fillMaxWidth()) {
                androidx.compose.material3.Icon(Icons.Rounded.Add, contentDescription = null)
                Text(stringResource(R.string.companies_create))
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.companies_search)) }
            )
            Spacer(Modifier.height(12.dp))
            if (visible.isEmpty()) {
                Text(
                    stringResource(if (organizations.isEmpty()) R.string.companies_empty else R.string.pipeline_no_results),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 28.dp)
                ) {
                    items(visible, key = { it.id }) { organization ->
                        Card(
                            onClick = { selectedOrganizationId = organization.id },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(organization.canonicalName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                val location = listOfNotNull(organization.city, organization.country).joinToString(" · ")
                                val meta = listOfNotNull(organization.domain, location.takeIf { it.isNotBlank() }).joinToString(" · ")
                                if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                organization.industry?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        OrganizationEditorDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { draft -> repository.createOrganization(draft).id },
            onSaved = { id ->
                selectedOrganizationId = id
                showCreateDialog = false
            }
        )
    }

    if (showDiscoveryDialog && incomingDocument != null) {
        OrganizationEditorDialog(
            source = incomingDocument,
            onDismiss = {
                showDiscoveryDialog = false
                onIncomingDocumentHandled()
            },
            onSave = { draft -> repository.createFromDiscovery(draft, incomingDocument).id },
            onSaved = { id ->
                selectedOrganizationId = id
                showDiscoveryDialog = false
                onIncomingDocumentHandled()
            }
        )
    }
}

@Composable
private fun OrganizationDossierScreen(
    organizationId: String,
    repository: OrganizationRepository,
    onBack: () -> Unit,
    onOpenSource: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val organization by repository.observeOrganization(organizationId).collectAsState(initial = null)
    val sources by repository.observeSources(organizationId).collectAsState(initial = emptyList())
    val claims by repository.observeClaims(organizationId).collectAsState(initial = emptyList())
    val people by repository.observePeople(organizationId).collectAsState(initial = emptyList())
    val buyerRepository = remember(repository) { repository.buyerRequirementRepository() }
    val buyerRequirements by buyerRepository.observeForOrganization(organizationId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showEditDialog by remember { mutableStateOf(false) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var showPersonDialog by remember { mutableStateOf(false) }
    var showBuyerRequirementDialog by remember { mutableStateOf(false) }
    var showBuyerLineItemDialog by remember { mutableStateOf(false) }
    var buyerEvidenceRequirement by remember { mutableStateOf<BuyerRequirementEntity?>(null) }
    var lineItemRequirement by remember { mutableStateOf<BuyerRequirementEntity?>(null) }
    var editingRequirement by remember { mutableStateOf<BuyerRequirementEntity?>(null) }
    var editingLineItem by remember { mutableStateOf<BuyerRequirementLineItemEntity?>(null) }
    var editingPerson by remember { mutableStateOf<OrganizationPersonEntity?>(null) }
    var claimSource by remember { mutableStateOf<OrganizationSourceEntity?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    val current = organization
    if (current == null) {
        Column(modifier.fillMaxSize().padding(20.dp)) {
            TextButton(onClick = onBack) {
                androidx.compose.material3.Icon(Icons.Rounded.ArrowBack, contentDescription = null)
                Text(stringResource(R.string.company_back_to_list))
            }
            Text(stringResource(R.string.company_profile_unknown), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val supported = claims.count { it.reviewState == OrganizationClaimState.SUPPORTED }
    val contradicted = claims.count { it.reviewState == OrganizationClaimState.CONTRADICTED }
    val unreviewed = claims.count { it.reviewState == OrganizationClaimState.UNREVIEWED }
    val missingFields = buildList {
        if (current.websiteUrl.isNullOrBlank()) add(stringResource(R.string.company_website_field))
        if (current.country.isNullOrBlank()) add(stringResource(R.string.company_country_field))
        if (current.industry.isNullOrBlank()) add(stringResource(R.string.company_industry_field))
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) {
            androidx.compose.material3.Icon(Icons.Rounded.ArrowBack, contentDescription = null)
            Text(stringResource(R.string.company_back_to_list))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(current.canonicalName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                current.domain?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary) }
            }
            TextButton(onClick = { showEditDialog = true }) { Text(stringResource(R.string.company_edit_profile)) }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.company_profile), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                current.websiteUrl?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                val location = listOfNotNull(current.city, current.country).joinToString(" · ")
                if (location.isNotBlank()) Text(location, style = MaterialTheme.typography.bodySmall)
                current.industry?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                if (missingFields.isNotEmpty()) {
                    Text(stringResource(R.string.company_profile_gaps, missingFields.joinToString(" · ")), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                } else {
                    Text(stringResource(R.string.company_profile_complete), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                current.notes?.let {
                    Text(stringResource(R.string.company_notes_unverified), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.company_contacts), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(onClick = { editingPerson = null; showPersonDialog = true }) { Text(stringResource(R.string.company_add_contact)) }
        }
        if (people.isEmpty()) Text(stringResource(R.string.company_no_contacts), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        else people.forEach { person ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(person.fullName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    person.role?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) }
                    person.email?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    person.phone?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    person.profileUrl?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary) }
                    person.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    TextButton(onClick = { editingPerson = person; showPersonDialog = true }) { Text(stringResource(R.string.company_edit_contact)) }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.buyer_requirements), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(onClick = { editingRequirement = null; showBuyerRequirementDialog = true }) { Text(stringResource(R.string.buyer_add_requirement)) }
        }
        if (buyerRequirements.isEmpty()) Text(stringResource(R.string.buyer_no_requirements), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        else buyerRequirements.forEach { requirement ->
            BuyerRequirementCard(
                requirement = requirement, repository = buyerRepository, sources = sources,
                onEdit = { editingRequirement = requirement; showBuyerRequirementDialog = true },
                onAddItem = { lineItemRequirement = requirement; editingLineItem = null; showBuyerLineItemDialog = true },
                onEditItem = { item -> lineItemRequirement = requirement; editingLineItem = item; showBuyerLineItemDialog = true },
                onLinkEvidence = { buyerEvidenceRequirement = requirement },
                onDelete = { scope.launch { buyerRepository.deleteRequirement(requirement.id) } }
            )
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(stringResource(R.string.company_claims), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.company_claim_counts, supported, contradicted, unreviewed), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.company_unreviewed_notice), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.company_sources), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(onClick = { showSourceDialog = true }) { Text(stringResource(R.string.company_add_source)) }
        }
        if (sources.isEmpty()) {
            Text(stringResource(R.string.company_no_sources), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            sources.forEach { source ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(if (source.sourceKind == OrganizationSourceKind.DISCOVERY_RESULT) R.string.company_source_discovery else R.string.company_source_manual), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(source.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(source.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        if (source.excerpt.isNotBlank()) Text(source.excerpt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onOpenSource(source.url) }) { Text(stringResource(R.string.company_open_source)) }
                            TextButton(onClick = { claimSource = source }) { Text(stringResource(R.string.company_add_claim)) }
                        }
                    }
                }
            }
        }

        Text(stringResource(R.string.company_claims), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (claims.isEmpty()) {
            Text(stringResource(R.string.company_no_claims), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val sourceById = remember(sources) { sources.associateBy { it.id } }
            claims.forEach { claim ->
                OrganizationClaimCard(
                    claim = claim,
                    source = sourceById[claim.sourceId],
                    onReview = { state ->
                        scope.launch {
                            runCatching { repository.reviewClaim(claim.id, state) }
                                .onFailure { error = it.message ?: "Claim review failed." }
                        }
                    }
                )
            }
        }
        notice?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Spacer(Modifier.height(28.dp))
    }

    if (showEditDialog) {
        OrganizationEditorDialog(
            initial = current,
            onDismiss = { showEditDialog = false },
            onSave = { draft -> repository.updateOrganization(organizationId, draft).id },
            onSaved = {
                showEditDialog = false
                notice = "Profile saved."
            }
        )
    }
    if (showPersonDialog) {
        OrganizationPersonDialog(
            initial = editingPerson,
            onDismiss = { showPersonDialog = false; editingPerson = null },
            onSave = { draft -> val selected = editingPerson; if (selected == null) repository.addPerson(organizationId, draft).id else repository.updatePerson(selected.id, draft).id },
            onDelete = { personId -> repository.deletePerson(personId) },
            onSaved = { _, deleted -> showPersonDialog = false; editingPerson = null; notice = if (deleted) "Contact deleted." else "Contact saved. No identity merge was performed." }
        )
    }
    if (showBuyerRequirementDialog) {
        val selected = editingRequirement
        BuyerRequirementEditorDialog(
            initial = selected,
            onDismiss = { showBuyerRequirementDialog = false; editingRequirement = null },
            onSave = { draft ->
                if (selected == null) buyerRepository.create(organizationId, draft).id
                else buyerRepository.update(selected.id, draft).id
            },
            onSaved = {
                showBuyerRequirementDialog = false
                editingRequirement = null
                notice = "Buyer requirement saved."
            }
        )
    }
    lineItemRequirement?.let { requirement ->
        if (showBuyerLineItemDialog) BuyerRequirementLineItemDialog(
            requirementTitle = requirement.title,
            initial = editingLineItem,
            onDismiss = { showBuyerLineItemDialog = false; lineItemRequirement = null; editingLineItem = null },
            onSave = { draft ->
                val selected = editingLineItem
                if (selected == null) buyerRepository.addLineItem(requirement.id, draft).id
                else buyerRepository.updateLineItem(selected.id, draft).id
            },
            onSaved = {
                showBuyerLineItemDialog = false
                lineItemRequirement = null
                editingLineItem = null
                notice = "Line item saved."
            }
        )
    }
    buyerEvidenceRequirement?.let { requirement ->
        BuyerRequirementEvidenceDialog(
            requirementTitle = requirement.title, sources = sources,
            onDismiss = { buyerEvidenceRequirement = null },
            onSave = { sourceId -> buyerRepository.linkEvidence(requirement.id, sourceId).id },
            onSaved = { buyerEvidenceRequirement = null; notice = "Evidence linked." }
        )
    }
    if (showSourceDialog) {
        OrganizationSourceDialog(
            onDismiss = { showSourceDialog = false },
            onSave = { draft -> repository.addSource(organizationId, draft).id },
            onSaved = {
                showSourceDialog = false
                notice = "Source saved."
            }
        )
    }
    claimSource?.let { source ->
        OrganizationClaimDialog(
            source = source,
            onDismiss = { claimSource = null },
            onSave = { field, statement ->
                repository.addClaim(organizationId, OrganizationClaimDraft(source.id, field, statement)).id
            },
            onSaved = {
                claimSource = null
                notice = "Claim added as unreviewed."
            }
        )
    }
}


private fun buyerRequirementStatusLabel(status: String): Int = when (status) {
    BuyerRequirementState.DRAFT -> R.string.buyer_status_draft
    BuyerRequirementState.OPEN -> R.string.buyer_status_open
    BuyerRequirementState.PAUSED -> R.string.buyer_status_paused
    BuyerRequirementState.FULFILLED -> R.string.buyer_status_fulfilled
    BuyerRequirementState.CLOSED -> R.string.buyer_status_closed
    else -> R.string.buyer_status_unknown
}

@Composable
private fun BuyerRequirementCard(
    requirement: BuyerRequirementEntity,
    repository: BuyerRequirementRepository,
    sources: List<OrganizationSourceEntity>,
    onEdit: () -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (BuyerRequirementLineItemEntity) -> Unit,
    onLinkEvidence: () -> Unit,
    onDelete: () -> Unit
) {
    val items by repository.observeItems(requirement.id).collectAsState(initial = emptyList())
    val evidence by repository.observeEvidence(requirement.id).collectAsState(initial = emptyList())
    val sourceById = remember(sources) { sources.associateBy { it.id } }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var statusMenu by remember(requirement.id) { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(requirement.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = onEdit) { Text(stringResource(R.string.buyer_edit_requirement)) }
                AssistChip(
                    onClick = { statusMenu = true },
                    label = { Text(stringResource(buyerRequirementStatusLabel(requirement.status))) }
                )
                DropdownMenu(expanded = statusMenu, onDismissRequest = { statusMenu = false }) {
                    listOf(BuyerRequirementState.DRAFT, BuyerRequirementState.OPEN, BuyerRequirementState.PAUSED, BuyerRequirementState.FULFILLED, BuyerRequirementState.CLOSED).forEach { status ->
                        DropdownMenuItem(
                            text = { Text(stringResource(buyerRequirementStatusLabel(status))) },
                            onClick = {
                                statusMenu = false
                                scope.launch {
                                    runCatching { repository.setStatus(requirement.id, status) }
                                        .onFailure { Toast.makeText(context, it.message ?: context.getString(R.string.buyer_status_update_failed), Toast.LENGTH_LONG).show() }
                                }
                            }
                        )
                    }
                }
            }
            requirement.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            val destination = listOfNotNull(requirement.destinationCity, requirement.destinationCountry).joinToString(" · ")
            if (destination.isNotBlank()) Text(stringResource(R.string.buyer_destination, destination), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            requirement.requiredByText?.let { Text(stringResource(R.string.buyer_required_by, it), style = MaterialTheme.typography.labelSmall) }
            if (items.isEmpty()) Text(stringResource(R.string.buyer_no_items), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            items.forEach { item ->
                Row(Modifier.fillMaxWidth().padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("• ${item.productName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        item.specification?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        val quantity = listOfNotNull(item.quantity, item.unit).joinToString(" ")
                        if (quantity.isNotBlank()) Text(quantity, style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = { onEditItem(item) }) { Text(stringResource(R.string.buyer_edit_line_item)) }
                }
            }
            if (evidence.isNotEmpty()) {
                Text(stringResource(R.string.buyer_evidence), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                evidence.forEach { link -> sourceById[link.sourceId]?.let { Text(it.title, style = MaterialTheme.typography.bodySmall) } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onAddItem) { Text(stringResource(R.string.buyer_add_line_item)) }
                TextButton(onClick = onLinkEvidence) { Text(stringResource(R.string.buyer_link_evidence)) }
                TextButton(onClick = onDelete) { Text(stringResource(R.string.buyer_delete)) }
            }
        }
    }
}

@Composable
private fun BuyerRequirementEditorDialog(
    initial: BuyerRequirementEntity?,
    onDismiss: () -> Unit,
    onSave: suspend (BuyerRequirementDraft) -> String,
    onSaved: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var description by remember(initial) { mutableStateOf(initial?.description.orEmpty()) }
    var country by remember(initial) { mutableStateOf(initial?.destinationCountry.orEmpty()) }
    var city by remember(initial) { mutableStateOf(initial?.destinationCity.orEmpty()) }
    var requiredBy by remember(initial) { mutableStateOf(initial?.requiredByText.orEmpty()) }
    var currency by remember(initial) { mutableStateOf(initial?.currency.orEmpty()) }
    var budget by remember(initial) { mutableStateOf(initial?.budgetMinorUnits?.toString().orEmpty()) }
    var notes by remember(initial) { mutableStateOf(initial?.notes.orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(stringResource(if (initial == null) R.string.buyer_add_requirement else R.string.buyer_edit_requirement)) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it.take(200); error = null }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.buyer_title)) }, singleLine = true)
                OutlinedTextField(description, { description = it.take(4_000) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.buyer_description)) }, minLines = 2, maxLines = 4)
                OutlinedTextField(country, { country = it.take(120) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.buyer_country)) }, singleLine = true)
                OutlinedTextField(city, { city = it.take(120) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.buyer_city)) }, singleLine = true)
                OutlinedTextField(requiredBy, { requiredBy = it.take(120) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.buyer_required_by_label)) }, singleLine = true)
                OutlinedTextField(currency, { currency = it.take(3) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.buyer_currency)) }, singleLine = true)
                OutlinedTextField(budget, { budget = it.take(18); error = null }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.buyer_budget_minor)) }, singleLine = true)
                OutlinedTextField(notes, { notes = it.take(2_000) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.buyer_notes)) }, minLines = 2, maxLines = 4)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(enabled = !saving && title.isNotBlank(), onClick = {
                val parsedBudget = budget.trim().takeIf { it.isNotBlank() }?.toLongOrNull()
                if (budget.isNotBlank() && parsedBudget == null) { error = "Enter budget as integer minor units."; return@Button }
                scope.launch {
                    saving = true
                    val draft = BuyerRequirementDraft(title, description, initial?.status ?: BuyerRequirementState.OPEN, country, city, requiredBy, currency, parsedBudget, notes)
                    val result = runCatching { onSave(draft) }
                    saving = false
                    result.onSuccess(onSaved).onFailure { error = it.message ?: "Could not save buyer requirement." }
                }
            }) { Text(stringResource(R.string.company_save)) }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text(stringResource(R.string.company_cancel)) } }
    )
}

@Composable
private fun BuyerRequirementLineItemDialog(
    requirementTitle: String,
    initial: BuyerRequirementLineItemEntity?,
    onDismiss: () -> Unit,
    onSave: suspend (BuyerRequirementLineItemDraft) -> String,
    onSaved: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember(initial) { mutableStateOf(initial?.productName.orEmpty()) }
    var specification by remember(initial) { mutableStateOf(initial?.specification.orEmpty()) }
    var quantity by remember(initial) { mutableStateOf(initial?.quantity.orEmpty()) }
    var unit by remember(initial) { mutableStateOf(initial?.unit.orEmpty()) }
    var targetPrice by remember(initial) { mutableStateOf(initial?.targetUnitPriceMinorUnits?.toString().orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(stringResource(if (initial == null) R.string.buyer_add_line_item else R.string.buyer_edit_line_item)) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 540.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(requirementTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                OutlinedTextField(name, { name = it.take(200); error = null }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.buyer_product_name)) }, singleLine = true)
                OutlinedTextField(specification, { specification = it.take(2_000) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.buyer_specification)) }, minLines = 2, maxLines = 4)
                OutlinedTextField(quantity, { quantity = it.take(40); error = null }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.buyer_quantity)) }, singleLine = true)
                OutlinedTextField(unit, { unit = it.take(40) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.buyer_unit)) }, singleLine = true)
                OutlinedTextField(targetPrice, { targetPrice = it.take(18); error = null }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.buyer_target_price_minor)) }, singleLine = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(enabled = !saving && name.isNotBlank(), onClick = {
                val price = targetPrice.trim().takeIf { it.isNotBlank() }?.toLongOrNull()
                if (targetPrice.isNotBlank() && price == null) { error = "Enter target price as integer minor units."; return@Button }
                scope.launch {
                    saving = true
                    val result = runCatching { onSave(BuyerRequirementLineItemDraft(name, specification, quantity, unit, price)) }
                    saving = false
                    result.onSuccess(onSaved).onFailure { error = it.message ?: "Could not save line item." }
                }
            }) { Text(stringResource(R.string.company_save)) }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text(stringResource(R.string.company_cancel)) } }
    )
}

@Composable
private fun BuyerRequirementEvidenceDialog(requirementTitle:String,sources:List<OrganizationSourceEntity>,onDismiss:()->Unit,onSave:suspend(String)->String,onSaved:(String)->Unit) {
    val scope=rememberCoroutineScope();var savingId by remember{mutableStateOf<String?>(null)};var error by remember{mutableStateOf<String?>(null)}
    AlertDialog(onDismissRequest={if(savingId==null)onDismiss()},title={Text(stringResource(R.string.buyer_link_evidence))},text={
        Column(Modifier.fillMaxWidth().heightIn(max=480.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text(requirementTitle,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.secondary)
            if(sources.isEmpty())Text(stringResource(R.string.buyer_no_saved_sources),style=MaterialTheme.typography.bodySmall)
            sources.forEach{source->TextButton(enabled=savingId==null,onClick={scope.launch{savingId=source.id;val result=runCatching{onSave(source.id)};savingId=null;result.onSuccess(onSaved).onFailure{error=it.message?:"Could not link evidence."}}}){Text(source.title)}}
            error?.let{Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)}
        }
    },confirmButton={TextButton(onClick=onDismiss,enabled=savingId==null){Text(stringResource(R.string.company_cancel))}})
}

@Composable
private fun OrganizationPersonDialog(initial: OrganizationPersonEntity?, onDismiss: () -> Unit, onSave: suspend (OrganizationPersonDraft) -> String, onDelete: suspend (String) -> Boolean, onSaved: (String, Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember(initial) { mutableStateOf(initial?.fullName.orEmpty()) }
    var role by remember(initial) { mutableStateOf(initial?.role.orEmpty()) }
    var email by remember(initial) { mutableStateOf(initial?.email.orEmpty()) }
    var phone by remember(initial) { mutableStateOf(initial?.phone.orEmpty()) }
    var profile by remember(initial) { mutableStateOf(initial?.profileUrl.orEmpty()) }
    var notes by remember(initial) { mutableStateOf(initial?.notes.orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, title = { Text(stringResource(if (initial == null) R.string.company_add_contact else R.string.company_edit_contact)) }, text = {
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it.take(160); error = null }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.company_contact_name)) }, singleLine = true)
            OutlinedTextField(role, { role = it.take(160) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.company_contact_role)) }, singleLine = true)
            OutlinedTextField(email, { email = it.take(254) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.company_contact_email)) }, singleLine = true)
            OutlinedTextField(phone, { phone = it.take(64) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.company_contact_phone)) }, singleLine = true)
            OutlinedTextField(profile, { profile = it.take(2048) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.company_contact_profile)) }, singleLine = true)
            OutlinedTextField(notes, { notes = it.take(2000) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.company_contact_notes)) }, minLines = 2, maxLines = 4)
            Text(stringResource(R.string.company_contact_identity_notice), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }, confirmButton = { Row {
        if (initial != null) TextButton(enabled = !saving, onClick = { scope.launch { saving = true; val result = runCatching { onDelete(initial.id) }; saving = false; result.onSuccess { deleted -> if (deleted) onSaved(initial.id, true) else error = "Contact no longer exists." }.onFailure { error = it.message ?: "Could not delete contact." } } }) { Text(stringResource(R.string.company_delete_contact)) }
        Button(enabled = !saving && name.isNotBlank(), onClick = { scope.launch { saving = true; val result = runCatching { onSave(OrganizationPersonDraft(name, role, email, phone, profile, notes)) }; saving = false; result.onSuccess { id -> onSaved(id, false) }.onFailure { error = it.message ?: "Could not save contact." } } }) { Text(stringResource(R.string.company_save)) }
    } }, dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text(stringResource(R.string.company_cancel)) } })
}

@Composable
private fun OrganizationClaimCard(
    claim: OrganizationClaimEntity,
    source: OrganizationSourceEntity?,
    onReview: (String) -> Unit
) {
    val stateLabel = when (claim.reviewState) {
        OrganizationClaimState.SUPPORTED -> R.string.company_claim_supported
        OrganizationClaimState.CONTRADICTED -> R.string.company_claim_contradicted
        OrganizationClaimState.REJECTED -> R.string.company_claim_rejected
        else -> R.string.company_claim_unreviewed
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(claim.field, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                AssistChip(onClick = {}, enabled = false, label = { Text(stringResource(stateLabel)) })
            }
            Text(claim.statement, style = MaterialTheme.typography.bodyMedium)
            source?.let {
                Text(stringResource(R.string.company_claim_source, it.title), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Text(it.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (claim.reviewState == OrganizationClaimState.UNREVIEWED) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { onReview(OrganizationClaimState.SUPPORTED) }) { Text(stringResource(R.string.company_mark_supported)) }
                    TextButton(onClick = { onReview(OrganizationClaimState.CONTRADICTED) }) { Text(stringResource(R.string.company_mark_contradicted)) }
                }
                TextButton(onClick = { onReview(OrganizationClaimState.REJECTED) }) { Text(stringResource(R.string.company_reject_claim)) }
            } else {
                TextButton(onClick = { onReview(OrganizationClaimState.UNREVIEWED) }) { Text(stringResource(R.string.company_reset_review)) }
            }
            claim.reviewNote?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun OrganizationEditorDialog(
    initial: OrganizationEntity? = null,
    source: DiscoveryDocument? = null,
    onDismiss: () -> Unit,
    onSave: suspend (OrganizationDraft) -> String,
    onSaved: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember(initial, source) { mutableStateOf(initial?.canonicalName.orEmpty()) }
    var website by remember(initial) { mutableStateOf(initial?.websiteUrl.orEmpty()) }
    var country by remember(initial) { mutableStateOf(initial?.country.orEmpty()) }
    var city by remember(initial) { mutableStateOf(initial?.city.orEmpty()) }
    var industry by remember(initial) { mutableStateOf(initial?.industry.orEmpty()) }
    var notes by remember(initial) { mutableStateOf(initial?.notes.orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(stringResource(if (initial == null) R.string.companies_create else R.string.company_edit_profile)) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (source != null) {
                    Text(stringResource(R.string.company_source_attached), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(source.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(source.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text(stringResource(R.string.company_source_warning), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedTextField(name, { name = it.take(200); error = null }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.company_name)) }, singleLine = true)
                OutlinedTextField(website, { website = it.take(2_048); error = null }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.company_website)) }, singleLine = true)
                OutlinedTextField(country, { country = it.take(120) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.company_country)) }, singleLine = true)
                OutlinedTextField(city, { city = it.take(120) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.company_city)) }, singleLine = true)
                OutlinedTextField(industry, { industry = it.take(160) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.company_industry)) }, singleLine = true)
                OutlinedTextField(notes, { notes = it.take(4_000) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.company_notes)) }, minLines = 2, maxLines = 5)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val draft = OrganizationDraft(name, website, country, city, industry, notes)
                scope.launch {
                    saving = true
                    val result = runCatching { onSave(draft) }
                    saving = false
                    result.onSuccess(onSaved).onFailure { error = it.message ?: "Could not save the company dossier." }
                }
            }, enabled = !saving && name.isNotBlank()) { Text(stringResource(R.string.company_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text(stringResource(R.string.company_cancel)) } }
    )
}

@Composable
private fun OrganizationSourceDialog(
    onDismiss: () -> Unit,
    onSave: suspend (OrganizationSourceDraft) -> String,
    onSaved: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var excerpt by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(stringResource(R.string.company_add_source)) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it.take(300); error = null }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.company_source_title)) }, singleLine = true)
                OutlinedTextField(url, { url = it.take(2_048); error = null }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.company_source_url)) }, singleLine = true)
                OutlinedTextField(excerpt, { excerpt = it.take(8_000) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.company_source_excerpt)) }, minLines = 3, maxLines = 8)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                scope.launch {
                    saving = true
                    val draft = OrganizationSourceDraft(OrganizationSourceKind.MANUAL_REFERENCE, title, url, excerpt)
                    val result = runCatching { onSave(draft) }
                    saving = false
                    result.onSuccess(onSaved).onFailure { error = it.message ?: "Could not save source." }
                }
            }, enabled = !saving && title.isNotBlank() && url.isNotBlank()) { Text(stringResource(R.string.company_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text(stringResource(R.string.company_cancel)) } }
    )
}

@Composable
private fun OrganizationClaimDialog(
    source: OrganizationSourceEntity,
    onDismiss: () -> Unit,
    onSave: suspend (String, String) -> String,
    onSaved: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var field by remember { mutableStateOf("") }
    var statement by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(stringResource(R.string.company_add_claim)) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 500.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.company_claim_source, source.title), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                OutlinedTextField(field, { field = it.take(80); error = null }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.company_claim_field)) }, singleLine = true)
                OutlinedTextField(statement, { statement = it.take(2_000); error = null }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.company_claim_statement)) }, minLines = 3, maxLines = 7)
                Text(stringResource(R.string.company_unreviewed_notice), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                scope.launch {
                    saving = true
                    val result = runCatching { onSave(field, statement) }
                    saving = false
                    result.onSuccess(onSaved).onFailure { error = it.message ?: "Could not add claim." }
                }
            }, enabled = !saving && field.isNotBlank() && statement.isNotBlank()) { Text(stringResource(R.string.company_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text(stringResource(R.string.company_cancel)) } }
    )
}
