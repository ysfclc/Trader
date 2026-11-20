package com.yusufculcu.trader.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TraderBrowserRoutingTest {
    @Test
    fun addressBarRoutesHostsAndSearchesButNotArbitrarySchemes() {
        assertEquals("https://example.com", TraderBrowserRouting.target("example.com"))
        assertEquals("https://www.google.com", TraderBrowserRouting.target(""))
        assertEquals(
            "https://www.google.com/search?q=hello+world",
            TraderBrowserRouting.target("hello world")
        )
        assertTrue(TraderBrowserRouting.target("javascript:alert(1)").startsWith("https://www.google.com/search?q="))
    }

    @Test
    fun detectsGoogleIdentityEndpointsWithoutMatchingOtherGoogleSites() {
        assertTrue(TraderBrowserRouting.isGoogleIdentityUrl("https://accounts.google.com/o/oauth2/v2/auth"))
        assertTrue(TraderBrowserRouting.isGoogleIdentityUrl("https://login.accounts.google.com/"))
        assertFalse(TraderBrowserRouting.isGoogleIdentityUrl("https://www.google.com/search"))
        assertFalse(TraderBrowserRouting.isGoogleIdentityUrl("https://example.com/accounts.google.com"))
    }

    @Test
    fun externalAuthFallbackRestartsTheRelyingPartySite() {
        val relyingParty = "https://www.linkedin.com/login"
        assertEquals(
            relyingParty,
            TraderBrowserRouting.externalBrowserTarget(
                "https://accounts.google.com/o/oauth2/v2/auth",
                relyingParty
            )
        )
        assertEquals(
            "https://example.com/login",
            TraderBrowserRouting.externalBrowserTarget("https://example.com/login", relyingParty)
        )
    }
}
