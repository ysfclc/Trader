package com.yusufculcu.trader.discovery

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DiscoverySourceStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var store: DiscoverySourceStore

    @Before
    fun setup() {
        context.getSharedPreferences("trader_discovery_sources", Context.MODE_PRIVATE).edit().clear().commit()
        store = DiscoverySourceStore(context)
    }

    @After
    fun cleanup() {
        context.getSharedPreferences("trader_discovery_sources", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun addsAndPersistsFeedConfiguration() {
        val added = store.add("Industry updates", "https://example.com/feed.xml")
        val reloaded = DiscoverySourceStore(context).list().single()

        assertEquals(added.id, reloaded.id)
        assertEquals("Industry updates", reloaded.name)
        assertEquals(RssAtomFeedAdapter.ADAPTER_ID, reloaded.adapterId)
        assertTrue(reloaded.enabled)
    }

    @Test
    fun duplicateUrlReturnsExistingConfiguration() {
        val first = store.add("Primary", "https://example.com/feed.xml")
        val duplicate = store.add("Renamed", "https://example.com/feed.xml")

        assertEquals(first.id, duplicate.id)
        assertEquals(1, store.list().size)
    }

    @Test
    fun supportsPauseHealthAndRemoval() {
        val source = store.add("Updates", "https://example.com/feed.xml")
        store.setEnabled(source.id, false)
        store.markChecked(source.id, 99L, "HTTP 503")
        val paused = store.list().single()
        assertFalse(paused.enabled)
        assertEquals(99L, paused.lastCheckedAt)
        assertEquals("HTTP 503", paused.lastError)
        store.remove(source.id)
        assertTrue(store.list().isEmpty())
    }

    @Test
    fun refusesBlankNamesAndNonPublicUrls() {
        assertThrows(IllegalArgumentException::class.java) { store.add(" ", "https://example.com/feed") }
        assertThrows(FeedSourceException::class.java) { store.add("Local", "http://localhost/feed") }
        assertTrue(store.list().isEmpty())
    }
}
