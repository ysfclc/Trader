package com.yusufculcu.trader.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yusufculcu.trader.domain.DiscoveryQuery
import com.yusufculcu.trader.domain.DiscoveryStrategy
import com.yusufculcu.trader.domain.DiscoveryTarget
import com.yusufculcu.trader.domain.ProblemSignature
import com.yusufculcu.trader.domain.ProofAsset
import com.yusufculcu.trader.domain.ProofAssetKind
import com.yusufculcu.trader.domain.ProofClaim
import com.yusufculcu.trader.domain.ProofClaimStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DiscoveryStrategyPersistenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val database = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val repository = DiscoveryStrategyRepository(database)

    @After
    fun closeDatabase() { database.close() }

    @Test
    fun persistsAndRoundTripsProofAssetAndStrategy() = runBlocking {
        val asset = ProofAsset(
            id = "asset-1",
            kind = ProofAssetKind.WORKING_PROJECT,
            name = "Trader",
            description = "A working local-first opportunity engine.",
            capabilities = listOf("source discovery", "evidence capture"),
            claims = listOf(ProofClaim("claim-1", "Supports bounded public discovery")),
            createdAt = 100,
            updatedAt = 110
        )
        assertTrue(repository.saveProofAsset(asset).isEmpty())
        val strategy = DiscoveryStrategy(
            id = "strategy-1",
            proofAssetId = asset.id,
            signatures = listOf(
                ProblemSignature(
                    id = "signature-1",
                    problem = "A company manually moves operational data between systems.",
                    observableSignals = listOf("spreadsheet handoffs"),
                    buyerRoles = listOf("operations manager"),
                    searchPhrases = listOf("workflow automation help"),
                    mustHave = listOf("publicly stated need")
                )
            ),
            target = DiscoveryTarget(countries = listOf("PL", "DE"), languages = listOf("en", "pl")),
            queries = listOf(DiscoveryQuery("public-search", "workflow automation help", "en", "PL", 10)),
            disqualifiers = listOf("unrelated consumer requests"),
            createdAt = 120,
            updatedAt = 125
        )
        assertTrue(repository.saveStrategy(strategy).isEmpty())
        assertEquals(asset, repository.getProofAsset(asset.id))
        assertEquals(strategy, repository.getStrategy(strategy.id))
        assertEquals(asset.id, database.discoveryStrategyDao().getById(strategy.id)?.proofAssetId)
    }

    @Test
    fun refusesStrategiesWithoutAnExistingProofAsset() = runBlocking {
        val strategy = DiscoveryStrategy(
            id = "orphan",
            proofAssetId = "missing",
            signatures = listOf(ProblemSignature("s1", "A stated problem", listOf("signal"))),
            queries = listOf(DiscoveryQuery("public-search", "stated problem")),
            createdAt = 1,
            updatedAt = 1
        )
        val errors = repository.saveStrategy(strategy)
        assertFalse(errors.isEmpty())
        assertEquals(0, database.discoveryStrategyDao().getAll().size)
    }

    @Test
    fun rejectsSupportedClaimsWithoutRealEvidenceRecords() = runBlocking {
        val asset = ProofAsset(
            id = "unsupported-proof",
            kind = ProofAssetKind.DOCUMENTED_RESULT,
            name = "Unsupported claim",
            description = "",
            capabilities = emptyList(),
            claims = listOf(ProofClaim("claim", "Claim", ProofClaimStatus.SUPPORTED, listOf("missing-evidence"), 2)),
            createdAt = 1,
            updatedAt = 2
        )
        assertFalse(repository.saveProofAsset(asset).isEmpty())
        assertTrue(database.proofAssetDao().getAll().isEmpty())
    }
}
