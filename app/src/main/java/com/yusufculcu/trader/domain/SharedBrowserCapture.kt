package com.yusufculcu.trader.domain

/** Explicit, versioned payload shared by Memo's authenticated browser through Android Sharesheet. */
data class SharedBrowserCapture(
    val title: String,
    val sourceUrl: String,
    val summary: String,
    val content: String
)

object SharedBrowserCaptureParser {
    const val HEADER = "MEMO_BROWSER_CAPTURE_V1"
    private const val CONTENT_MARKER = "Captured page text:\n"
    private const val MAX_PAYLOAD_CHARS = 20_000
    private const val MAX_CONTENT_CHARS = 18_000

    fun parse(raw: String): SharedBrowserCapture? {
        val payload = raw.trim().replace("\r\n", "\n")
        if (payload.length > MAX_PAYLOAD_CHARS || !payload.startsWith("$HEADER\n")) return null

        val contentStart = payload.indexOf(CONTENT_MARKER)
        if (contentStart < 0) return null
        val header = payload.substring(HEADER.length + 1, contentStart)
        val body = payload.substring(contentStart + CONTENT_MARKER.length).trim().take(MAX_CONTENT_CHARS)
        if (body.length < 20) return null

        val fields = header.lineSequence().mapNotNull { line ->
            val separator = line.indexOf(':')
            if (separator <= 0) null else line.substring(0, separator).trim() to line.substring(separator + 1).trim()
        }.toMap()

        val title = fields["Title"].orEmpty().take(240)
        val sourceUrl = fields["Source URL"].orEmpty().take(2_048)
        val summary = fields["Summary"].orEmpty().take(600)
        val uri = runCatching { java.net.URI(sourceUrl) }.getOrNull()
        val scheme = uri?.scheme?.lowercase()
        if (title.isBlank() || scheme !in setOf("http", "https") || uri?.host.isNullOrBlank() || uri?.rawUserInfo != null) {
            return null
        }
        // Query strings and fragments may contain one-time OAuth codes or session tokens.
        val safeSourceUrl = uri.toASCIIString().substringBefore('?').substringBefore('#').take(2_048)
        if (safeSourceUrl.isBlank()) return null
        return SharedBrowserCapture(title, safeSourceUrl, summary, body)
    }

    fun toEvidenceContent(capture: SharedBrowserCapture): String = buildString {
        if (capture.summary.isNotBlank()) {
            append("Page summary: ").append(capture.summary).append("\n\n")
        }
        append(capture.content)
    }.take(MAX_CONTENT_CHARS)
}
