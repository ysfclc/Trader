package com.yusufculcu.trader.ui

import android.webkit.CookieManager
import android.webkit.WebView

/** Cookie settings required for a useful, persistent embedded browsing session. */
internal object TraderBrowserCookiePolicy {
    fun configure(
        webView: WebView,
        cookieManager: CookieManager = CookieManager.getInstance()
    ) {
        cookieManager.setAcceptCookie(true)
        // Targeting Lollipop+ defaults this off. Enable it for sites whose SSO relies on
        // cross-site identity widgets or redirects. The user can still clear app site data.
        cookieManager.setAcceptThirdPartyCookies(webView, true)
    }
}
