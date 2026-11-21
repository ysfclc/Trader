package com.yusufculcu.trader.discovery

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RssAtomFeedAdapterTest {
    private val source = DiscoverySourceConfig(
        id = "feed-1",
        adapterId = RssAtomFeedAdapter.ADAPTER_ID,
        name = "Example feed",
        feedUrl = "https://example.com/feed.xml"
    )

    @Test
    fun parsesRssItemsAndKeepsNativeIdsAndTimestamps() {
        val xml = """
            <?xml version="1.0"?>
            <rss version="2.0"><channel><title>Example</title>
              <item>
                <title>Factory needs a better scheduling system</title>
                <link>/posts/first?ref=rss</link>
                <guid>post-123</guid>
                <pubDate>Sat, 21 Nov 2026 08:00:00 +0000</pubDate>
                <description><![CDATA[We still coordinate shifts in spreadsheets.]]></description>
              </item>
            </channel></rss>
        """.trimIndent()

        val items = RssAtomFeedAdapter.parse(xml, source, 1234L)

        assertEquals(1, items.size)
        assertEquals("post-123", items.single().nativeId)
        assertEquals("https://example.com/posts/first?ref=rss", items.single().url)
        assertEquals("Factory needs a better scheduling system", items.single().title)
        assertEquals("We still coordinate shifts in spreadsheets.", items.single().summary)
        assertEquals(1234L, items.single().retrievedAt)
        assertTrue(items.single().publishedAt != null)
    }

    @Test
    fun parsesAtomLinkAttributesAndRemovesMarkupFromSummary() {
        val xml = """
            <feed xmlns="http://www.w3.org/2005/Atom">
              <entry>
                <id>tag:example.com,2026:one</id>
                <title>New production site</title>
                <link rel="alternate" href="https://example.com/news/site"/>
                <updated>2026-11-20T12:30:00Z</updated>
                <summary type="html">&lt;p&gt;A new &lt;b&gt;production&lt;/b&gt; site opens.&lt;/p&gt;</summary>
              </entry>
            </feed>
        """.trimIndent()

        val item = RssAtomFeedAdapter.parse(xml, source, 42L).single()

        assertEquals("tag:example.com,2026:one", item.nativeId)
        assertEquals("https://example.com/news/site", item.url)
        assertEquals("A new production site opens.", item.summary)
        assertTrue(item.publishedAt != null)
    }

    @Test
    fun dropsUnsafeItemLinksAndDeduplicatesCanonicalUrls() {
        val xml = """
            <rss><channel>
              <item><title>Safe</title><link>https://example.com/post</link><guid>one</guid></item>
              <item><title>Duplicate</title><link>https://example.com/post</link><guid>two</guid></item>
              <item><title>Unsafe</title><link>http://example.net/plain</link><guid>three</guid></item>
              <item><title>Credential URL</title><link>https://user:pass@example.com/secret</link></item>
            </channel></rss>
        """.trimIndent()

        val items = RssAtomFeedAdapter.parse(xml, source, 1L)

        assertEquals(1, items.size)
        assertEquals("Safe", items.single().title)
    }

    @Test
    fun rejectsInsecureLocalAndCredentialBearingFeedUrls() {
        assertThrows(FeedSourceException::class.java) { RssAtomFeedAdapter.validateFeedUrl("http://example.com/feed") }
        assertThrows(FeedSourceException::class.java) { RssAtomFeedAdapter.validateFeedUrl("https://localhost/feed") }
        assertThrows(FeedSourceException::class.java) { RssAtomFeedAdapter.validateFeedUrl("https://192.168.1.2/feed") }
        assertThrows(FeedSourceException::class.java) { RssAtomFeedAdapter.validateFeedUrl("https://user:pass@example.com/feed") }
        assertThrows(FeedSourceException::class.java) { RssAtomFeedAdapter.validateFeedUrl("https://example.com:8443/feed") }
        assertTrue(RssAtomFeedAdapter.validateFeedUrl("https://example.com/feed").toString().startsWith("https://"))
    }

    @Test
    fun rejectsXmlExternalEntityDeclarations() {
        val xml = """
            <!DOCTYPE rss [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
            <rss><channel><item><title>&xxe;</title><link>https://example.com/post</link></item></channel></rss>
        """.trimIndent()

        assertThrows(FeedSourceException::class.java) { RssAtomFeedAdapter.parse(xml, source, 1L) }
    }

    @Test
    fun capsItemCountAndTextLength() {
        val items = (1..40).joinToString("") { index ->
            "<item><title>${"T".repeat(350)}</title><link>https://example.com/$index</link></item>"
        }
        val parsed = RssAtomFeedAdapter.parse("<rss><channel>$items</channel></rss>", source, 7L)
        assertEquals(RssAtomFeedAdapter.MAX_ITEMS, parsed.size)
        assertEquals(300, parsed.first().title.length)
    }
}
