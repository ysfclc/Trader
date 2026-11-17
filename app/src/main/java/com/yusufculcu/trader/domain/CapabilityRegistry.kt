package com.yusufculcu.trader.domain

enum class CapabilityAvailability {
    AVAILABLE,
    NOT_IMPLEMENTED,
    BLOCKED
}

enum class CapabilityEffect {
    LOCAL_READ,
    LOCAL_WRITE,
    LOCAL_COMPUTE,
    USER_TASK,
    NETWORK_READ,
    BROWSER_INTERACTION,
    APP_HANDOFF,
    APP_INTERACTION,
    EXTERNAL_SIDE_EFFECT
}

enum class RetryPolicy {
    SAFE,
    USER_CONFIRMED,
    NEVER
}

data class CapabilityDescriptor(
    val id: String,
    val title: String,
    val description: String,
    val availability: CapabilityAvailability,
    val effect: CapabilityEffect,
    val retryPolicy: RetryPolicy,
    val requiresApproval: Boolean = false,
    val requiredPermissions: Set<String> = emptySet(),
    val requiresVisibleSurface: Boolean = false,
    val supportsOffline: Boolean = false,
    val requiresPostcondition: Boolean = true
)

data class CapabilityAssessment(
    val id: String,
    val descriptor: CapabilityDescriptor?,
    val canPlan: Boolean,
    val canExecute: Boolean,
    val requiresApproval: Boolean,
    val retryPolicy: RetryPolicy,
    val missingPermissions: Set<String>,
    val reason: String?
)

object CapabilityRegistry {
    private val descriptors = listOf(
        CapabilityDescriptor(
            id = "manual.task",
            title = "Manual task",
            description = "A user-performed step tracked and verified by Trader",
            availability = CapabilityAvailability.AVAILABLE,
            effect = CapabilityEffect.USER_TASK,
            retryPolicy = RetryPolicy.USER_CONFIRMED,
            supportsOffline = true
        ),
        CapabilityDescriptor(
            id = "problem.capture.manual",
            title = "Capture a problem",
            description = "Create a local ProblemCase from direct user input",
            availability = CapabilityAvailability.AVAILABLE,
            effect = CapabilityEffect.LOCAL_WRITE,
            retryPolicy = RetryPolicy.SAFE,
            supportsOffline = true
        ),
        CapabilityDescriptor(
            id = "problem.capture.share",
            title = "Capture shared content",
            description = "Save URL or text received through Android share",
            availability = CapabilityAvailability.AVAILABLE,
            effect = CapabilityEffect.LOCAL_WRITE,
            retryPolicy = RetryPolicy.SAFE,
            supportsOffline = true
        ),
        CapabilityDescriptor(
            id = "plan.step.manage",
            title = "Manage plan steps",
            description = "Create and update local plan steps",
            availability = CapabilityAvailability.AVAILABLE,
            effect = CapabilityEffect.LOCAL_WRITE,
            retryPolicy = RetryPolicy.SAFE,
            supportsOffline = true
        ),
        CapabilityDescriptor(
            id = "activity.read",
            title = "Read activity history",
            description = "Inspect locally stored activity and workflow events",
            availability = CapabilityAvailability.AVAILABLE,
            effect = CapabilityEffect.LOCAL_READ,
            retryPolicy = RetryPolicy.SAFE,
            supportsOffline = true
        ),
        CapabilityDescriptor(
            id = "browser.visible.open",
            title = "Open visible browser session",
            description = "Navigate in a user-visible browser session with pause and takeover",
            availability = CapabilityAvailability.NOT_IMPLEMENTED,
            effect = CapabilityEffect.BROWSER_INTERACTION,
            retryPolicy = RetryPolicy.SAFE,
            requiresVisibleSurface = true
        ),
        CapabilityDescriptor(
            id = "web.public.search",
            title = "Search permitted public sources",
            description = "Discover public information with source attribution and rate limits",
            availability = CapabilityAvailability.NOT_IMPLEMENTED,
            effect = CapabilityEffect.NETWORK_READ,
            retryPolicy = RetryPolicy.SAFE,
            requiredPermissions = setOf("network_access")
        ),
        CapabilityDescriptor(
            id = "web.page.fetch",
            title = "Fetch a public page",
            description = "Read a permitted public page and preserve its source URL",
            availability = CapabilityAvailability.NOT_IMPLEMENTED,
            effect = CapabilityEffect.NETWORK_READ,
            retryPolicy = RetryPolicy.SAFE,
            requiredPermissions = setOf("network_access")
        ),
        CapabilityDescriptor(
            id = "android.app.handoff",
            title = "Hand off to an installed app",
            description = "Open a supported app or deep link for a user-visible action",
            availability = CapabilityAvailability.NOT_IMPLEMENTED,
            effect = CapabilityEffect.APP_HANDOFF,
            retryPolicy = RetryPolicy.USER_CONFIRMED,
            requiresVisibleSurface = true
        ),
        CapabilityDescriptor(
            id = "android.accessibility.action",
            title = "Bounded installed-app interaction",
            description = "Perform a narrowly scoped, visible interaction through a user-enabled accessibility service",
            availability = CapabilityAvailability.NOT_IMPLEMENTED,
            effect = CapabilityEffect.APP_INTERACTION,
            retryPolicy = RetryPolicy.NEVER,
            requiresApproval = true,
            requiredPermissions = setOf("accessibility_service_enabled"),
            requiresVisibleSurface = true
        ),
        CapabilityDescriptor(
            id = "ai.context.prepare",
            title = "Prepare bounded AI context",
            description = "Assemble a selected, inspectable context packet from problem data and evidence",
            availability = CapabilityAvailability.NOT_IMPLEMENTED,
            effect = CapabilityEffect.LOCAL_COMPUTE,
            retryPolicy = RetryPolicy.SAFE,
            supportsOffline = true
        ),
        CapabilityDescriptor(
            id = "ai.inference.local",
            title = "Run local model inference",
            description = "Use a compatible on-device model without requiring paid API credits",
            availability = CapabilityAvailability.NOT_IMPLEMENTED,
            effect = CapabilityEffect.LOCAL_COMPUTE,
            retryPolicy = RetryPolicy.USER_CONFIRMED,
            supportsOffline = true
        ),
        CapabilityDescriptor(
            id = "ai.inference.remote",
            title = "Run remote model inference",
            description = "Send an explicitly selected and consented context packet to a remote model",
            availability = CapabilityAvailability.NOT_IMPLEMENTED,
            effect = CapabilityEffect.NETWORK_READ,
            retryPolicy = RetryPolicy.USER_CONFIRMED,
            requiresApproval = true,
            requiredPermissions = setOf("remote_model_consent", "network_access")
        ),
        CapabilityDescriptor(
            id = "communication.draft",
            title = "Prepare a communication draft",
            description = "Create a local draft from verified facts for user review",
            availability = CapabilityAvailability.NOT_IMPLEMENTED,
            effect = CapabilityEffect.LOCAL_WRITE,
            retryPolicy = RetryPolicy.USER_CONFIRMED,
            supportsOffline = true
        ),
        CapabilityDescriptor(
            id = "communication.send",
            title = "Send an external communication",
            description = "Hand off or send an exact approved message to a specific recipient and channel",
            availability = CapabilityAvailability.NOT_IMPLEMENTED,
            effect = CapabilityEffect.EXTERNAL_SIDE_EFFECT,
            retryPolicy = RetryPolicy.NEVER,
            requiresApproval = true,
            requiredPermissions = setOf("explicit_action_approval"),
            requiresVisibleSurface = true
        ),
        CapabilityDescriptor(
            id = "browser.submit",
            title = "Submit an external form",
            description = "Submit a form only after exact payload approval and postcondition verification",
            availability = CapabilityAvailability.NOT_IMPLEMENTED,
            effect = CapabilityEffect.EXTERNAL_SIDE_EFFECT,
            retryPolicy = RetryPolicy.NEVER,
            requiresApproval = true,
            requiredPermissions = setOf("explicit_action_approval"),
            requiresVisibleSurface = true
        )
    )

    private val byId = descriptors.associateBy { it.id }

    init {
        check(byId.size == descriptors.size) { "Capability IDs must be unique" }
        check(descriptors.filter { it.effect == CapabilityEffect.EXTERNAL_SIDE_EFFECT }
            .all { it.requiresApproval && it.retryPolicy == RetryPolicy.NEVER }) {
            "External side-effect capabilities must require approval and prohibit blind retries"
        }
    }

    fun all(): List<CapabilityDescriptor> = descriptors.toList()

    fun find(id: String): CapabilityDescriptor? = byId[id]

    fun assess(id: String, grantedPermissions: Set<String> = emptySet()): CapabilityAssessment {
        val descriptor = byId[id] ?: return CapabilityAssessment(
            id = id,
            descriptor = null,
            canPlan = true,
            canExecute = false,
            requiresApproval = true,
            retryPolicy = RetryPolicy.NEVER,
            missingPermissions = emptySet(),
            reason = "Unregistered capability: planning is allowed, execution is blocked until it is reviewed and registered."
        )
        val missing = descriptor.requiredPermissions - grantedPermissions
        val available = descriptor.availability == CapabilityAvailability.AVAILABLE
        val canExecute = available && missing.isEmpty()
        val reason = when {
            descriptor.availability == CapabilityAvailability.BLOCKED -> "Capability is blocked by policy."
            descriptor.availability == CapabilityAvailability.NOT_IMPLEMENTED -> "Capability is not implemented."
            missing.isNotEmpty() -> "Missing permissions or consent: ${missing.sorted().joinToString()}"
            else -> null
        }
        return CapabilityAssessment(
            id = id,
            descriptor = descriptor,
            canPlan = true,
            canExecute = canExecute,
            requiresApproval = descriptor.requiresApproval || descriptor.effect == CapabilityEffect.EXTERNAL_SIDE_EFFECT,
            retryPolicy = descriptor.retryPolicy,
            missingPermissions = missing,
            reason = reason
        )
    }

    fun normalizedRetrySafe(id: String, userDeclaredSafe: Boolean): Boolean =
        when (assess(id).retryPolicy) {
            RetryPolicy.SAFE -> true
            RetryPolicy.USER_CONFIRMED -> userDeclaredSafe
            RetryPolicy.NEVER -> false
        }

    fun requiresApproval(id: String, userRequiresApproval: Boolean): Boolean =
        userRequiresApproval || assess(id).requiresApproval
}
