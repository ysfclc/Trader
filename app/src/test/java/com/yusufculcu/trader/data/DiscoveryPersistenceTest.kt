package com.yusufculcu.trader.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DiscoveryPersistenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val database = Room.inMemoryDatabaseBuilder(context, TraderDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val repository = TraderRepository(database)

    @After
    fun closeDatabase() { database.close() }

    @Test
    fun savesPublicDiscoveryAsUnreviewedResearchAndDeduplicatesByUrl() = runBlocking {
        val first = repository.captureDiscoveryItem(
            sourceName = "Operations forum",
            title = "Need help automating our workflow",
            sourceUrl = "https://example.com/posts/123?ref=feed#top",
            summary = "The team still moves work between spreadsheets.",
            publishedAt = 123L
        )
        val second = repository.captureDiscoveryItem(
            sourceName = "Another source",
            title = "Duplicate title",
            sourceUrl = "https://example.com/posts/123?ref=feed#different",
            summary = "A different excerpt.",
            publishedAt = null
        )

        assertNotNull(first)
        assertEquals(first, second)
        val problem = database.problemDao().getById(first!!)
        val evidence = database.evidenceDao().getAll().single()
        assertEquals("Research", problem?.category)
        assertEquals("discovery", problem?.inputKind)
        assertEquals("https://example.com/posts/123?ref=feed", evidence.sourceUri)
        assertEquals("PUBLIC_SOURCE", evidence.sourceKind)
        assertEquals("UNREVIEWED", evidence.verificationState)
        assertTrue(evidence.content.contains("Operations forum"))
        assertEquals(1, database.problemDao().getAll().size)
    }

    @Test
    fun rejectsUnsafeDiscoveryUrls() = runBlocking {
        assertNull(repository.captureDiscoveryItem("Feed", "Invalid", "http://example.com/post", "summary", null))
        assertNull(repository.captureDiscoveryItem("Feed", "Invalid", "https://user:pass@example.com/post", "summary", null))
        assertTrue(database.problemDao().getAll().isEmpty())
    }
}
