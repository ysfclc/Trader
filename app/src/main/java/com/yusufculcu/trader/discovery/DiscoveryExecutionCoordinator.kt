package com.yusufculcu.trader.discovery

import com.yusufculcu.trader.data.DiscoveryRunDraft
import com.yusufculcu.trader.data.DiscoveryRunRepository
import com.yusufculcu.trader.data.SourceRunDraft
import com.yusufculcu.trader.domain.DiscoveryStrategy
import com.yusufculcu.trader.domain.DiscoveryStrategyRules
import com.yusufculcu.trader.domain.ProofAsset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.URI

/** Executes an explicit, bounded discovery run against public search and selected local feeds. */
class DiscoveryExecutionCoordinator(
    private val sourceStore: DiscoverySourceStore,
    private val adapters: DiscoveryAdapterRegistry,
    private val runs: DiscoveryRunRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val sourceTimeoutMs: Long = DEFAULT_SOURCE_TIMEOUT_MS,
    private val maxSourcesPerRun: Int = MAX_SOURCES_PER_RUN,
    private val maxResultsPerSource: Int = MAX_RESULTS_PER_SOURCE
) {
    init {
        require(sourceTimeoutMs in 1..120_000)
        require(maxSourcesPerRun in 1..MAX_SOURCES_PER_RUN)
        require(maxResultsPerSource in 1..MAX_RESULTS_PER_SOURCE)
    }

    suspend fun execute(
        objective: String,
        sourceIds: Set<String>? = null,
        includePublicSearch: Boolean = false,
        onDocument: suspend (DiscoveryDocument) -> Unit = {},
        strategyId: String? = null,
        resultLimitOverride: Int? = null
    ): ExecutionSummary {
        val cleanObjective = objective.trim().replace(Regex("\\s+"), " ").take(MAX_OBJECTIVE_LENGTH)
        require(cleanObjective.length >= 2) { "Describe what this discovery run is looking for." }

        val configuredSources = sourceStore.list()
            .filter { it.enabled && (sourceIds == null || it.id in sourceIds) }
        val sources = buildList {
            if (includePublicSearch) add(DuckDuckGoDiscoveryAdapter.publicSearchSource())
            val availableFeedSlots = maxSourcesPerRun - size
            if (availableFeedSlots > 0) addAll(configuredSources.take(availableFeedSlots))
        }
        require(sources.isNotEmpty()) {
            "No enabled source is available for this discovery run. Select an enabled feed or include public search."
        }
        val run = runs.create(DiscoveryRunDraft(strategyId, cleanObjective, clock()))
        if (!runs.start(run.id, clock())) error("Could not start discovery run.")

        val successes = mutableListOf<String>()
        val failures = mutableListOf<String>()
        val seenUrls = HashSet<String>()
        var resultCount = 0
        try {
            sources.forEach { source ->
                val sourceRun = runs.addSource(run.id, SourceRunDraft(source.id, source.adapterId, clock()))
                if (!runs.startSource(sourceRun.id, clock())) {
                    failures += source.name
                    return@forEach
                }
                try {
                    val adapter = adapters.adapterFor(source)
                    val fetched = withTimeout(sourceTimeoutMs) {
                        if (source.adapterId == DuckDuckGoDiscoveryAdapter.ADAPTER_ID) {
                            adapter.search(source, cleanObjective)
                        } else {
                            adapter.fetch(source)
                        }
                    }
                    require(fetched.all { it.sourceId == source.id }) {
                        "Discovery adapter returned a finding for a different source."
                    }
                    val documents = fetched.asSequence()
                        .filter { it.url.isNotBlank() && seenUrls.add(canonicalUrl(it.url)) }
                        .take(minOf(maxResultsPerSource, resultLimitOverride ?: maxResultsPerSource))
                        .toList()

                    documents.forEach { document ->
                        runs.saveDocument(run.id, document)
                        onDocument(document)
                        resultCount++
                    }
                    runs.finishSource(sourceRun.id, documents.size, clock())
                    if (source.id != DuckDuckGoDiscoveryAdapter.PUBLIC_SEARCH_SOURCE_ID) {
                        runCatching { sourceStore.markChecked(source.id, clock()) }
                    }
                    successes += source.name
                } catch (timeout: TimeoutCancellationException) {
                    val message = "Source exceeded the " + sourceTimeoutMs + " ms execution limit."
                    runs.failSource(sourceRun.id, "TIMEOUT", message, clock())
                    if (source.id != DuckDuckGoDiscoveryAdapter.PUBLIC_SEARCH_SOURCE_ID) {
                        runCatching { sourceStore.markChecked(source.id, clock(), message) }
                    }
                    failures += source.name
                } catch (cancelled: CancellationException) {
                    withContext(NonCancellable) {
                        if (source.id != DuckDuckGoDiscoveryAdapter.PUBLIC_SEARCH_SOURCE_ID) {
                            runCatching { sourceStore.markChecked(source.id, clock(), "Run cancelled.") }
                        }
                        runs.requestCancellation(run.id, clock())
                    }
                    throw cancelled
                } catch (failure: Exception) {
                    val message = (failure.message ?: failure.javaClass.simpleName).take(2_000)
                    runs.failSource(sourceRun.id, "SOURCE_FAILURE", message, clock())
                    if (source.id != DuckDuckGoDiscoveryAdapter.PUBLIC_SEARCH_SOURCE_ID) {
                        runCatching { sourceStore.markChecked(source.id, clock(), message.take(300)) }
                    }
                    failures += source.name
                }
            }
            runs.complete(run.id, clock())
            return ExecutionSummary(run.id, sources.size, successes, failures, resultCount)
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) { runs.requestCancellation(run.id, clock()) }
            throw cancelled
        }
    }


    /**
     * Executes each approved query in a saved strategy as its own durable run, linked
     * back to the strategy. RSS/Atom query entries select configured feeds; public-search
     * entries execute their query text. No contact or external action is performed.
     */
    suspend fun executeStrategy(
        strategy: DiscoveryStrategy,
        proofAsset: ProofAsset,
        sourceIds: Set<String>? = null,
        onDocument: suspend (DiscoveryDocument) -> Unit = {}
    ): List<ExecutionSummary> {
        val validation = DiscoveryStrategyRules.validate(strategy, listOf(proofAsset))
        require(validation.isEmpty()) { validation.joinToString(" ") }
        val enabledFeeds = sourceStore.list().filter { it.enabled && (sourceIds == null || it.id in sourceIds) }
        val summaries = mutableListOf<ExecutionSummary>()
        strategy.queries.forEach { query ->
            when (query.sourceAdapterId) {
                DuckDuckGoDiscoveryAdapter.ADAPTER_ID -> {
                    require(query.resultLimit in 1..DuckDuckGoDiscoveryAdapter.MAX_RESULTS) {
                        "Public search supports at most ${DuckDuckGoDiscoveryAdapter.MAX_RESULTS} results per query."
                    }
                    summaries += execute(
                        objective = query.query,
                        includePublicSearch = true,
                        onDocument = onDocument,
                        strategyId = strategy.id,
                        resultLimitOverride = minOf(query.resultLimit, strategy.maxResultsPerSource)
                    )
                }
                RssAtomFeedAdapter.ADAPTER_ID -> {
                    val feedIds = enabledFeeds.filter { it.adapterId == RssAtomFeedAdapter.ADAPTER_ID }
                        .take(maxSourcesPerRun).map { it.id }.toSet()
                    require(feedIds.isNotEmpty()) { "This strategy has an RSS/Atom query, but no enabled feed source is configured." }
                    summaries += execute(
                        objective = query.query,
                        sourceIds = feedIds,
                        onDocument = onDocument,
                        strategyId = strategy.id,
                        resultLimitOverride = query.resultLimit
                    )
                }
                else -> error("Strategy references an unregistered discovery adapter: ${query.sourceAdapterId}")
            }
        }
        return summaries
    }

    private fun canonicalUrl(value: String): String = runCatching {
        val uri = URI(value.trim()).normalize()
        val scheme = uri.scheme?.lowercase()
        val host = uri.host?.lowercase()
        if (scheme != "https" || host.isNullOrBlank()) value.trim().trimEnd('/')
        else URI(
            scheme, uri.userInfo, host, uri.port,
            uri.path?.trimEnd('/').orEmpty().ifEmpty { "/" },
            uri.query, null
        ).toASCIIString()
    }.getOrDefault(value.trim().trimEnd('/'))

    data class ExecutionSummary(
        val runId: String,
        val sourcesAttempted: Int,
        val succeededSourceNames: List<String>,
        val failedSourceNames: List<String>,
        val documentsReceived: Int
    )

    companion object {
        const val DEFAULT_SOURCE_TIMEOUT_MS = 30_000L
        const val MAX_SOURCES_PER_RUN = 20
        const val MAX_RESULTS_PER_SOURCE = 50
        const val MAX_OBJECTIVE_LENGTH = 2_000
    }
}
