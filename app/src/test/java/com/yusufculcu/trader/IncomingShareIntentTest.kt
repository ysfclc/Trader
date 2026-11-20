package com.yusufculcu.trader

import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IncomingShareIntentTest {
    @Test
    fun sendIntentPreservesStyledCharSequenceAndTrimsOuterWhitespace() {
        val shared = SpannableString("  supplier specification  ")
        val intent = Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, shared)

        assertEquals("supplier specification", extractIncomingContent(intent))
    }

    @Test
    fun viewIntentCapturesItsUri() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.test/catalog"))

        assertEquals("https://example.test/catalog", extractIncomingContent(intent))
    }

    @Test
    fun blankSharedTextIsIgnored() {
        val intent = Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, " \n ")

        assertNull(extractIncomingContent(intent))
    }

    @Test
    fun unrelatedIntentActionsAreIgnored() {
        val intent = Intent(Intent.ACTION_MAIN).putExtra(Intent.EXTRA_TEXT, "must not be captured")

        assertNull(extractIncomingContent(intent))
    }

    @Test
    fun missingIntentOrPayloadIsIgnored() {
        assertNull(extractIncomingContent(null))
        assertNull(extractIncomingContent(Intent(Intent.ACTION_SEND)))
    }
    @Test
    fun sendIntentPreservesVersionedMemoBrowserCapturePayload() {
        val pagePayload = """
            MEMO_BROWSER_CAPTURE_V1
            Title: Acme supplier profile
            Source URL: https://example.test/suppliers/acme
            Summary: CNC manufacturing

            Captured page text:
            Acme supplies CNC turned and milled components to industrial manufacturers.
        """.trimIndent()
        val intent = Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, pagePayload)

        val extracted = extractIncomingContent(intent)
        val parsed = extracted?.let(com.yusufculcu.trader.domain.SharedBrowserCaptureParser::parse)

        assertEquals(pagePayload, extracted)
        assertEquals("Acme supplier profile", parsed?.title)
        assertEquals("https://example.test/suppliers/acme", parsed?.sourceUrl)
        assertTrue(parsed?.content?.contains("CNC turned and milled") == true)
    }

}
