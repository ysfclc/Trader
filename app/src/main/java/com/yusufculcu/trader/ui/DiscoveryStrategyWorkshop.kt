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
import com.yusufculcu.trader.data.DiscoveryStrategyCodec
import com.yusufculcu.trader.ai.TraderContextPacket
import com.yusufculcu.trader.ai.TraderContextFact
import com.yusufculcu.trader.ai.TraderFactStatus
import com.yusufculcu.trader.ai.TraderDiscoveryStrategyDraft
import com.yusufculcu.trader.ai.TraderDiscoveryStrategyDraftGenerator
import com.yusufculcu.trader.ai.TraderModelStore
import com.yusufculcu.trader.domain.ProofAsset
import com.yusufculcu.trader.domain.ProofAssetKind
import com.yusufculcu.trader.discovery.RssAtomFeedAdapter
import com.yusufculcu.trader.discovery.DuckDuckGoDiscoveryAdapter
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
internal fun DiscoveryStrategyWorkshop(app: TraderApplication, modelStore: TraderModelStore) {
    val problems by app.repository.observeProblems().collectAsState(initial = emptyList())
    val assetRows by app.discoveryStrategyRepository.observeProofAssets().collectAsState(initial = emptyList())
    // Room rows intentionally keep a compact indexed envelope; decode the versioned domain payload at this boundary.
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
    var problemText by remember { mutableStateOf("") }
    var queryText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val problem = problems.firstOrNull { it.id == problemId } ?: problems.firstOrNull()
    val asset = assets.firstOrNull { it.id == assetId } ?: assets.firstOrNull()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Discovery strategy workshop", style = MaterialTheme.typography.titleLarge)
            Text("Use a real problem and a user-owned proof asset. The local model drafts only; nothing is executed until you approve a saved strategy.", style = MaterialTheme.typography.bodySmall)
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
                                allowedAdapterIds = setOf(RssAtomFeedAdapter.ADAPTER_ID, DuckDuckGoDiscoveryAdapter.ADAPTER_ID)
                            )
                            draft = result.draft
                            if (result.draft != null) {
                                problemText = result.draft.problemSignatures.first().problem
                                queryText = result.draft.queries.first().query
                                message = "Draft generated. Review and edit the problem signature and first query."
                            } else message = result.issues.joinToString(" ")
                            busy = false
                        }
                    }) { Text(if (busy) "Working…" else "Generate strategy locally") }
                }
                draft?.let { current ->
                    Text("Review strategy", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(problemText, { problemText = it.take(4000) }, label = { Text("Problem signature") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    OutlinedTextField(queryText, { queryText = it.take(4000) }, label = { Text("First search query") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    Text("Other queries: " + (current.queries.size - 1) + " · Disqualifiers: " + current.disqualifiers.size, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(enabled = !busy && problemText.isNotBlank() && queryText.isNotBlank() && asset != null, onClick = {
                            val revised = current.copy(
                                problemSignatures = current.problemSignatures.mapIndexed { index, item -> if (index == 0) item.copy(problem = problemText.trim()) else item },
                                queries = current.queries.mapIndexed { index, item -> if (index == 0) item.copy(query = queryText.trim()) else item }
                            )
                            val strategy = TraderDiscoveryStrategyDraftGenerator(modelStore).materializeReviewedDraft(revised, asset!!)
                            busy = true
                            scope.launch {
                                val errors = app.discoveryStrategyRepository.saveStrategy(strategy)
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
