package com.yusufculcu.trader.discovery

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yusufculcu.trader.data.DiscoveryRunRepository
import com.yusufculcu.trader.data.DiscoveryRunStatus
import com.yusufculcu.trader.data.SourceRunStatus
import com.yusufculcu.trader.data.TraderDatabase
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

    @Test fun sourceFailureIsRecordedAndReturnedAsPartialFailure() = runBlocking {
        val source = sourceStore.add("Failing", "https://example.org/fail.xml")
        val adapter = FakeAdapter(failure = IllegalStateException("provider unavailable"))
        val coordinator = DiscoveryExecutionCoordinator(sourceStore, DiscoveryAdapterRegistry(listOf(adapter)), runRepository, clock = { 200L })
        val summary = coordinator.execute("Research", onDocument = {})
        assertEquals(listOf(source.name), summary.failedSourceNames)
        assertEquals(DiscoveryRunStatus.FAILED, database.discoveryRunDao().getById(summary.runId)?.status)
        assertEquals("SOURCE_FAILURE", database.sourceRunDao().getForRun(summary.runId).single().errorCode)
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
