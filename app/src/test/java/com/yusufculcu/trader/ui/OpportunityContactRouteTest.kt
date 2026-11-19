package com.yusufculcu.trader.ui

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OpportunityContactRouteTest {
    @Test
    fun webAndEmailRoutesOpenOnlyTheirExpectedHandlers() {
        val web = OpportunityContactRoute.createIntent("https://example.test/contact")
        val email = OpportunityContactRoute.createIntent("person@example.test")

        assertEquals(Intent.ACTION_VIEW, web?.action)
        assertEquals("https", web?.data?.scheme)
        assertEquals(Intent.ACTION_SENDTO, email?.action)
        assertEquals("mailto", email?.data?.scheme)
    }

    @Test
    fun phoneRouteOpensDialerWithoutPlacingCall() {
        val phone = OpportunityContactRoute.createIntent("tel:+48123456789")

        assertEquals(Intent.ACTION_DIAL, phone?.action)
        assertEquals("tel", phone?.data?.scheme)
    }

    @Test
    fun unsupportedSchemesAndAmbiguousTextAreRejected() {
        assertNull(OpportunityContactRoute.createIntent("intent://host/#Intent;scheme=https;end"))
        assertNull(OpportunityContactRoute.createIntent("javascript:alert(1)"))
        assertNull(OpportunityContactRoute.createIntent("call this person sometime"))
        assertNull(OpportunityContactRoute.createIntent(""))
    }
}
