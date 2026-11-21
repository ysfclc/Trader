package com.yusufculcu.trader.discovery

import android.util.Xml
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import org.xmlpull.v1.XmlPullParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DiscoveryDocument(
    val id: String,
    val sourceId: String,
    val sourceName: String,
    val nativeId: String,
    val title: String,
    val url: String,
    val summary: String,
    val publishedAt: Long?,
    val retrievedAt: Long
)

class FeedSourceException(message: String, cause: Throwable? = null) : IOException(message, cause)

/** Bounded public RSS 2.0 / Atom reader. It never follows redirects or reads non-HTTPS sources. */
class RssAtomFeedAdapter(
    private val clock: () -> Long = System::currentTimeMillis,
    private val connectionFactory: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection }
) {
    suspend fun fetch(source: DiscoverySourceConfig): List<DiscoveryDocument> = withContext(Dispatchers.IO) {
        val url = validateFeedUrl(source.feedUrl)
        validatePublicHost(url.host)
        val connection = connectionFactory(url)
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/atom+xml, application/rss+xml, application/xml, text/xml")
            connection.setRequestProperty("User-Agent", USER_AGENT)
            val status = connection.responseCode
            if (status in 300..399) throw FeedSourceException("Feed redirected. Update the source URL to its final HTTPS address.")
            if (status !in 200..299) throw FeedSourceException("Feed returned HTTP $status.")
            val bytes = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8_192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_RESPONSE_BYTES) throw FeedSourceException("Feed exceeded the 1.5 MB response limit.")
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            val xml = String(bytes, Charsets.UTF_8)
            parse(xml, source, clock())
        } catch (error: FeedSourceException) {
            throw error
        } catch (error: Exception) {
            throw FeedSourceException("Feed check failed: ${error.message ?: error.javaClass.simpleName}", error)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val ADAPTER_ID = "rss_atom"
        const val MAX_RESPONSE_BYTES = 1_500_000
        const val MAX_ITEMS = 30
        private const val CONNECT_TIMEOUT_MS = 8_000
        private const val READ_TIMEOUT_MS = 10_000
        private const val MAX_TEXT = 4_000
        private const val USER_AGENT = "TraderAndroid/0.1 (public-feed-reader)"

        fun validateFeedUrl(value: String): URL {
            val text = value.trim().takeIf { it.length in 1..2_048 }
                ?: throw FeedSourceException("Enter a feed URL under 2,048 characters.")
            val uri = runCatching { URI(text) }.getOrNull()
                ?: throw FeedSourceException("Feed URL is invalid.")
            val host = uri.host?.lowercase(Locale.ROOT)
            if (uri.scheme?.lowercase(Locale.ROOT) != "https" || host.isNullOrBlank() ||
                uri.rawUserInfo != null || uri.port !in setOf(-1, 443) ||
                host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local") ||
                host.matches(Regex("[0-9.]+")) || host.contains(":") || !host.contains(".") ||
                uri.toASCIIString().length > 2_048
            ) throw FeedSourceException("Only public HTTPS feed URLs are supported.")
            return runCatching { uri.toURL() }.getOrElse { throw FeedSourceException("Feed URL is invalid.", it) }
        }

        private fun validatePublicHost(host: String) {
            val addresses = try { InetAddress.getAllByName(host) } catch (error: Exception) {
                throw FeedSourceException("Feed host could not be resolved.", error)
            }
            if (addresses.isEmpty() || addresses.any {
                    it.isAnyLocalAddress || it.isLoopbackAddress || it.isLinkLocalAddress ||
                        it.isSiteLocalAddress || it.isMulticastAddress
                }
            ) throw FeedSourceException("Feed host resolves to a local or non-public network address.")
        }

        fun parse(xml: String, source: DiscoverySourceConfig, retrievedAt: Long): List<DiscoveryDocument> {
            if (xml.length > MAX_RESPONSE_BYTES) throw FeedSourceException("Feed document is too large.")
            val parser = Xml.newPullParser()
            try {
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
                parser.setInput(xml.reader())
                val output = ArrayList<DiscoveryDocument>()
                var item: FeedItem? = null
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT && output.size < MAX_ITEMS) {
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            val tag = parser.name.substringAfter(':').lowercase(Locale.ROOT)
                            if (tag == "item" || tag == "entry") {
                                item = FeedItem()
                            } else if (item != null) {
                                when (tag) {
                                    "link" -> {
                                        val href = parser.getAttributeValue(null, "href")
                                            ?: parser.getAttributeValue("http://www.w3.org/1999/xhtml", "href")
                                        if (!href.isNullOrBlank()) item.link = href.take(2_048)
                                        else readElementText(parser)?.let { item.link = it.take(2_048) }
                                    }
                                    "title" -> readElementText(parser)?.let { item.title = it.take(300) }
                                    "description", "summary", "content", "encoded" ->
                                        readElementText(parser)?.let { item.summary = cleanText(it).take(MAX_TEXT) }
                                    "pubdate", "published", "updated", "issued" ->
                                        readElementText(parser)?.let { item.published = it.take(100) }
                                    "guid", "id" -> readElementText(parser)?.let { item.nativeId = it.take(2_048) }
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            val tag = parser.name.substringAfter(':').lowercase(Locale.ROOT)
                            if (tag == "item" || tag == "entry") {
                                item?.let { candidate ->
                                    val safeUrl = resolveItemUrl(source.feedUrl, candidate.link)
                                    if (candidate.title.isNotBlank() && safeUrl != null) {
                                        val nativeId = candidate.nativeId.takeIf { it.isNotBlank() }
                                            ?: sha256(safeUrl)
                                        output.add(
                                            DiscoveryDocument(
                                                id = "${source.id}:$nativeId",
                                                sourceId = source.id,
                                                sourceName = source.name,
                                                nativeId = nativeId,
                                                title = cleanText(candidate.title).take(300).ifBlank { "Untitled feed item" },
                                                url = safeUrl,
                                                summary = cleanText(candidate.summary).take(MAX_TEXT),
                                                publishedAt = parseDate(candidate.published),
                                                retrievedAt = retrievedAt
                                            )
                                        )
                                    }
                                }
                                item = null
                            }
                        }
                    }
                    event = parser.next()
                }
                return output.distinctBy { it.url }.take(MAX_ITEMS)
            } catch (error: FeedSourceException) {
                throw error
            } catch (error: Exception) {
                throw FeedSourceException("Feed is not valid RSS 2.0 or Atom XML.", error)
            }
        }

        private fun readElementText(parser: XmlPullParser): String {
            val startDepth = parser.depth
            val text = StringBuilder()
            var event = parser.next()
            while (!(event == XmlPullParser.END_TAG && parser.depth == startDepth) &&
                event != XmlPullParser.END_DOCUMENT
            ) {
                if (event == XmlPullParser.TEXT || event == XmlPullParser.CDSECT || event == XmlPullParser.ENTITY_REF) {
                    if (text.length < MAX_TEXT * 2) text.append(parser.text)
                }
                event = parser.next()
            }
            return text.toString()
        }

        private fun resolveItemUrl(feedUrl: String, raw: String): String? {
            val value = raw.trim().takeIf { it.isNotBlank() } ?: return null
            val uri = runCatching { URI(feedUrl).resolve(URI(value)) }.getOrNull() ?: return null
            if (uri.scheme?.lowercase(Locale.ROOT) != "https" || uri.host.isNullOrBlank() ||
                uri.rawUserInfo != null || uri.host.equals("localhost", true)
            ) return null
            return uri.toASCIIString().takeIf { it.length <= 2_048 }
        }

        private fun cleanText(value: String): String =
            value.replace(Regex("(?is)<[^>]*>"), " ")
                .replace("&nbsp;", " ", true)
                .replace("&amp;", "&", true)
                .replace("&quot;", "\"", true)
                .replace("&#39;", "'", true)
                .replace("&lt;", "<", true)
                .replace("&gt;", ">", true)
                .replace(Regex("\\s+"), " ")
                .trim()

        private fun parseDate(value: String): Long? {
            if (value.isBlank()) return null
            val formats = listOf(
                "EEE, dd MMM yyyy HH:mm:ss Z",
                "EEE, d MMM yyyy HH:mm:ss Z",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ss'Z'"
            )
            for (pattern in formats) {
                val format = SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val parsed = format.parse(value, ParsePosition(0))
                if (parsed != null) return parsed.time
            }
            return null
        }

        private fun sha256(value: String): String =
            MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
                .joinToString("") { "%02x".format(it) }
    }

    private data class FeedItem(
        var title: String = "",
        var link: String = "",
        var summary: String = "",
        var published: String = "",
        var nativeId: String = ""
    )
}
