package com.yusufculcu.trader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yusufculcu.trader.TraderApplication
import com.yusufculcu.trader.ai.TraderContextPacket
import com.yusufculcu.trader.ai.TraderContextFact
import com.yusufculcu.trader.ai.TraderFactStatus
import com.yusufculcu.trader.ai.TraderDiscoveryStrategyDraft
import com.yusufculcu.trader.ai.TraderDiscoveryStrategyDraftGenerator
import com.yusufculcu.trader.ai.TraderModelStore
import com.yusufculcu.trader.data.DiscoveryStrategyCodec
import com.yusufculcu.trader.discovery.DuckDuckGoDiscoveryAdapter
import com.yusufculcu.trader.discovery.RssAtomFeedAdapter
import com.yusufculcu.trader.domain.DiscoveryQuery
import com.yusufculcu.trader.domain.DiscoveryStrategyRules
import com.yusufculcu.trader.domain.DiscoveryTarget
import com.yusufculcu.trader.domain.ProofAsset
import com.yusufculcu.trader.domain.ProofAssetKind
import com.yusufculcu.trader.domain.ProblemSignature
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val strategyAdapterIds = setOf(RssAtomFeedAdapter.ADAPTER_ID, DuckDuckGoDiscoveryAdapter.ADAPTER_ID)

@Composable
internal fun DiscoveryStrategyWorkshop(app: TraderApplication, modelStore: TraderModelStore) {
    val problems by app.repository.observeProblems().collectAsState(initial = emptyList())
    val assetRows by app.discoveryStrategyRepository.observeProofAssets().collectAsState(initial = emptyList())
    val assets = assetRows.mapNotNull { row -> runCatching { DiscoveryStrategyCodec.decodeAsset(row.payloadJson) }.getOrNull() }
    val scope = rememberCoroutineScope()
    var problemId by remember { mutableStateOf("") }
    var assetId by remember { mutableStateOf("") }
    var problemMenu by remember { mutableStateOf(false) }
    var assetMenu by remember { mutableStateOf(false) }
    var assetName by remember { mutableStateOf("") }
    var assetDescription by remember { mutableStateOf("") }
    var capabilities by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf<TraderDiscoveryStrategyDraft?>(null) }
    var message by remember { mutableStateOf("") }
    val problem = problems.firstOrNull { it.id == problemId } ?: problems.firstOrNull()
    val asset = assets.firstOrNull { it.id == assetId } ?: assets.firstOrNull()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Discovery strategy workshop", style = MaterialTheme.typography.titleLarge)
            Text("The local model drafts only. Review every field before saving; saving does not run discovery or contact anyone.", style = MaterialTheme.typography.bodySmall)
            if (problems.isEmpty()) {
                Text("Capture a problem in Inbox first.")
            } else {
                androidx.compose.foundation.layout.Box {
                    OutlinedButton(onClick = { problemMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Problem: " + (problem?.title ?: "Select"))
                    }
                    DropdownMenu(expanded = problemMenu, onDismissRequest = { problemMenu = false }) {
                        problems.forEach { item ->
                            DropdownMenuItem(text = { Text(item.title) }, onClick = {
                                problemId = item.id
                                draft = null
                                problemMenu = false
                            })
                        }
                    }
                }
                if (assets.isNotEmpty()) {
                    androidx.compose.foundation.layout.Box {
                        OutlinedButton(onClick = { assetMenu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Proof asset: " + (asset?.name ?: "Select"))
                        }
                        DropdownMenu(expanded = assetMenu, onDismissRequest = { assetMenu = false }) {
                            assets.forEach { item ->
                                DropdownMenuItem(text = { Text(item.name) }, onClick = {
                                    assetId = item.id
                                    draft = null
                                    assetMenu = false
                                })
                            }
                        }
                    }
                } else {
                    Text("Create a proof asset below. A problem description is not automatically treated as proof.")
                }
                Text("Create proof asset", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(assetName, { assetName = it.take(120) }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(assetDescription, { assetDescription = it.take(4000) }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(capabilities, { capabilities = it.take(2000) }, label = { Text("Capabilities, comma separated") }, modifier = Modifier.fillMaxWidth())
                Button(enabled = !busy && assetName.isNotBlank() && assetDescription.isNotBlank() && capabilities.isNotBlank(), onClick = {
                    busy = true
                    val now = System.currentTimeMillis()
                    val created = ProofAsset(
                        id = UUID.randomUUID().toString(),
                        kind = ProofAssetKind.SERVICE_CAPABILITY,
                        name = assetName.trim(),
                        description = assetDescription.trim(),
                        capabilities = capabilities.split(",").map { it.trim() }.filter { it.isNotBlank() }.distinct(),
                        claims = emptyList(),
                        createdAt = now,
                        updatedAt = now
                    )
                    scope.launch {
                        val errors = app.discoveryStrategyRepository.saveProofAsset(created)
                        message = if (errors.isEmpty()) "Proof asset saved. Its claims remain unverified." else errors.joinToString(" ")
                        if (errors.isEmpty()) {
                            assetId = created.id
                            assetName = ""
                            assetDescription = ""
                            capabilities = ""
                        }
                        busy = false
                    }
                }) { Text("Save proof asset") }

                if (asset != null) {
                    Button(enabled = !busy && modelStore.activeModelId() != null, onClick = {
                        val selectedProblem = problem ?: return@Button
                        val selectedAsset = asset
                        busy = true
                        draft = null
                        message = "Generating locally…"
                        scope.launch {
                            val steps = app.repository.observeSteps(selectedProblem.id).first()
                            val evidence = app.repository.observeEvidence(selectedProblem.id).first()
                            val packet = TraderContextPacket(
                                task = selectedProblem.title + "\n" + selectedProblem.description +
                                    "\nDesired outcome: " + selectedProblem.desiredOutcome +
                                    "\nConstraints: " + selectedProblem.constraintsText,
                                facts = evidence.map { row ->
                                    TraderContextFact(
                                        label = row.title,
                                        value = row.content.take(3000),
                                        status = if (row.verificationState.equals("VERIFIED", true)) TraderFactStatus.USER_CONFIRMED else TraderFactStatus.SOURCE_UNVERIFIED,
                                        sourceUri = row.sourceUri,
                                        capturedAt = row.capturedAt
                                    )
                                },
                                sections = mapOf("Plan" to steps.sortedBy { it.position }.joinToString("\n") { "- [" + it.status + "] " + it.title + ": " + it.expectedResult }),
                                unknowns = listOf("Buyer roles, target organizations and demand signals require independent verification.")
                            )
                            val result = TraderDiscoveryStrategyDraftGenerator(modelStore).generate(
                                task = "Find organizations with needs matching this proof asset. Consider industrial tooling and machining between Türkiye and Poland/EU only where supported by the case evidence.",
                                context = packet,
                                proofAsset = selectedAsset,
                                allowedAdapterIds = strategyAdapterIds
                            )
                            draft = result.draft
                            message = if (result.draft != null) "Draft generated. Review all sections, including every list item, before approval." else result.issues.joinToString(" ")
                            busy = false
                        }
                    }) { Text(if (busy) "Working…" else "Generate strategy locally") }
                }
                draft?.let { current ->
                    Text("Review strategy", style = MaterialTheme.typography.titleMedium)
                    Text("Every generated field is editable. List fields use one item per line.", style = MaterialTheme.typography.bodySmall)
                    current.problemSignatures.forEachIndexed { index, signature ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Problem signature " + (index + 1), style = MaterialTheme.typography.titleSmall)
                                OutlinedTextField(signature.problem, { value ->
                                    draft = current.copy(problemSignatures = current.problemSignatures.mapIndexed { i, item -> if (i == index) item.copy(problem = value.take(4000)) else item })
                                }, label = { Text("Problem") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                                StringListEditor("Observable signals", signature.observableSignals) { values ->
                                    draft = current.copy(problemSignatures = current.problemSignatures.mapIndexed { i, item -> if (i == index) item.copy(observableSignals = values) else item })
                                }
                                StringListEditor("Buyer roles", signature.buyerRoles) { values ->
                                    draft = current.copy(problemSignatures = current.problemSignatures.mapIndexed { i, item -> if (i == index) item.copy(buyerRoles = values) else item })
                                }
                                StringListEditor("Search phrases", signature.searchPhrases) { values ->
                                    draft = current.copy(problemSignatures = current.problemSignatures.mapIndexed { i, item -> if (i == index) item.copy(searchPhrases = values) else item })
                                }
                                StringListEditor("Must-have conditions", signature.mustHave) { values ->
                                    draft = current.copy(problemSignatures = current.problemSignatures.mapIndexed { i, item -> if (i == index) item.copy(mustHave = values) else item })
                                }
                                OutlinedButton(enabled = current.problemSignatures.size > 1, onClick = {
                                    draft = current.copy(problemSignatures = current.problemSignatures.filterIndexed { i, _ -> i != index })
                                }) { Text("Remove signature") }
                            }
                        }
                    }
                    Button(enabled = current.problemSignatures.size < 50, onClick = {
                        draft = current.copy(problemSignatures = current.problemSignatures + ProblemSignature(
                            id = "reviewed-signature-" + UUID.randomUUID().toString(),
                            problem = "",
                            observableSignals = emptyList()
                        ))
                    }) { Text("Add problem signature") }

                    Text("Target constraints", style = MaterialTheme.typography.titleSmall)
                    StringListEditor("Countries", current.target.countries) { draft = current.copy(target = current.target.copy(countries = it)) }
                    StringListEditor("Regions", current.target.regions) { draft = current.copy(target = current.target.copy(regions = it)) }
                    StringListEditor("Languages", current.target.languages) { draft = current.copy(target = current.target.copy(languages = it)) }
                    StringListEditor("Organization types", current.target.organizationTypes) { draft = current.copy(target = current.target.copy(organizationTypes = it)) }
                    OutlinedTextField(current.target.companySize.orEmpty(), { draft = current.copy(target = current.target.copy(companySize = it.take(120).ifBlank { null })) }, label = { Text("Company size (optional)") }, modifier = Modifier.fillMaxWidth())
                    StringListEditor("Excluded domains", current.target.excludedDomains) { draft = current.copy(target = current.target.copy(excludedDomains = it)) }

                    Text("Source queries", style = MaterialTheme.typography.titleSmall)
                    current.queries.forEachIndexed { index, query ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Query " + (index + 1), style = MaterialTheme.typography.titleSmall)
                                OutlinedTextField(query.sourceAdapterId, { value ->
                                    draft = current.copy(queries = current.queries.mapIndexed { i, item -> if (i == index) item.copy(sourceAdapterId = value.take(120)) else item })
                                }, label = { Text("Source adapter ID") }, modifier = Modifier.fillMaxWidth())
                                Text("Registered adapters: " + strategyAdapterIds.sorted().joinToString(", "), style = MaterialTheme.typography.bodySmall)
                                OutlinedTextField(query.query, { value ->
                                    draft = current.copy(queries = current.queries.mapIndexed { i, item -> if (i == index) item.copy(query = value.take(4000)) else item })
                                }, label = { Text("Search query") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                                OutlinedTextField(query.language.orEmpty(), { value ->
                                    draft = current.copy(queries = current.queries.mapIndexed { i, item -> if (i == index) item.copy(language = value.take(120).ifBlank { null }) else item })
                                }, label = { Text("Language (optional)") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(query.region.orEmpty(), { value ->
                                    draft = current.copy(queries = current.queries.mapIndexed { i, item -> if (i == index) item.copy(region = value.take(120).ifBlank { null }) else item })
                                }, label = { Text("Region (optional)") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(query.resultLimit.toString(), { value ->
                                    val limit = value.toIntOrNull() ?: query.resultLimit
                                    draft = current.copy(queries = current.queries.mapIndexed { i, item -> if (i == index) item.copy(resultLimit = limit) else item })
                                }, label = { Text("Result limit (1–50)") }, modifier = Modifier.fillMaxWidth())
                                OutlinedButton(enabled = current.queries.size > 1, onClick = {
                                    draft = current.copy(queries = current.queries.filterIndexed { i, _ -> i != index })
                                }) { Text("Remove query") }
                            }
                        }
                    }
                    Button(enabled = current.queries.size < 50, onClick = {
                        draft = current.copy(queries = current.queries + DiscoveryQuery(
                            sourceAdapterId = DuckDuckGoDiscoveryAdapter.ADAPTER_ID,
                            query = "",
                            resultLimit = 10
                        ))
                    }) { Text("Add source query") }

                    StringListEditor("Disqualifiers", current.disqualifiers) { draft = current.copy(disqualifiers = it) }
                    OutlinedTextField(current.maxResultsPerSource.toString(), { value ->
                        val limit = value.toIntOrNull() ?: current.maxResultsPerSource
                        draft = current.copy(maxResultsPerSource = limit)
                    }, label = { Text("Maximum results per source (1–50)") }, modifier = Modifier.fillMaxWidth())

                    val edited = draft ?: current
                    val strategy = edited.toStrategy("strategy-review-validation", asset?.id ?: "", 0L)
                    val validation = DiscoveryStrategyRules.validate(strategy, listOfNotNull(asset)).toMutableList()
                    edited.queries.forEachIndexed { index, query ->
                        if (query.sourceAdapterId !in strategyAdapterIds) validation += "Query " + (index + 1) + " uses an unregistered source adapter."
                    }
                    Text("Review validation", style = MaterialTheme.typography.titleSmall)
                    if (validation.isEmpty()) {
                        Text("All strategy fields pass local validation.", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    } else {
                        validation.distinct().forEach { Text("• " + it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(enabled = !busy && validation.isEmpty() && asset != null, onClick = {
                            val latest = draft ?: current
                            val now = System.currentTimeMillis()
                            val strategyToSave = TraderDiscoveryStrategyDraftGenerator(modelStore)
                                .materializeReviewedDraft(latest, asset!!).copy(updatedAt = now)
                            busy = true
                            scope.launch {
                                val errors = app.discoveryStrategyRepository.saveStrategy(strategyToSave)
                                message = if (errors.isEmpty()) "Approved strategy saved. Discovery has not been run." else errors.joinToString(" ")
                                if (errors.isEmpty()) draft = null
                                busy = false
                            }
                        }) { Text("Approve and save") }
                        OutlinedButton(onClick = { draft = null; message = "Draft discarded." }) { Text("Discard") }
                    }
                }
            }
            if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall)
            if (modelStore.activeModelId() == null) Text("Select an installed, verified model in Model Hub first.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StringListEditor(label: String, values: List<String>, onChange: (List<String>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = values.joinToString("\n"),
            onValueChange = { raw -> onChange(if (raw.isEmpty()) emptyList() else raw.split("\n").map { it.take(4000) }) },
            label = { Text(label + " (one per line)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onChange(values + "") }) { Text("Add item") }
            OutlinedButton(enabled = values.isNotEmpty(), onClick = { onChange(values.dropLast(1)) }) { Text("Remove last item") }
        }
    }
}
