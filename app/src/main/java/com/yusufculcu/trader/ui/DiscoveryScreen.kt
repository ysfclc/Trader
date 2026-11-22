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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yusufculcu.trader.R
import com.yusufculcu.trader.data.TraderRepository
import com.yusufculcu.trader.data.DiscoveryRunRepository
import com.yusufculcu.trader.discovery.DiscoveryExecutionCoordinator
import com.yusufculcu.trader.discovery.DiscoveryDocument
import com.yusufculcu.trader.discovery.DiscoveryAdapterRegistry
import com.yusufculcu.trader.discovery.DiscoverySourceStore
import com.yusufculcu.trader.discovery.DuckDuckGoHtmlSearch
import com.yusufculcu.trader.discovery.PublicSearchRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun DiscoveryScreen(
    repository: TraderRepository,
    sourceStore: DiscoverySourceStore,
    runRepository: DiscoveryRunRepository,
    onOpenSource: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val adapterRegistry = remember { DiscoveryAdapterRegistry.builtIns() }
    val scope = rememberCoroutineScope()
    var sources by remember { mutableStateOf(sourceStore.list()) }
    var results by remember { mutableStateOf(emptyList<DiscoveryDocument>()) }
    var searchQuery by remember { mutableStateOf("") }
    var runObjective by remember { mutableStateOf("") }
    var feedName by remember { mutableStateOf("") }
    var feedUrl by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var checkingSourceId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var savedIds by remember { mutableStateOf(emptySet<String>()) }

    fun reloadSources() { sources = sourceStore.list() }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.discover_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.discover_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.discover_run_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = runObjective,
                    onValueChange = { runObjective = it.take(300); error = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.discover_run_objective_hint)) },
                    maxLines = 3
                )
                Button(
                    onClick = {
                        if (runObjective.trim().length < 2) {
                            error = context.getString(R.string.discover_query_too_short)
                        } else {
                            busy = true
                            error = null
                            notice = null
                            scope.launch {
                                val coordinator = DiscoveryExecutionCoordinator(sourceStore, adapterRegistry, runRepository)
                                runCatching {
                                    coordinator.execute(runObjective) { item -> results = (results + item).distinctBy { it.url } }
                                }.onSuccess { summary ->
                                    reloadSources()
                                    notice = context.getString(R.string.discover_run_complete, summary.succeededSourceNames.size, summary.failedSourceNames.size, summary.documentsReceived)
                                }.onFailure { error = it.message ?: context.getString(R.string.discover_source_failed) }
                                busy = false
                            }
                        }
                    },
                    enabled = !busy && sources.any { it.enabled } && runObjective.trim().length >= 2,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.discover_run_now)) }
                Text(stringResource(R.string.discover_run_limit_note), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.discover_web_search), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it.take(300); error = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.discover_search_hint)) },
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (searchQuery.trim().length < 2) {
                            error = context.getString(R.string.discover_query_too_short)
                        } else {
                            busy = true
                            error = null
                            notice = null
                            scope.launch {
                                runCatching { DuckDuckGoHtmlSearch().search(PublicSearchRequest(searchQuery)) }
                                    .onSuccess { response ->
                                        results = response.results.map { result ->
                                            DiscoveryDocument(
                                                id = "duckduckgo:${result.url}",
                                                sourceId = response.sourceId,
                                                sourceName = context.getString(R.string.discover_public_web),
                                                nativeId = result.url,
                                                title = result.title,
                                                url = result.url,
                                                summary = result.snippet,
                                                publishedAt = null,
                                                retrievedAt = response.retrievedAt
                                            )
                                        }
                                        notice = context.getString(R.string.discover_search_complete, results.size)
                                    }
                                    .onFailure { error = it.message ?: context.getString(R.string.discover_source_failed) }
                                busy = false
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (busy && checkingSourceId == null) CircularProgressIndicator(
                        modifier = Modifier.height(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    ) else Text(stringResource(R.string.discover_search_now))
                }
                Text(stringResource(R.string.discover_search_disclaimer), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    singleLine = true
                )
                OutlinedTextField(
                    value = feedUrl,
                    onValueChange = { feedUrl = it.take(2_048); error = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.discover_feed_url)) },
                    singleLine = true
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
                    modifier = Modifier.fillMaxWidth()
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.discover_add_hn_example))
                }
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
                                label = { Text(if (source.enabled) stringResource(R.string.discover_enabled) else stringResource(R.string.discover_paused)) }
                            )
                        }
                        val checkText = when {
                            source.lastError != null -> stringResource(R.string.discover_last_error, source.lastError)
                            source.lastCheckedAt != null -> stringResource(R.string.discover_last_checked, formatDiscoveryDate(source.lastCheckedAt))
                            else -> stringResource(R.string.discover_never_checked)
                        }
                        Text(checkText, style = MaterialTheme.typography.labelSmall, color = if (source.lastError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    checkingSourceId = source.id
                                    busy = true
                                    error = null
                                    notice = null
                                    scope.launch {
                                        val checkedAt = System.currentTimeMillis()
                                        runCatching { adapterRegistry.adapterFor(source).fetch(source) }
                                            .onSuccess { found ->
                                                results = found
                                                notice = context.getString(R.string.discover_feed_complete, found.size, source.name)
                                                sourceStore.markChecked(source.id, checkedAt)
                                            }
                                            .onFailure { failure ->
                                                val message = failure.message ?: context.getString(R.string.discover_source_failed)
                                                sourceStore.markChecked(source.id, checkedAt, message)
                                                error = message
                                            }
                                        reloadSources()
                                        checkingSourceId = null
                                        busy = false
                                    }
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
                        ) { Text(stringResource(if (item.id in savedIds) R.string.discover_saved_button else R.string.discover_save)) }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun formatDiscoveryDate(value: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(value))
