package com.yusufculcu.trader.ui

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** URL and identity-provider routing rules for Trader's embedded browser. */
internal object TraderBrowserRouting {
    fun target(raw: String): String {
        val input = raw.trim()
        if (input.isBlank()) return "https://www.google.com"
        if (input.startsWith("https://", ignoreCase = true) || input.startsWith("http://", ignoreCase = true)) {
            return input
        }
        // Do not interpret javascript:, file:, intent: or arbitrary app schemes as address-bar URLs.
        val hasScheme = Regex("^[A-Za-z][A-Za-z0-9+.-]*:").containsMatchIn(input)
        val looksLikeHost = input.contains('.') && !input.any(Char::isWhitespace)
        return if (!hasScheme && looksLikeHost) {
            "https://$input"
        } else {
            "https://www.google.com/search?q=" +
                URLEncoder.encode(input, StandardCharsets.UTF_8.name())
        }
    }

    fun isGoogleIdentityUrl(raw: String): Boolean {
        val host = runCatching { URI(raw).host }.getOrNull()
            ?.lowercase()
            ?.removePrefix("www.")
            ?: return false
        return host == "accounts.google.com" || host.endsWith(".accounts.google.com")
    }

    fun externalBrowserTarget(currentUrl: String, previousSiteUrl: String): String {
        return if (isGoogleIdentityUrl(currentUrl) && previousSiteUrl.startsWith("https://", true)) {
            previousSiteUrl
        } else {
            currentUrl
        }
    }

    fun isWebUrl(raw: String): Boolean =
        raw.startsWith("https://", ignoreCase = true) || raw.startsWith("http://", ignoreCase = true)
}
