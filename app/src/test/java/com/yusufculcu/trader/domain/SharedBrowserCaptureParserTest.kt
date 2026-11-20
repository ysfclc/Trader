package com.yusufculcu.trader.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedBrowserCaptureParserTest {
    @Test
    fun parsesMemoBrowserCaptureAndRetainsSourceProvenance() {
        val payload = """
            MEMO_BROWSER_CAPTURE_V1
            Title: Acme supplier profile
            Source URL: https://example.com/suppliers/acme
            Summary: CNC machining and industrial components

            Captured page text:
            Acme supplies CNC turned and milled components to industrial manufacturers.
        """.trimIndent()

        val capture = SharedBrowserCaptureParser.parse(payload)

        assertEquals("Acme supplier profile", capture?.title)
        assertEquals("https://example.com/suppliers/acme", capture?.sourceUrl)
        assertEquals("CNC machining and industrial components", capture?.summary)
        assertTrue(capture?.content?.contains("CNC turned and milled") == true)
        assertTrue(SharedBrowserCaptureParser.toEvidenceContent(capture!!).startsWith("Page summary:"))
    }

    @Test
    fun rejectsUnknownVersionUnsafeSourceAndShortContent() {
        val validContent = "This page has enough visible public information to preserve safely."
        fun payload(url: String, content: String = validContent, header: String = SharedBrowserCaptureParser.HEADER) = """
            $header
            Title: Example
            Source URL: $url
            Summary: Summary

            Captured page text:
            $content
        """.trimIndent()

        assertNull(SharedBrowserCaptureParser.parse(payload("https://example.com", header = "MEMO_BROWSER_CAPTURE_V0")))
        assertNull(SharedBrowserCaptureParser.parse(payload("javascript:alert(1)")))
        assertNull(SharedBrowserCaptureParser.parse(payload("file:///etc/passwd")))
        assertNull(SharedBrowserCaptureParser.parse(payload("https://user:pass@example.com/")))
        assertNull(SharedBrowserCaptureParser.parse(payload("https://example.com", content = "short")))
    }

    @Test
    fun rejectsOversizedAndMissingContentPayloads() {
        val oversized = "x".repeat(SharedBrowserCaptureParser.HEADER.length + 30_000)
        val missingContent = """
            MEMO_BROWSER_CAPTURE_V1
            Title: Example
            Source URL: https://example.com
            Summary: Public page
        """.trimIndent()

        assertNull(SharedBrowserCaptureParser.parse(oversized))
        assertNull(SharedBrowserCaptureParser.parse(missingContent))
    }
}
