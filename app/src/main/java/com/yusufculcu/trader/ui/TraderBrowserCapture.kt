package com.yusufculcu.trader.ui

import org.json.JSONObject
import org.json.JSONTokener

data class TraderBrowserPageSnapshot(
    val title: String,
    val url: String,
    val content: String,
    val selectedText: String,
    val summary: String
)

/**
 * Reads rendered page text only. It deliberately does not inspect cookies, storage,
 * form values, password fields, tokens or browser profile data.
 */
internal object TraderBrowserCapture {
    const val MAX_CONTENT_CHARS = 20_000
    const val MAX_SELECTION_CHARS = 8_000

    val script: String = """
        (function() {
          const clean = value => (value || '')
            .replace(/\u00a0/g, ' ')
            .replace(/[\t ]+/g, ' ')
            .replace(/\n{3,}/g, '\n\n')
            .trim();
          const bodyText = clean(document.body ? document.body.innerText : '');
          const selection = clean(window.getSelection ? window.getSelection().toString() : '');
          const headings = Array.from(document.querySelectorAll('h1,h2,h3'))
            .filter(element => {
              const rect = element.getBoundingClientRect();
              const style = getComputedStyle(element);
              return rect.width > 0 && rect.height > 0 &&
                style.visibility !== 'hidden' && style.display !== 'none';
            })
            .slice(0, 40)
            .map(element => clean(element.innerText))
            .filter(Boolean);
          const description = clean(
            (document.querySelector('meta[name="description"]') || {}).content || ''
          );
          return JSON.stringify({
            ok: bodyText.length >= 30 || selection.length >= 20,
            title: clean(document.title || location.hostname).slice(0, 240),
            url: location.href,
            content: bodyText.slice(0, ${MAX_CONTENT_CHARS}),
            selectedText: selection.slice(0, ${MAX_SELECTION_CHARS}),
            summary: (description || headings.slice(0, 8).join(' · ') || bodyText.slice(0, 360)).slice(0, 600)
          });
        })();
    """.trimIndent()

    fun decodeJavascriptResult(
        raw: String?,
        fallbackTitle: String,
        fallbackUrl: String
    ): TraderBrowserPageSnapshot? {
        if (raw.isNullOrBlank() || raw == "null") return null
        return runCatching {
            val token = JSONTokener(raw)
            val decoded = token.nextValue() as? String ?: raw
            val json = JSONObject(decoded)
            if (!json.optBoolean("ok")) return null

            val candidateUrl = json.optString("url", fallbackUrl).trim().take(2_048).ifBlank { fallbackUrl }
            val parsed = runCatching { java.net.URI(candidateUrl) }.getOrNull()
            val scheme = parsed?.scheme?.lowercase()
            if (scheme !in setOf("http", "https") || parsed?.host.isNullOrBlank() || parsed?.rawUserInfo != null) return null
            // Do not retain query parameters or fragments that may contain temporary credentials.
            val safeUrl = parsed.toASCIIString().substringBefore('?').substringBefore('#').take(2_048)
            if (safeUrl.isBlank()) return null

            val content = json.optString("content").trim().take(MAX_CONTENT_CHARS)
            val selectedText = json.optString("selectedText").trim().take(MAX_SELECTION_CHARS)
            if (content.length < 30 && selectedText.length < 20) return null

            TraderBrowserPageSnapshot(
                title = json.optString("title", fallbackTitle).trim().take(240).ifBlank { fallbackTitle },
                url = safeUrl,
                content = content,
                selectedText = selectedText,
                summary = json.optString("summary").trim().take(600)
            )
        }.getOrNull()
    }

    fun contentForSelection(snapshot: TraderBrowserPageSnapshot, selectedOnly: Boolean): String =
        (if (selectedOnly) snapshot.selectedText else snapshot.content).trim()
}
