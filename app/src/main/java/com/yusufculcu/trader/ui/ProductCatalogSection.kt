package com.yusufculcu.trader.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yusufculcu.trader.R
import com.yusufculcu.trader.data.OrganizationSourceEntity
import com.yusufculcu.trader.data.ProductCatalogRepository
import com.yusufculcu.trader.data.ProductDraft
import com.yusufculcu.trader.data.ProductEntity
import com.yusufculcu.trader.data.ProductStatus
import kotlinx.coroutines.launch

@Composable
fun ProductCatalogSection(
    organizationId: String,
    repository: ProductCatalogRepository,
    sources: List<OrganizationSourceEntity>,
    modifier: Modifier = Modifier
) {
    val products by repository.observeForOrganization(organizationId).collectAsState(initial = emptyList())
    val productSavedText = stringResource(R.string.product_saved)
    val productStatusSavedText = stringResource(R.string.product_status_saved)
    val productEvidenceSavedText = stringResource(R.string.product_evidence_saved)
    var editing by remember { mutableStateOf<ProductEntity?>(null) }
    var editorVisible by remember { mutableStateOf(false) }
    var evidenceTarget by remember { mutableStateOf<ProductEntity?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.product_catalog_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { editing = null; editorVisible = true; error = null }) {
                Text(stringResource(R.string.product_add))
            }
        }
        Text(stringResource(R.string.product_catalog_intro), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (products.isEmpty()) {
            Text(stringResource(R.string.product_empty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            products.forEach { product ->
                ProductCard(
                    product = product,
                    repository = repository,
                    sources = sources,
                    onEdit = { editing = product; editorVisible = true; error = null },
                    onManageEvidence = { evidenceTarget = product; error = null },
                    onError = { error = it },
                    onStatusSaved = { notice = productStatusSavedText; error = null }
                )
            }
        }
        notice?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }

    if (editorVisible) {
        ProductEditorDialog(
            initial = editing,
            onDismiss = { editorVisible = false; editing = null },
            onSave = { draft ->
                val selected = editing
                if (selected == null) repository.create(organizationId, draft)
                else repository.update(selected.id, draft)
            },
            onSaved = {
                editorVisible = false
                editing = null
                notice = productSavedText
                error = null
            }
        )
    }
    evidenceTarget?.let { product ->
        ProductEvidenceDialog(
            product = product,
            repository = repository,
            sources = sources,
            onDismiss = { evidenceTarget = null },
            onSave = { selected -> repository.setEvidenceLinks(product.id, selected) },
            onSaved = {
                evidenceTarget = null
                notice = productEvidenceSavedText
                error = null
            }
        )
    }
}

@Composable
private fun ProductCard(
    product: ProductEntity,
    repository: ProductCatalogRepository,
    sources: List<OrganizationSourceEntity>,
    onEdit: () -> Unit,
    onManageEvidence: () -> Unit,
    onError: (String) -> Unit,
    onStatusSaved: () -> Unit
) {
    val evidence by repository.observeEvidence(product.id).collectAsState(initial = emptyList())
    val sourceById = remember(sources) { sources.associateBy { it.id } }
    val scope = rememberCoroutineScope()
    var statusMenu by remember(product.id) { mutableStateOf(false) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                AssistChip(onClick = { statusMenu = true }, label = { Text(stringResource(productStatusLabel(product.status))) })
                DropdownMenu(expanded = statusMenu, onDismissRequest = { statusMenu = false }) {
                    listOf(ProductStatus.DRAFT, ProductStatus.ACTIVE, ProductStatus.DISCONTINUED, ProductStatus.ARCHIVED).forEach { status ->
                        DropdownMenuItem(
                            text = { Text(stringResource(productStatusLabel(status))) },
                            onClick = {
                                statusMenu = false
                                scope.launch {
                                    runCatching { repository.setStatus(product.id, status) }
                                        .onSuccess { changed ->
                                            if (changed) onStatusSaved() else onError("Product record no longer exists.")
                                        }
                                        .onFailure { onError(it.message ?: "Product status update failed.") }
                                }
                            }
                        )
                    }
                }
            }
            product.skuCode?.let { Text(stringResource(R.string.product_sku_value, it), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary) }
            product.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            product.specification?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            val materialAndUnit = listOfNotNull(product.material, product.unit).joinToString(" · ")
            if (materialAndUnit.isNotBlank()) Text(materialAndUnit, style = MaterialTheme.typography.labelSmall)
            if (evidence.isEmpty()) {
                Text(stringResource(R.string.product_no_evidence), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            } else {
                Text(stringResource(R.string.product_evidence), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                evidence.forEach { link -> sourceById[link.sourceId]?.let { Text(it.title, style = MaterialTheme.typography.bodySmall) } }
            }
            Row {
                TextButton(onClick = onEdit) { Text(stringResource(R.string.product_edit)) }
                TextButton(onClick = onManageEvidence) { Text(stringResource(R.string.product_manage_evidence)) }
            }
        }
    }
}

@Composable
private fun ProductEditorDialog(
    initial: ProductEntity?,
    onDismiss: () -> Unit,
    onSave: suspend (ProductDraft) -> ProductEntity,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var sku by remember(initial) { mutableStateOf(initial?.skuCode.orEmpty()) }
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var description by remember(initial) { mutableStateOf(initial?.description.orEmpty()) }
    var specification by remember(initial) { mutableStateOf(initial?.specification.orEmpty()) }
    var material by remember(initial) { mutableStateOf(initial?.material.orEmpty()) }
    var unit by remember(initial) { mutableStateOf(initial?.unit.orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(stringResource(if (initial == null) R.string.product_add else R.string.product_edit)) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 540.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(sku, { sku = it.take(80); error = null }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.product_sku)) }, singleLine = true)
                OutlinedTextField(name, { name = it.take(200); error = null }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.product_name)) }, singleLine = true)
                OutlinedTextField(description, { description = it.take(4_000) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.product_description)) }, minLines = 2, maxLines = 3)
                OutlinedTextField(specification, { specification = it.take(2_000) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.product_specification)) }, minLines = 2, maxLines = 3)
                OutlinedTextField(material, { material = it.take(120) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.product_material)) }, singleLine = true)
                OutlinedTextField(unit, { unit = it.take(40) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.product_unit)) }, singleLine = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(enabled = !saving && name.isNotBlank(), onClick = {
                scope.launch {
                    saving = true
                    val draft = ProductDraft(
                        skuCode = sku,
                        name = name,
                        description = description,
                        specification = specification,
                        material = material,
                        unit = unit,
                        status = initial?.status ?: ProductStatus.DRAFT
                    )
                    val result = runCatching { onSave(draft) }
                    saving = false
                    result.onSuccess { onSaved() }.onFailure { error = it.message ?: "Could not save product." }
                }
            }) { Text(stringResource(R.string.product_save)) }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text(stringResource(R.string.company_cancel)) } }
    )
}

@Composable
private fun ProductEvidenceDialog(
    product: ProductEntity,
    repository: ProductCatalogRepository,
    sources: List<OrganizationSourceEntity>,
    onDismiss: () -> Unit,
    onSave: suspend (Set<String>) -> Int,
    onSaved: () -> Unit
) {
    val evidence by repository.observeEvidence(product.id).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var selected by remember(product.id) { mutableStateOf(emptySet<String>()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(evidence) { selected = evidence.map { it.sourceId }.toSet() }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(stringResource(R.string.product_evidence_dialog_title)) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(product.name, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.product_evidence_notice), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                if (sources.isEmpty()) {
                    Text(stringResource(R.string.product_evidence_empty_sources), style = MaterialTheme.typography.bodySmall)
                } else {
                    sources.forEach { source ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = source.id in selected,
                                onCheckedChange = { checked ->
                                    selected = if (checked) selected + source.id else selected - source.id
                                }
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
            Button(enabled = !saving && sources.isNotEmpty(), onClick = {
                scope.launch {
                    saving = true
                    val result = runCatching { onSave(selected) }
                    saving = false
                    result.onSuccess { onSaved() }.onFailure { error = it.message ?: "Could not update product evidence." }
                }
            }) { Text(stringResource(R.string.product_save)) }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text(stringResource(R.string.company_cancel)) } }
    )
}

private fun productStatusLabel(status: String): Int = when (status) {
    ProductStatus.DRAFT -> R.string.product_status_draft
    ProductStatus.ACTIVE -> R.string.product_status_active
    ProductStatus.DISCONTINUED -> R.string.product_status_discontinued
    ProductStatus.ARCHIVED -> R.string.product_status_archived
    else -> R.string.product_status_unknown
}
