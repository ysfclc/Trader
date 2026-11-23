package com.yusufculcu.trader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yusufculcu.trader.R
import com.yusufculcu.trader.data.DiscoveryRunEntity
import com.yusufculcu.trader.data.DiscoveryRunRepository
import com.yusufculcu.trader.data.DiscoveryStrategyRepository
import com.yusufculcu.trader.data.DiscoveryStrategyCodec
import com.yusufculcu.trader.data.DiscoveryRunStatus
import com.yusufculcu.trader.data.SourceRunStatus
import com.yusufculcu.trader.data.TraderRepository
import com.yusufculcu.trader.discovery.DiscoveryDocument
import com.yusufculcu.trader.discovery.DiscoveryAdapterRegistry
import com.yusufculcu.trader.discovery.DiscoveryExecutionCoordinator
import com.yusufculcu.trader.discovery.DiscoverySourceConfig
import com.yusufculcu.trader.discovery.DiscoverySourceStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiscoveryScreen(
    repository: TraderRepository,
    sourceStore: DiscoverySourceStore,
    runRepository: DiscoveryRunRepository,
    strategyRepository: DiscoveryStrategyRepository,
    onOpenSource: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val adapterRegistry = remember { DiscoveryAdapterRegistry.builtIns() }
    val coordinator = remember(sourceStore, runRepository, adapterRegistry) {
        DiscoveryExecutionCoordinator(sourceStore, adapterRegistry, runRepository)
    }
    val scope = rememberCoroutineScope()
    val runs by runRepository.observeRuns().collectAsState(initial = emptyList())
    val strategyRows by strategyRepository.observeStrategies().collectAsState(initial = emptyList())
    val strategies = remember(strategyRows) { strategyRows.mapNotNull { row -> runCatching { DiscoveryStrategyCodec.decodeStrategy(row.payloadJson) }.getOrNull() } }
    var sources by remember { mutableStateOf(sourceStore.list()) }
    var results by remember { mutableStateOf(emptyList<DiscoveryDocument>()) }
    var objective by rememberSaveable { mutableStateOf("") }
    var feedName by rememberSaveable { mutableStateOf("") }
    var feedUrl by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var checkingSourceId by remember { mutableStateOf<String?>(null) }
    var activeJob by remember { mutableStateOf<Job?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var savedIds by remember { mutableStateOf(emptySet<String>()) }

    fun reloadSources() { sources = sourceStore.list() }

    fun startStrategyExecution(strategyId: String) {
        if (busy) return
        busy = true
        checkingSourceId = null
        error = null
        notice = null
        results = emptyList()
        savedIds = emptySet()
        val job = scope.launch {
            val collected = mutableListOf<DiscoveryDocument>()
            try {
                val strategy = strategyRepository.getStrategy(strategyId) ?: error("The saved strategy could not be loaded.")
                val proofAsset = strategyRepository.getProofAsset(strategy.proofAssetId)
                    ?: error("The strategy's proof asset is missing. Review the strategy before running it.")
                val summaries = coordinator.executeStrategy(strategy, proofAsset) { document ->
                    val normalized = document.url.trim().trimEnd('/').lowercase(Locale.ROOT)
                    if (collected.none { it.url.trim().trimEnd('/').lowercase(Locale.ROOT) == normalized }) {
                        collected += document
                        results = collected.toList()
                    }
                }
                val received = summaries.sumOf { it.documentsReceived }
                val failed = summaries.sumOf { it.failedSourceNames.size }
                notice = if (failed == 0) {
                    "Strategy completed: $received source-backed results across ${summaries.size} query runs."
                } else {
                    "Strategy partially completed: $received results; $failed source failures across ${summaries.size} query runs."
                }
            } catch (cancelled: CancellationException) {
                notice = context.getString(R.string.discover_run_cancelled)
            } catch (failure: Exception) {
                error = failure.message ?: context.getString(R.string.discover_source_failed)
            } finally {
                results = collected.toList()
                reloadSources()
                busy = false
                checkingSourceId = null
                activeJob = null
            }
        }
        activeJob = job
    }

    fun startExecution(
        objectiveText: String,
        sourceIds: Set<String>? = null,
        includePublicSearch: Boolean = true,
        checkingId: String? = null
    ) {
        if (busy) return
        val cleanObjective = objectiveText.trim()
        if (cleanObjective.length < 2) {
            error = context.getString(R.string.discover_query_too_short)
            return
        }
        busy = true
        checkingSourceId = checkingId
        error = null
        notice = null
        results = emptyList()
        savedIds = emptySet()
        val job = scope.launch {
            val collected = mutableListOf<DiscoveryDocument>()
            try {
                val summary = coordinator.execute(
                    objective = cleanObjective,
                    sourceIds = sourceIds,
                    includePublicSearch = includePublicSearch,
                    onDocument = { document ->
                    val normalized = document.url.trim().trimEnd('/').lowercase(Locale.ROOT)
                    if (collected.none { it.url.trim().trimEnd('/').lowercase(Locale.ROOT) == normalized }) {
                        collected += document
                        results = collected.toList()
                    }
                    }
                )
                notice = if (summary.failedSourceNames.isEmpty()) {
                    context.getString(R.string.discover_run_complete, summary.documentsReceived, summary.sourcesAttempted)
                } else {
                    context.getString(
                        R.string.discover_run_partial,
                        summary.documentsReceived,
                        summary.succeededSourceNames.size,
                        summary.failedSourceNames.size
                    )
                }
            } catch (cancelled: CancellationException) {
                notice = context.getString(R.string.discover_run_cancelled)
            } catch (failure: Exception) {
                error = failure.message ?: context.getString(R.string.discover_source_failed)
            } finally {
                results = collected.toList()
                reloadSources()
                busy = false
                checkingSourceId = null
                activeJob = null
            }
        }
        activeJob = job
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.discover_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.discover_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (strategies.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Approved discovery strategies", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Run the reviewed queries against their configured sources. Results remain unverified until you inspect their original sources.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    strategies.forEach { strategy ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Strategy " + strategy.id.take(8), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("${strategy.queries.size} saved queries · ${strategy.target.countries.joinToString().ifBlank { "No country filter" }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(onClick = { startStrategyExecution(strategy.id) }, enabled = !busy) {
                                Text("Run")
                            }
                        }
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.discover_web_search), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                OutlinedTextField(
                    value = objective,
                    onValueChange = { objective = it.take(DiscoveryExecutionCoordinator.MAX_OBJECTIVE_LENGTH); error = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.discover_search_hint)) },
                    minLines = 2,
                    maxLines = 4,
                    enabled = !busy
                )
                Text(stringResource(R.string.discover_run_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(
                    onClick = { startExecution(objective, includePublicSearch = true) },
                    enabled = !busy && objective.trim().length >= 2,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (busy && checkingSourceId == null) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.discover_run_now))
                    }
                }
                if (busy) {
                    OutlinedButton(onClick = { activeJob?.cancel() }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.discover_stop))
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.discover_add_feed), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = feedName,
                    onValueChange = { feedName = it.take(100); error = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.discover_source_name)) },
                    singleLine = true,
                    enabled = !busy
                )
                OutlinedTextField(
                    value = feedUrl,
                    onValueChange = { feedUrl = it.take(2_048); error = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.discover_feed_url)) },
                    singleLine = true,
                    enabled = !busy
                )
                OutlinedButton(
                    onClick = {
                        runCatching { sourceStore.add(feedName, feedUrl) }
                            .onSuccess { saved ->
                                reloadSources()
                                feedName = ""
                                feedUrl = ""
                                error = null
                                notice = context.getString(R.string.discover_source_added, saved.name)
                            }
                            .onFailure { error = it.message ?: context.getString(R.string.discover_source_failed) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy
                ) {
                    androidx.compose.material3.Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.padding(start = 4.dp))
                    Text(stringResource(R.string.discover_add_source))
                }
                OutlinedButton(
                    onClick = {
                        runCatching {
                            sourceStore.add(
                                context.getString(R.string.discover_hn_example_name),
                                "https://news.ycombinator.com/rss"
                            )
                        }.onSuccess { saved ->
                            reloadSources()
                            error = null
                            notice = context.getString(R.string.discover_source_added, saved.name)
                        }.onFailure { error = it.message ?: context.getString(R.string.discover_source_failed) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy
                ) { Text(stringResource(R.string.discover_add_hn_example)) }
                Text(stringResource(R.string.discover_feed_security), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.discover_sources), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            AssistChip(onClick = { reloadSources() }, label = { Text(stringResource(R.string.discover_reload)) }, leadingIcon = {
                androidx.compose.material3.Icon(Icons.Rounded.Refresh, contentDescription = null)
            })
        }

        if (sources.isEmpty()) {
            Text(stringResource(R.string.discover_no_sources), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            sources.forEach { source ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(source.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(source.feedUrl, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            FilterChip(
                                selected = source.enabled,
                                onClick = { sourceStore.setEnabled(source.id, !source.enabled); reloadSources() },
                                label = { Text(if (source.enabled) stringResource(R.string.discover_enabled) else stringResource(R.string.discover_paused)) },
                                enabled = !busy
                            )
                        }
                        val checkText = when {
                            source.lastError != null -> context.getString(R.string.discover_last_error, source.lastError)
                            source.lastCheckedAt != null -> context.getString(R.string.discover_last_checked, formatDiscoveryDate(source.lastCheckedAt))
                            else -> context.getString(R.string.discover_never_checked)
                        }
                        Text(checkText, style = MaterialTheme.typography.labelSmall, color = if (source.lastError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    startExecution(
                                        objectiveText = "Manual refresh: " + source.name,
                                        sourceIds = setOf(source.id),
                                        includePublicSearch = false,
                                        checkingId = source.id
                                    )
                                },
                                enabled = source.enabled && !busy
                            ) {
                                if (checkingSourceId == source.id) CircularProgressIndicator(
                                    modifier = Modifier.height(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                ) else Text(stringResource(R.string.discover_check_now))
                            }
                            OutlinedButton(onClick = { sourceStore.remove(source.id); reloadSources() }, enabled = !busy) {
                                Text(stringResource(R.string.discover_remove))
                            }
                        }
                    }
                }
            }
        }

        if (runs.isNotEmpty()) {
            Text(stringResource(R.string.discover_runs), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            runs.take(6).forEach { run ->
                DiscoveryRunCard(run = run, runRepository = runRepository, sources = sources)
            }
        } else {
            Text(stringResource(R.string.discover_no_runs), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }

        notice?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.discover_results), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.discover_result_count, results.size), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (results.isEmpty()) {
            Text(stringResource(R.string.discover_no_results), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            results.forEach { item ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(item.sourceName.uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (item.summary.isNotBlank()) Text(item.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(item.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Text(stringResource(R.string.discover_unverified_note), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onOpenSource(item.url) }) {
                                Text(stringResource(R.string.discover_open_source))
                            }
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        val saved = repository.captureDiscoveryItem(
                                            sourceName = item.sourceName,
                                            title = item.title,
                                            sourceUrl = item.url,
                                            summary = item.summary,
                                            publishedAt = item.publishedAt
                                        )
                                        if (saved != null) {
                                            savedIds = savedIds + item.id
                                            notice = context.getString(R.string.discover_saved)
                                        } else {
                                            error = context.getString(R.string.discover_save_failed)
                                        }
                                    }
                                },
                                enabled = item.id !in savedIds
                            ) {
                                Text(stringResource(if (item.id in savedIds) R.string.discover_saved_button else R.string.discover_save))
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DiscoveryRunCard(
    run: DiscoveryRunEntity,
    runRepository: DiscoveryRunRepository,
    sources: List<DiscoverySourceConfig>
) {
    val context = LocalContext.current
    val sourceRuns by remember(run.id) { runRepository.observeSources(run.id) }.collectAsState(initial = emptyList())
    val configuredSources = remember(sources) { sources.associateBy { it.id } }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(run.objective, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(runStatusLabel(context, run.status), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(formatDiscoveryDate(run.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            sourceRuns.forEach { sourceRun ->
                val sourceName = if (sourceRun.sourceId == com.yusufculcu.trader.discovery.DuckDuckGoDiscoveryAdapter.PUBLIC_SEARCH_SOURCE_ID) {
                    context.getString(R.string.discover_public_web)
                } else {
                    configuredSources[sourceRun.sourceId]?.name ?: sourceRun.sourceId
                }
                Text(
                    context.getString(
                        R.string.discover_run_source_result,
                        sourceName,
                        sourceRunStatusLabel(context, sourceRun.status),
                        sourceRun.resultCount
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (sourceRun.status == SourceRunStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                sourceRun.errorMessage?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun runStatusLabel(context: android.content.Context, status: String): String = context.getString(
    when (status) {
        DiscoveryRunStatus.CREATED -> R.string.discover_run_status_created
        DiscoveryRunStatus.RUNNING -> R.string.discover_run_status_running
        DiscoveryRunStatus.COMPLETED -> R.string.discover_run_status_completed
        DiscoveryRunStatus.PARTIAL -> R.string.discover_run_status_partial
        DiscoveryRunStatus.FAILED -> R.string.discover_run_status_failed
        DiscoveryRunStatus.CANCELLED -> R.string.discover_run_status_cancelled
        else -> R.string.discover_run_status_unknown
    }
)

private fun sourceRunStatusLabel(context: android.content.Context, status: String): String = context.getString(
    when (status) {
        SourceRunStatus.PENDING -> R.string.discover_source_run_pending
        SourceRunStatus.RUNNING -> R.string.discover_source_run_running
        SourceRunStatus.SUCCEEDED -> R.string.discover_source_run_succeeded
        SourceRunStatus.FAILED -> R.string.discover_source_run_failed
        SourceRunStatus.SKIPPED -> R.string.discover_source_run_skipped
        SourceRunStatus.CANCELLED -> R.string.discover_source_run_cancelled
        else -> R.string.discover_run_status_unknown
    }
)

private fun formatDiscoveryDate(value: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(value))
