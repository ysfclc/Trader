package com.yusufculcu.trader.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityRegistryTest {
    @Test
    fun registryIdsAreUniqueAndExternalEffectsAreGated() {
        val descriptors = CapabilityRegistry.all()
        assertEquals(descriptors.size, descriptors.map { it.id }.toSet().size)
        assertTrue(descriptors.filter { it.effect == CapabilityEffect.EXTERNAL_SIDE_EFFECT }.all {
            it.requiresApproval && it.retryPolicy == RetryPolicy.NEVER
        })
    }

    @Test
    fun manualTaskIsAvailableButRetryRequiresUserDeclaration() {
        val assessment = CapabilityRegistry.assess("manual.task")
        assertTrue(assessment.canPlan)
        assertTrue(assessment.canExecute)
        assertFalse(assessment.requiresApproval)
        assertEquals(RetryPolicy.USER_CONFIRMED, assessment.retryPolicy)
        assertFalse(CapabilityRegistry.normalizedRetrySafe("manual.task", false))
        assertTrue(CapabilityRegistry.normalizedRetrySafe("manual.task", true))
    }

    @Test
    fun unimplementedBrowserAndCommunicationActionsFailClosed() {
        val browser = CapabilityRegistry.assess("browser.submit")
        val send = CapabilityRegistry.assess("communication.send")
        assertTrue(browser.canPlan)
        assertFalse(browser.canExecute)
        assertTrue(browser.requiresApproval)
        assertEquals(RetryPolicy.NEVER, browser.retryPolicy)
        assertFalse(send.canExecute)
        assertTrue(send.requiresApproval)
        assertFalse(CapabilityRegistry.normalizedRetrySafe("browser.submit", true))
    }

    @Test
    fun unknownCapabilitiesCanBePlannedButNeverExecuted() {
        val assessment = CapabilityRegistry.assess("custom.unreviewed.action")
        assertTrue(assessment.canPlan)
        assertFalse(assessment.canExecute)
        assertTrue(assessment.requiresApproval)
        assertEquals(RetryPolicy.NEVER, assessment.retryPolicy)
        assertNotNull(assessment.reason)
    }

    @Test
    fun permissionsAreExplicitAndVisible() {
        val withoutPermission = CapabilityRegistry.assess("android.accessibility.action")
        assertTrue(withoutPermission.missingPermissions.contains("accessibility_service_enabled"))
        assertFalse(withoutPermission.canExecute)
        assertTrue(withoutPermission.descriptor?.requiresVisibleSurface == true)
    }

    @Test
    fun approvalRequirementCannotBeRemovedByCaller() {
        assertTrue(CapabilityRegistry.requiresApproval("communication.send", false))
        assertTrue(CapabilityRegistry.requiresApproval("browser.submit", false))
        assertTrue(CapabilityRegistry.requiresApproval("manual.task", true))
        assertFalse(CapabilityRegistry.requiresApproval("manual.task", false))
    }
}
