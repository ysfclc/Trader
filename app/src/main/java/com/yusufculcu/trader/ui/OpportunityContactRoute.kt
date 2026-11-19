package com.yusufculcu.trader.ui

import android.content.Intent
import android.net.Uri

object OpportunityContactRoute {
    private val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun createIntent(rawRoute: String): Intent? {
        val value = rawRoute.trim()
        if (value.isBlank() || value.length > 2_048) return null

        val uri = when {
            value.startsWith("https://", ignoreCase = true) || value.startsWith("http://", ignoreCase = true) ->
                runCatching { Uri.parse(value) }.getOrNull()
            value.startsWith("mailto:", ignoreCase = true) ->
                runCatching { Uri.parse(value) }.getOrNull()
            value.startsWith("tel:", ignoreCase = true) ->
                runCatching { Uri.parse(value) }.getOrNull()
            emailPattern.matches(value) -> Uri.fromParts("mailto", value, null)
            else -> null
        } ?: return null

        return when (uri.scheme?.lowercase()) {
            "http", "https" -> if (uri.host.isNullOrBlank()) null else Intent(Intent.ACTION_VIEW, uri)
            "mailto" -> Intent(Intent.ACTION_SENDTO, uri)
            "tel" -> Intent(Intent.ACTION_DIAL, uri)
            else -> null
        }
    }
}
