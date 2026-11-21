package com.yusufculcu.trader.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DiscoveryRunPersistenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java).allowMainThreadQueries().build()
    private val repo = DiscoveryRunRepository(db)
    @After fun close() = db.close()

    @Test fun recordsSourceSuccessAndFailureWithoutLosingRunHistory() = runBlocking {
        val run = repo.create(DiscoveryRunDraft(null, "Find public buyer requirements", 100))
        assertTrue(repo.start(run.id, 110))
        val good = repo.addSource(run.id, SourceRunDraft("source-a", "rss.atom", 120))
        val bad = repo.addSource(run.id, SourceRunDraft("source-b", "public-search", 120))
        assertTrue(repo.startSource(good.id, 130))
        assertTrue(repo.finishSource(good.id, 4, 140))
        assertTrue(repo.startSource(bad.id, 130))
        assertTrue(repo.failSource(bad.id, "HTTP_429", "Rate limited", 150))
        assertTrue(repo.complete(run.id, 160))
        assertEquals(DiscoveryRunStatus.PARTIAL, db.discoveryRunDao().getById(run.id)?.status)
        assertEquals(4, db.sourceRunDao().getById(good.id)?.resultCount)
        assertEquals("HTTP_429", db.sourceRunDao().getById(bad.id)?.errorCode)
        assertEquals(2, repo.observeSources(run.id).first().size)
    }

    @Test fun interruptedRunsBecomePartialAndActiveSourcesNeedReview() = runBlocking {
        val run = repo.create(DiscoveryRunDraft(null, "Find suppliers", 1))
        repo.start(run.id, 2)
        val source = repo.addSource(run.id, SourceRunDraft("source", "rss.atom", 3))
        repo.startSource(source.id, 4)
        assertEquals(1, repo.reconcileInterrupted(5))
        assertEquals(DiscoveryRunStatus.PARTIAL, db.discoveryRunDao().getById(run.id)?.status)
        assertEquals(SourceRunStatus.FAILED, db.sourceRunDao().getById(source.id)?.status)
        assertEquals(0, repo.reconcileInterrupted(6))
    }

    @Test fun cancellationIsIdempotentAndPreventsLateCompletion() = runBlocking {
        val run = repo.create(DiscoveryRunDraft(null, "Find opportunities", 1))
        repo.start(run.id, 2)
        assertTrue(repo.requestCancellation(run.id, 3))
        assertFalse(repo.complete(run.id, 4))
        assertFalse(repo.requestCancellation(run.id, 5))
        assertEquals(DiscoveryRunStatus.CANCELLED, db.discoveryRunDao().getById(run.id)?.status)
    }
}
