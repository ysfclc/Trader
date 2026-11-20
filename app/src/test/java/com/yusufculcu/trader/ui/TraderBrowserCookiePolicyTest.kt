package com.yusufculcu.trader.ui

import android.webkit.CookieManager
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TraderBrowserCookiePolicyTest {
    @Test
    fun enablesFirstPartyAndThirdPartyCookiesForWebViewLoginSessions() {
        val webView = WebView(ApplicationProvider.getApplicationContext<Context>())
        val cookieManager = CookieManager.getInstance()

        TraderBrowserCookiePolicy.configure(webView, cookieManager)

        assertTrue(cookieManager.acceptCookie())
        assertTrue(cookieManager.acceptThirdPartyCookies(webView))
        webView.destroy()
    }
}
