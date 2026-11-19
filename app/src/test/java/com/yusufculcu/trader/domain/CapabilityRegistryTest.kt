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
            it.requiresApproval &&
                it.retryPolicy == RetryPolicy.NEVER &&
                it.cost.requiresExplicitConsent &&
                it.postcondition.verification == PostconditionVerification.PROVIDER_CONFIRMATION
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

    @Test
    fun everyCapabilityDeclaresCompleteTypedContractAndOperationalBoundaries() {
        val descriptors = CapabilityRegistry.all()
        assertTrue(descriptors.isNotEmpty())
        descriptors.forEach { descriptor ->
            assertTrue(descriptor.id.isNotBlank())
            assertTrue(descriptor.inputs.fields.isNotEmpty())
            assertTrue(descriptor.outputs.fields.isNotEmpty())
            assertTrue(descriptor.inputs.fields.all { it.name.isNotBlank() && it.description.isNotBlank() })
            assertTrue(descriptor.outputs.fields.all { it.name.isNotBlank() && it.description.isNotBlank() })
            assertEquals(
                descriptor.inputs.fields.size,
                descriptor.inputs.fields.map { it.name }.toSet().size
            )
            assertEquals(
                descriptor.outputs.fields.size,
                descriptor.outputs.fields.map { it.name }.toSet().size
            )
            assertTrue(descriptor.cost.disclosure.isNotBlank())
            assertTrue(descriptor.limits.description.isNotBlank())
            assertTrue(descriptor.postcondition.required)
            assertTrue(descriptor.postcondition.description.isNotBlank())
            assertTrue(descriptor.sideEffects.isNotEmpty())
        }
        assertTrue(descriptors.filter { it.availability == CapabilityAvailability.AVAILABLE }
            .none { it.limits.mode == CapabilityLimitMode.ADAPTER_MUST_DECLARE })
    }

    @Test
    fun inputValidationAcceptsWellTypedPayloadAndOptionalFieldsMayBeOmitted() {
        val result = CapabilityRegistry.validateInputs(
            "manual.task",
            mapOf(
                "stepId" to "step-1",
                "title" to "Review supplier offer",
                "expectedOutcome" to "Record technical gaps"
            )
        )

        assertTrue(result.valid)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun inputValidationRejectsMissingRequiredWrongTypedAndUnexpectedFields() {
        val result = CapabilityRegistry.validateInputs(
            "manual.task",
            mapOf(
                "stepId" to "step-1",
                "title" to 42,
                "unexpected" to "not in schema"
            )
        )

        assertFalse(result.valid)
        assertTrue(result.issues.any { it.contains("expectedOutcome") })
        assertTrue(result.issues.any { it.contains("title") && it.contains("TEXT") })
        assertTrue(result.issues.any { it.contains("unexpected") })
    }

    @Test
    fun outputValidationChecksTypesAndRejectsUnregisteredCapabilities() {
        val valid = CapabilityRegistry.validateOutputs(
            "manual.task",
            mapOf("reportedOutcome" to "Completed", "userConfirmed" to true)
        )
        val invalid = CapabilityRegistry.validateOutputs(
            "manual.task",
            mapOf("reportedOutcome" to "Completed", "userConfirmed" to "yes")
        )
        val unknown = CapabilityRegistry.validateInputs("unknown.capability", emptyMap())

        assertTrue(valid.valid)
        assertFalse(invalid.valid)
        assertFalse(unknown.valid)
        assertTrue(unknown.issues.single().contains("Unregistered capability"))
    }

    @Test
    fun remoteInferenceIsExplicitDataDisclosureAndRequiresConsent() {
        val descriptor = CapabilityRegistry.find("ai.inference.remote")
        val assessment = CapabilityRegistry.assess("ai.inference.remote")

        assertNotNull(descriptor)
        assertEquals(CapabilityEffect.REMOTE_DATA_DISCLOSURE, descriptor?.effect)
        assertEquals(CapabilityCostKind.PROVIDER_BILLED, descriptor?.cost?.kind)
        assertTrue(descriptor?.cost?.requiresExplicitConsent == true)
        assertTrue(assessment.requiresApproval)
        assertFalse(assessment.canExecute)
    }

    @Test
    fun externalDataDisclosureCannotBeRegisteredWithoutApprovalAndConsent() {
        val descriptor = CapabilityRegistry.find("ai.inference.remote")
        assertTrue(descriptor?.requiresApproval == true)
        assertTrue(descriptor?.cost?.requiresExplicitConsent == true)
        assertEquals(RetryPolicy.USER_CONFIRMED, descriptor?.retryPolicy)
    }
}
