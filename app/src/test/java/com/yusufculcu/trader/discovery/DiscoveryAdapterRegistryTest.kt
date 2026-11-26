package com.yusufculcu.trader.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DiscoveryAdapterRegistryTest {
    private class StubAdapter(override val adapterId: String) : DiscoverySourceAdapter {
        override suspend fun fetch(source: DiscoverySourceConfig): List<DiscoveryDocument> = emptyList()
    }

    @Test
    fun resolvesConfiguredSourceToItsAdapter() {
        val registry = DiscoveryAdapterRegistry(listOf(StubAdapter("rss_atom")))
        val source = DiscoverySourceConfig("1", "rss_atom", "Feed", "https://example.com/feed")

        assertEquals("rss_atom", registry.adapterFor(source).adapterId)
    }

    @Test
    fun rejectsDuplicateAdapterIds() {
        assertThrows(IllegalArgumentException::class.java) {
            DiscoveryAdapterRegistry(listOf(StubAdapter("same"), StubAdapter("same")))
        }
    }

    @Test
    fun failsClosedForUnknownAdapterIds() {
        val registry = DiscoveryAdapterRegistry(listOf(StubAdapter("rss_atom")))
        val source = DiscoverySourceConfig("1", "unknown_provider", "Unknown", "https://example.com/feed")

        assertThrows(FeedSourceException::class.java) { registry.adapterFor(source) }
    }
}
