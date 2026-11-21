package com.yusufculcu.trader.discovery

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.net.URI
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A public search result. Search snippets are source claims, not verified facts. */
data class PublicSearchResult(
    val title: String,
    val url: String,
    val snippet: String,
    val sourceId: String = DuckDuckGoHtmlSearch.SOURCE_ID,
    val retrievedAt: Long
)

data class PublicSearchRequest(
    val query: String,
    val limit: Int = 8
)

data class PublicSearchResponse(
    val query: String,
    val results: List<PublicSearchResult>,
    val sourceId: String,
    val retrievedAt: Long
)

class PublicSearchException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * Experimental public-web search adapter.
 *
 * It performs one bounded request to DuckDuckGo's public HTML search page. It does not
 * crawl result pages, authenticate, solve challenges, or bypass access restrictions.
 * Call only from an explicit user-triggered action and honor source policy/availability.
 */
class DuckDuckGoHtmlSearch(
    private val clock: () -> Long = System::currentTimeMillis,
    private val connectionFactory: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection }
) {
    suspend fun search(request: PublicSearchRequest): PublicSearchResponse = withContext(Dispatchers.IO) {
        val query = request.query.trim().replace(Regex("\\s+"), " ").take(MAX_QUERY_LENGTH)
        if (query.length < 2) throw PublicSearchException("Enter a search query of at least 2 characters.")
        if (request.limit !in 1..MAX_RESULTS) throw PublicSearchException("Result limit must be between 1 and $MAX_RESULTS.")
        val url = buildSearchUrl(query)
        val connection = connectionFactory(url)
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "text/html")
            connection.setRequestProperty("User-Agent", USER_AGENT)
            val status = connection.responseCode
            if (status !in 200..299) throw PublicSearchException("Search source returned HTTP $status.")
            val bytes = connection.inputStream.use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8_192)
                var total = 0
                while (true) {
                    val read = stream.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > MAX_RESPONSE_BYTES) throw PublicSearchException("Search response exceeded the size limit.")
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            }
            val retrievedAt = clock()
            PublicSearchResponse(
                query = query,
                results = parseHtml(String(bytes, Charsets.UTF_8), retrievedAt).take(request.limit),
                sourceId = SOURCE_ID,
                retrievedAt = retrievedAt
            )
        } catch (error: PublicSearchException) {
            throw error
        } catch (error: Exception) {
            throw PublicSearchException("Public search failed: \${error.message ?: error.javaClass.simpleName}", error)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val SOURCE_ID = "duckduckgo_html"
        const val MAX_QUERY_LENGTH = 300
        const val MAX_RESULTS = 10
        const val MAX_RESPONSE_BYTES = 1_500_000
        private const val CONNECT_TIMEOUT_MS = 8_000
        private const val READ_TIMEOUT_MS = 10_000
        private const val USER_AGENT = "TraderAndroid/0.1 (public-search; user-triggered)"

        fun buildSearchUrl(query: String): URL {
            val clean = query.trim().replace(Regex("\\s+"), " ").take(MAX_QUERY_LENGTH)
            return URL("https://html.duckduckgo.com/html/?q=\${URLEncoder.encode(clean, "UTF-8")}")
        }

        fun parseHtml(html: String, retrievedAt: Long): List<PublicSearchResult> {
            val links = RESULT_LINK.findAll(html).map { match ->
                val attributes = match.groupValues[1]
                val rawHref = ATTRIBUTE_HREF.find(attributes)?.groupValues?.get(1).orEmpty()
                val title = cleanHtml(match.groupValues[2])
                val resolved = resolveResultUrl(rawHref)
                title to resolved
            }.filter { (title, url) -> title.isNotBlank() && url != null }
            val snippets = RESULT_SNIPPET.findAll(html).map { cleanHtml(it.groupValues[1]) }.toList()
            val seen = HashSet<String>()
            return links.mapIndexedNotNull { index, (title, url) ->
                val safeUrl = url ?: return@mapIndexedNotNull null
                if (!seen.add(safeUrl)) return@mapIndexedNotNull null
                PublicSearchResult(
                    title = title.take(300),
                    url = safeUrl,
                    snippet = snippets.getOrNull(index).orEmpty().take(1_000),
                    retrievedAt = retrievedAt
                )
            }
        }

        private fun resolveResultUrl(raw: String): String? {
            val decoded = decodeEntities(raw.trim())
            val candidate = runCatching {
                val uri = URI(decoded)
                if (uri.host?.lowercase(Locale.ROOT)?.let { host -> host == "duckduckgo.com" || host.endsWith(".duckduckgo.com") } == true &&
                    uri.rawQuery != null && uri.rawQuery.contains("uddg=")
                ) {
                    val encoded = uri.rawQuery.split("&").firstOrNull { it.startsWith("uddg=") }?.substringAfter("uddg=")
                    java.net.URLDecoder.decode(encoded.orEmpty(), "UTF-8")
                } else decoded
            }.getOrNull() ?: return null
            val parsed = runCatching { URI(candidate) }.getOrNull() ?: return null
            if (parsed.scheme?.lowercase(Locale.ROOT) != "https" || parsed.host.isNullOrBlank() ||
                parsed.rawUserInfo != null || parsed.host.lowercase(Locale.ROOT).let { host -> host == "duckduckgo.com" || host.endsWith(".duckduckgo.com") }
            ) return null
            return parsed.toASCIIString().takeIf { it.length <= 2_048 }
        }

        private fun cleanHtml(value: String): String {
            val withoutTags = value.replace(Regex("(?is)<[^>]+>"), " ")
            return decodeEntities(withoutTags).replace(Regex("\\s+"), " ").trim()
        }

        private fun decodeEntities(value: String): String {
            var result = value
                .replace("&amp;", "&", ignoreCase = true)
                .replace("&quot;", "\\"", ignoreCase = true)
                .replace("&#39;", "'", ignoreCase = true)
                .replace("&apos;", "'", ignoreCase = true)
                .replace("&lt;", "<", ignoreCase = true)
                .replace("&gt;", ">", ignoreCase = true)
                .replace("&nbsp;", " ", ignoreCase = true)
            result = result.replace(Regex("&#(\\d+);")) { match ->
                match.groupValues[1].toIntOrNull()?.let { code -> String(Character.toChars(code)) } ?: match.value
            }
            result = result.replace(Regex("&#x([0-9a-fA-F]+);")) { match ->
                match.groupValues[1].toIntOrNull(16)?.let { code -> String(Character.toChars(code)) } ?: match.value
            }
            return result
        }

        private val RESULT_LINK = Regex(
            """(?is)<a\\b([^>]*class="[^"]*\\bresult__a[^"]*"[^>]*)>(.*?)</a>"""
        )
        private val ATTRIBUTE_HREF = Regex("""(?is)href=["']([^"']+)["']""")
        private val RESULT_SNIPPET = Regex(
            """(?is)<(?:a|div)[^>]*class=["'][^"']*\\bresult__snippet[^"']*["'][^>]*>(.*?)</(?:a|div)>"""
        )
    }
}
