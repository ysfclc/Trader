package com.yusufculcu.trader.discovery

import com.yusufculcu.trader.data.DiscoveryRunDraft
import com.yusufculcu.trader.data.DiscoveryRunRepository
import com.yusufculcu.trader.data.SourceRunDraft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/** Executes an explicit, bounded discovery run against selected local source instances. */
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
        onDocument: suspend (DiscoveryDocument) -> Unit = {}
    ): ExecutionSummary {
        require(objective.isNotBlank()) { "Describe what this discovery run is looking for." }
        val selected = sourceStore.list().filter { it.enabled && (sourceIds == null || it.id in sourceIds) }
            .take(maxSourcesPerRun)
        val run = runs.create(DiscoveryRunDraft(null, objective.trim().take(2_000), clock()))
        if (!runs.start(run.id, clock())) error("Could not start discovery run.")
        val successes = mutableListOf<String>()
        val failures = mutableListOf<String>()
        var resultCount = 0
        try {
            selected.forEach { source ->
                val sourceRun = runs.addSource(run.id, SourceRunDraft(source.id, source.adapterId, clock()))
                if (!runs.startSource(sourceRun.id, clock())) {
                    failures += source.name
                    return@forEach
                }
                try {
                    val documents = withTimeout(sourceTimeoutMs) { adapters.adapterFor(source).fetch(source) }
                        .distinctBy { it.url }
                        .take(maxResultsPerSource)
                    documents.forEach { onDocument(it); resultCount++ }
                    runs.finishSource(sourceRun.id, documents.size, clock())
                    sourceStore.markChecked(source.id, clock())
                    successes += source.name
                } catch (timeout: TimeoutCancellationException) {
                    val message = "Source exceeded the configured time limit."
                    runs.failSource(sourceRun.id, "TIMEOUT", message, clock())
                    sourceStore.markChecked(source.id, clock(), message)
                    failures += source.name
                } catch (cancelled: CancellationException) {
                    sourceStore.markChecked(source.id, clock(), "Run cancelled.")
                    runs.requestCancellation(run.id, clock())
                    throw cancelled
                } catch (failure: Exception) {
                    val code = "SOURCE_FAILURE"
                    val message = (failure.message ?: failure.javaClass.simpleName).take(2_000)
                    runs.failSource(sourceRun.id, code, message, clock())
                    sourceStore.markChecked(source.id, clock(), message.take(300))
                    failures += source.name
                }
            }
            runs.complete(run.id, clock())
            return ExecutionSummary(run.id, selected.size, successes, failures, resultCount)
        } catch (cancelled: CancellationException) {
            runs.requestCancellation(run.id, clock())
            throw cancelled
        }
    }

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
    }
}
