package com.yusufculcu.trader.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DuckDuckGoHtmlSearchTest {
    @Test fun buildsEncodedBoundedHttpsSearchUrl() {
        val url = DuckDuckGoHtmlSearch.buildSearchUrl("  carbide  milling tools  Poland ")
        assertTrue(url.toString().startsWith("https://html.duckduckgo.com/html/?q="))
        assertTrue(url.toString().contains("carbide"))
        assertFalse(url.toString().contains("  "))
        assertEquals(300, DuckDuckGoHtmlSearch.buildSearchUrl("x".repeat(500)).toString().substringAfter("q=").length)
    }

    @Test fun parsesTitlesSnippetsAndRedirectUrls() {
        val html = """
            <div class="result">
              <a class="result__a" href="https://duckduckgo.com/l/?uddg=https%3A%2F%2Fexample.com%2Ftools&amp;rut=123">
                Carbide <b>Milling</b> Tools &amp; More
              </a>
              <a class="result__snippet">Factory <b>catalog</b> with sizes &amp; grades.</a>
            </div>
            <div class="result">
              <a class="result__a" href="https://example.org/products">Steel Bars</a>
              <div class="result__snippet">Available in several grades.</div>
            </div>
        """.trimIndent()
        val results = DuckDuckGoHtmlSearch.parseHtml(html, 42L)
        assertEquals(2, results.size)
        assertEquals("Carbide Milling Tools & More", results[0].title)
        assertEquals("https://example.com/tools", results[0].url)
        assertEquals("Factory catalog with sizes & grades.", results[0].snippet)
        assertEquals("https://example.org/products", results[1].url)
        assertEquals(42L, results[1].retrievedAt)
    }

    @Test fun rejectsUnsafeAndSearchEngineUrls() {
        val html = """
            <a class="result__a" href="http://example.com/insecure">Insecure</a>
            <a class="result__a" href="https://user:pass@example.com/secret">Credentials</a>
            <a class="result__a" href="https://duckduckgo.com/?q=internal">Search page</a>
            <a class="result__a" href="javascript:alert(1)">Script</a>
        """.trimIndent()
        assertTrue(DuckDuckGoHtmlSearch.parseHtml(html, 1L).isEmpty())
    }

    @Test fun deduplicatesResultUrlsAndTruncatesFields() {
        val html = """
            <a class="result__a" href="https://example.com"> \${"T".repeat(320)} </a>
            <a class="result__a" href="https://example.com">Duplicate</a>
        """.trimIndent()
        val results = DuckDuckGoHtmlSearch.parseHtml(html, 7L)
        assertEquals(1, results.size)
        assertEquals(300, results.single().title.length)
    }
}
