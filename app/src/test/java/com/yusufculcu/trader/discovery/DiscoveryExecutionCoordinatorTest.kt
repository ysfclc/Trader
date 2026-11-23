package com.yusufculcu.trader.discovery

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yusufculcu.trader.data.DiscoveryRunRepository
import com.yusufculcu.trader.data.DiscoveryRunStatus
import com.yusufculcu.trader.data.SourceRunStatus
import com.yusufculcu.trader.data.TraderDatabase
import com.yusufculcu.trader.data.DiscoveryStrategyRepository
import com.yusufculcu.trader.domain.DiscoveryStrategy
import com.yusufculcu.trader.domain.DiscoveryQuery
import com.yusufculcu.trader.domain.ProblemSignature
import com.yusufculcu.trader.domain.ProofAsset
import com.yusufculcu.trader.domain.ProofAssetKind
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DiscoveryExecutionCoordinatorTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val database = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java).allowMainThreadQueries().build()
    private val runRepository = DiscoveryRunRepository(database)
    private val sourceStore = DiscoverySourceStore(context, "coordinator-test-${System.nanoTime()}")
    @After fun close() = database.close()

    @Test fun recordsSourceSuccessAndAppliesResultLimit() = runBlocking {
        val source = sourceStore.add("Feed A", "https://example.org/feed.xml")
        val adapter = FakeAdapter(listOf(document(source.id, "https://example.org/1"), document(source.id, "https://example.org/2")))
        val coordinator = DiscoveryExecutionCoordinator(sourceStore, DiscoveryAdapterRegistry(listOf(adapter)), runRepository, clock = { 100L }, maxResultsPerSource = 1)
        val forwarded = mutableListOf<DiscoveryDocument>()
        val summary = coordinator.execute("Find buyers", onDocument = { forwarded += it })
        assertEquals(DiscoveryRunStatus.COMPLETED, database.discoveryRunDao().getById(summary.runId)?.status)
        val sourceRun = database.sourceRunDao().getForRun(summary.runId).single()
        assertEquals(SourceRunStatus.SUCCEEDED, sourceRun.status)
        assertEquals(1, sourceRun.resultCount)
        assertEquals(1, summary.documentsReceived)
        assertEquals(1, forwarded.size)
        assertEquals(source.name, sourceStore.list().single().name)
    }

    @Test fun sourceFailureIsRecordedAndReturnedAsFailure() = runBlocking {
        val source = sourceStore.add("Failing", "https://example.org/fail.xml")
        val adapter = FakeAdapter(failure = IllegalStateException("provider unavailable"))
        val coordinator = DiscoveryExecutionCoordinator(sourceStore, DiscoveryAdapterRegistry(listOf(adapter)), runRepository, clock = { 200L })
        val summary = coordinator.execute("Research", onDocument = {})
        assertEquals(listOf(source.name), summary.failedSourceNames)
        assertEquals(DiscoveryRunStatus.FAILED, database.discoveryRunDao().getById(summary.runId)?.status)
        assertEquals("SOURCE_FAILURE", database.sourceRunDao().getForRun(summary.runId).single().errorCode)
    }

    @Test fun timeoutIsRecordedAsSourceFailureAndDoesNotCancelTheRun() = runBlocking {
        val slow = sourceStore.add("Slow source", "https://example.org/slow.xml")
        val fast = sourceStore.add("Fast source", "https://example.org/fast.xml")
        val adapter = object : DiscoverySourceAdapter {
            override val adapterId = RssAtomFeedAdapter.ADAPTER_ID
            override suspend fun fetch(source: DiscoverySourceConfig): List<DiscoveryDocument> {
                if (source.id == slow.id) delay(100)
                return listOf(document(source.id, "https://example.org/" + source.name.replace(" ", "-")))
            }
        }
        val coordinator = DiscoveryExecutionCoordinator(
            sourceStore, DiscoveryAdapterRegistry(listOf(adapter)), runRepository,
            clock = { 300L }, sourceTimeoutMs = 10L
        )
        val summary = coordinator.execute("Find relevant suppliers")
        assertEquals(listOf(slow.name), summary.failedSourceNames)
        assertEquals(listOf(fast.name), summary.succeededSourceNames)
        assertEquals(DiscoveryRunStatus.PARTIAL, database.discoveryRunDao().getById(summary.runId)?.status)
        val rows = database.sourceRunDao().getForRun(summary.runId)
        assertEquals(SourceRunStatus.FAILED, rows.first { it.sourceId == slow.id }.status)
        assertEquals("TIMEOUT", rows.first { it.sourceId == slow.id }.errorCode)
        assertEquals(SourceRunStatus.SUCCEEDED, rows.first { it.sourceId == fast.id }.status)
    }

    @Test fun includesObjectiveSearchAndDeduplicatesAcrossSources() = runBlocking {
        val feed = sourceStore.add("Supplier updates", "https://example.org/feed.xml")
        val searchAdapter = object : DiscoverySourceAdapter {
            override val adapterId = DuckDuckGoDiscoveryAdapter.ADAPTER_ID
            override suspend fun fetch(source: DiscoverySourceConfig) = emptyList<DiscoveryDocument>()
            override suspend fun search(source: DiscoverySourceConfig, query: String): List<DiscoveryDocument> {
                assertEquals("Polish importers seeking carbide tools", query)
                return listOf(
                    document(source.id, "https://example.org/company"),
                    document(source.id, "https://example.org/tender")
                )
            }
        }
        val feedAdapter = object : DiscoverySourceAdapter {
            override val adapterId = RssAtomFeedAdapter.ADAPTER_ID
            override suspend fun fetch(source: DiscoverySourceConfig) = listOf(
                document(source.id, "https://example.org/company"),
                document(source.id, "https://example.org/feed-item")
            )
        }
        val coordinator = DiscoveryExecutionCoordinator(
            sourceStore, DiscoveryAdapterRegistry(listOf(searchAdapter, feedAdapter)), runRepository, clock = { 400L }
        )
        val forwarded = mutableListOf<DiscoveryDocument>()
        val summary = coordinator.execute(
            "Polish importers seeking carbide tools",
            includePublicSearch = true,
            onDocument = { forwarded += it }
        )
        assertEquals(2, summary.sourcesAttempted)
        assertEquals(3, summary.documentsReceived)
        assertEquals(3, forwarded.size)
        assertEquals(DiscoveryRunStatus.COMPLETED, database.discoveryRunDao().getById(summary.runId)?.status)
        assertEquals(2, database.sourceRunDao().getForRun(summary.runId).size)
    }


    @Test fun executesApprovedStrategyQueriesAndLinksEveryRun() = runBlocking {
        val proof = ProofAsset(
            id = "strategy-proof",
            kind = ProofAssetKind.SERVICE_CAPABILITY,
            name = "Precision manufacturing",
            description = "Machining and inspection",
            capabilities = listOf("CNC", "inspection"),
            claims = emptyList(),
            createdAt = 1,
            updatedAt = 1
        )
        val strategyRepository = DiscoveryStrategyRepository(database)
        assertTrue(strategyRepository.saveProofAsset(proof).isEmpty())
        val strategy = DiscoveryStrategy(
            id = "approved-strategy",
            proofAssetId = proof.id,
            signatures = listOf(ProblemSignature("signature", "Capacity or quality need", listOf("long lead time"))),
            queries = listOf(
                DiscoveryQuery(DuckDuckGoDiscoveryAdapter.ADAPTER_ID, "precision machining Poland", resultLimit = 2),
                DiscoveryQuery(DuckDuckGoDiscoveryAdapter.ADAPTER_ID, "CNC overflow capacity Poland", resultLimit = 1)
            ),
            createdAt = 2,
            updatedAt = 2
        )
        assertTrue(strategyRepository.saveStrategy(strategy).isEmpty())
        val queries = mutableListOf<String>()
        val searchAdapter = object : DiscoverySourceAdapter {
            override val adapterId = DuckDuckGoDiscoveryAdapter.ADAPTER_ID
            override suspend fun fetch(source: DiscoverySourceConfig) = emptyList<DiscoveryDocument>()
            override suspend fun search(source: DiscoverySourceConfig, query: String): List<DiscoveryDocument> {
                queries += query
                return listOf(
                    document(source.id, "https://example.org/a"),
                    document(source.id, "https://example.org/b"),
                    document(source.id, "https://example.org/c")
                )
            }
        }
        val coordinator = DiscoveryExecutionCoordinator(
            sourceStore, DiscoveryAdapterRegistry(listOf(searchAdapter)), runRepository, clock = { 50L }
        )
        val summaries = coordinator.executeStrategy(strategy, proof)
        assertEquals(listOf("precision machining Poland", "CNC overflow capacity Poland"), queries)
        assertEquals(2, summaries.size)
        assertEquals(listOf(2, 1), summaries.map { it.documentsReceived })
        val runRows = database.discoveryRunDao().getAll()
        assertEquals(2, runRows.size)
        assertTrue(runRows.all { it.strategyId == strategy.id })
        assertEquals(2, database.sourceRunDao().getForRun(runRows.first().id).single().resultCount)
        assertEquals(1, database.sourceRunDao().getForRun(runRows.last().id).single().resultCount)
    }

    private fun document(sourceId: String, url: String) = DiscoveryDocument(
        id = "id-$url", sourceId = sourceId, sourceName = "Test", nativeId = url,
        title = "Result", url = url, summary = "Evidence snippet", publishedAt = null, retrievedAt = 100
    )

    private class FakeAdapter(
        private val documents: List<DiscoveryDocument> = emptyList(),
        private val failure: Throwable? = null
    ) : DiscoverySourceAdapter {
        override val adapterId = RssAtomFeedAdapter.ADAPTER_ID
        override suspend fun fetch(source: DiscoverySourceConfig): List<DiscoveryDocument> {
            failure?.let { throw it }
            return documents
        }
    }
}
