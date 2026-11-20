package com.yusufculcu.trader.ui

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TraderBrowserCaptureTest {
    @Test
    fun decodesReviewedVisiblePageAndSelectedTextFromWebViewResult() {
        val page = JSONObject()
            .put("ok", true)
            .put("title", "Supplier profile")
            .put("url", "https://example.com/suppliers/acme")
            .put("content", "Company name: Acme Tools\nCapabilities: CNC turning and milling")
            .put("selectedText", "Capabilities: CNC turning and milling")
            .put("summary", "Industrial supplier profile")
            .toString()

        val decoded = TraderBrowserCapture.decodeJavascriptResult(
            JSONObject.quote(page),
            fallbackTitle = "Page",
            fallbackUrl = "https://example.com"
        )

        assertEquals("Supplier profile", decoded?.title)
        assertEquals("https://example.com/suppliers/acme", decoded?.url)
        assertTrue(decoded?.content?.contains("Acme Tools") == true)
        assertEquals(
            "Capabilities: CNC turning and milling",
            decoded?.let { TraderBrowserCapture.contentForSelection(it, selectedOnly = true) }
        )
    }

    @Test
    fun rejectsNonWebUrlsAndUnreadablePages() {
        val content = "This is enough visible content for a page capture."

        val fileUrl = JSONObject().put("ok", true).put("url", "file:///etc/passwd").put("content", content).toString()
        val credentialUrl = JSONObject().put("ok", true).put("url", "https://user:password@example.com/private").put("content", content).toString()
        val empty = JSONObject().put("ok", true).put("url", "https://example.com").put("content", "too short").toString()

        assertNull(TraderBrowserCapture.decodeJavascriptResult(JSONObject.quote(fileUrl), "Page", "https://example.com"))
        assertNull(TraderBrowserCapture.decodeJavascriptResult(JSONObject.quote(credentialUrl), "Page", "https://example.com"))
        assertNull(TraderBrowserCapture.decodeJavascriptResult(JSONObject.quote(empty), "Page", "https://example.com"))
        assertNull(TraderBrowserCapture.decodeJavascriptResult(null, "Page", "https://example.com"))
    }

    @Test
    fun boundsCapturedTextAndDoesNotReadBrowserCredentialsOrStorage() {
        val longText = "x".repeat(TraderBrowserCapture.MAX_CONTENT_CHARS + 100)
        val json = JSONObject().put("ok", true).put("url", "https://example.com").put("content", longText).toString()
        val decoded = TraderBrowserCapture.decodeJavascriptResult(JSONObject.quote(json), "Page", "https://example.com")

        assertEquals(TraderBrowserCapture.MAX_CONTENT_CHARS, decoded?.content?.length)
        assertFalse(TraderBrowserCapture.script.contains("document.cookie"))
        assertFalse(TraderBrowserCapture.script.contains("localStorage"))
        assertFalse(TraderBrowserCapture.script.contains("sessionStorage"))
        assertFalse(TraderBrowserCapture.script.contains("password"))
    }
}
