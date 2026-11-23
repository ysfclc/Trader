package com.yusufculcu.trader.discovery

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yusufculcu.trader.data.DiscoveryRunRepository
import com.yusufculcu.trader.data.TraderDatabase
import com.yusufculcu.trader.data.DiscoveryStrategyRepository
import com.yusufculcu.trader.domain.DiscoveryStrategy
import com.yusufculcu.trader.domain.DiscoveryQuery
import com.yusufculcu.trader.domain.ProblemSignature
import com.yusufculcu.trader.domain.ProofAsset
import com.yusufculcu.trader.domain.ProofAssetKind
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DiscoveryStrategyResultCapTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val database = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java).allowMainThreadQueries().build()
    private val runs = DiscoveryRunRepository(database)
    private val sources = DiscoverySourceStore(context, "strategy-cap-test-${System.nanoTime()}")

    @After fun close() = database.close()

    @Test fun strategyWidePerSourceCapCannotBeRaisedByQueryLimit() = runBlocking {
        val proof = ProofAsset(
            id = "cap-proof", kind = ProofAssetKind.SERVICE_CAPABILITY,
            name = "Precision machining", description = "CNC capacity",
            capabilities = listOf("CNC"), claims = emptyList(),
            createdAt = 1, updatedAt = 1
        )
        val repository = DiscoveryStrategyRepository(database)
        assertTrue(repository.saveProofAsset(proof).isEmpty())
        val strategy = DiscoveryStrategy(
            id = "cap-strategy", proofAssetId = proof.id,
            signatures = listOf(ProblemSignature("sig", "Capacity need")),
            queries = listOf(DiscoveryQuery(DuckDuckGoDiscoveryAdapter.ADAPTER_ID, "CNC Poland", resultLimit = 5)),
            maxResultsPerSource = 2, createdAt = 2, updatedAt = 2
        )
        assertTrue(repository.saveStrategy(strategy).isEmpty())

        val search = object : DiscoverySourceAdapter {
            override val adapterId = DuckDuckGoDiscoveryAdapter.ADAPTER_ID
            override suspend fun fetch(source: DiscoverySourceConfig) = emptyList<DiscoveryDocument>()
            override suspend fun search(source: DiscoverySourceConfig, query: String) =
                (1..5).map { index -> DiscoveryDocument(
                    id = "result-$index", sourceId = source.id, sourceName = source.name,
                    nativeId = "https://example.org/$index", title = "Result $index",
                    url = "https://example.org/$index", summary = "Snippet",
                    publishedAt = null, retrievedAt = 10
                ) }
        }
        val coordinator = DiscoveryExecutionCoordinator(sources, DiscoveryAdapterRegistry(listOf(search)), runs)
        val received = mutableListOf<DiscoveryDocument>()
        val summaries = coordinator.executeStrategy(strategy, proof) { received += it }

        assertEquals(1, summaries.size)
        assertEquals(2, summaries.single().documentsReceived)
        assertEquals(2, received.size)
        assertEquals(2, database.sourceRunDao().getForRun(summaries.single().runId).single().resultCount)
    }
}
