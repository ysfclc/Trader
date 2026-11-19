package com.yusufculcu.trader.domain

enum class CapabilityAvailability { AVAILABLE, NOT_IMPLEMENTED, BLOCKED }

enum class CapabilityEffect {
    LOCAL_READ, LOCAL_WRITE, LOCAL_COMPUTE, USER_TASK, NETWORK_READ,
    BROWSER_INTERACTION, APP_HANDOFF, APP_INTERACTION, EXTERNAL_SIDE_EFFECT
}

enum class RetryPolicy { SAFE, USER_CONFIRMED, NEVER }

enum class CapabilityDataType {
    IDENTIFIER, STRING, TEXT, URI, BOOLEAN, INTEGER, DECIMAL,
    STRING_LIST, OBJECT, OBJECT_LIST
}

data class CapabilityField(
    val name: String,
    val type: CapabilityDataType,
    val description: String,
    val required: Boolean = true,
    val sensitive: Boolean = false
)

data class CapabilityIoContract(
    val fields: List<CapabilityField>,
    val constraints: List<String> = emptyList(),
    val additionalFieldsAllowed: Boolean = false
)

enum class CapabilityCostKind {
    NONE, USER_TIME, DEVICE_RESOURCES, NETWORK_DATA, PROVIDER_BILLED,
    UNKNOWN_UNTIL_ADAPTER_SELECTED
}

data class CapabilityCostContract(
    val kind: CapabilityCostKind,
    val disclosure: String,
    val requiresExplicitConsent: Boolean
)

enum class CapabilityLimitMode { USER_CONTROLLED, PLATFORM_BOUNDED, ADAPTER_MUST_DECLARE }

data class CapabilityLimits(
    val mode: CapabilityLimitMode,
    val maxInputBytes: Long? = null,
    val maxOutputBytes: Long? = null,
    val timeoutMillis: Long? = null,
    val requestsPerMinute: Int? = null,
    val maxConcurrentRuns: Int? = null,
    val description: String
)

enum class PostconditionVerification {
    LOCAL_READ_BACK, STRUCTURAL_VALIDATION, USER_CONFIRMATION,
    SOURCE_READ_BACK, PROVIDER_CONFIRMATION
}

data class CapabilityPostcondition(
    val required: Boolean,
    val description: String,
    val verification: PostconditionVerification,
    val evidenceRequired: Boolean
)

data class CapabilityDescriptor(
    val id: String,
    val title: String,
    val description: String,
    val availability: CapabilityAvailability,
    val effect: CapabilityEffect,
    val retryPolicy: RetryPolicy,
    val inputs: CapabilityIoContract,
    val outputs: CapabilityIoContract,
    val cost: CapabilityCostContract,
    val limits: CapabilityLimits,
    val postcondition: CapabilityPostcondition,
    val sideEffects: List<String>,
    val requiresApproval: Boolean = false,
    val requiredPermissions: Set<String> = emptySet(),
    val requiresVisibleSurface: Boolean = false,
    val supportsOffline: Boolean = false
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

private fun capabilityField(
    name: String,
    type: CapabilityDataType,
    description: String,
    required: Boolean = true,
    sensitive: Boolean = false
) = CapabilityField(name, type, description, required, sensitive)

private fun capabilityIo(
    vararg fields: CapabilityField,
    constraints: List<String> = emptyList()
) = CapabilityIoContract(fields.toList(), constraints)

private fun capabilityCost(
    kind: CapabilityCostKind,
    disclosure: String,
    requiresConsent: Boolean = false
) = CapabilityCostContract(kind, disclosure, requiresConsent)

private fun capabilityLimits(mode: CapabilityLimitMode, description: String) =
    CapabilityLimits(mode = mode, description = description)

private fun capabilityPostcondition(
    description: String,
    verification: PostconditionVerification,
    evidenceRequired: Boolean = false
) = CapabilityPostcondition(true, description, verification, evidenceRequired)

object CapabilityRegistry {
    private val descriptors = listOf(
        CapabilityDescriptor(
            id = "manual.task",
            title = "Manual task",
            description = "A user-performed step tracked and verified by Trader",
            availability = CapabilityAvailability.AVAILABLE,
            effect = CapabilityEffect.USER_TASK,
            retryPolicy = RetryPolicy.USER_CONFIRMED,
            inputs = capabilityIo(
                capabilityField("stepId", CapabilityDataType.IDENTIFIER, "Persisted workflow step to perform"),
                capabilityField("title", CapabilityDataType.TEXT, "Human-readable task"),
                capabilityField("instructions", CapabilityDataType.TEXT, "Instructions and relevant context", required = false),
                capabilityField("expectedOutcome", CapabilityDataType.TEXT, "Observable result expected from the user")
            ),
            outputs = capabilityIo(
                capabilityField("reportedOutcome", CapabilityDataType.TEXT, "User-reported result"),
                capabilityField("evidenceNote", CapabilityDataType.TEXT, "Evidence or explanation supplied by the user", required = false),
                capabilityField("userConfirmed", CapabilityDataType.BOOLEAN, "Whether the user confirms the task outcome")
            ),
            cost = capabilityCost(CapabilityCostKind.USER_TIME, "The user performs the task; Trader does not charge for or execute the external action."),
            limits = capabilityLimits(CapabilityLimitMode.USER_CONTROLLED, "No automated execution or product-controlled task duration. The user controls scope and timing."),
            postcondition = capabilityPostcondition("The user explicitly records the outcome; evidence is retained when supplied.", PostconditionVerification.USER_CONFIRMATION, evidenceRequired = true),
            sideEffects = listOf("The user may act outside Trader; Trader only stores the user-reported outcome and evidence."),
            supportsOffline = true
        ),
        CapabilityDescriptor(
            id = "problem.capture.manual",
            title = "Capture a problem",
            description = "Create a local ProblemCase from direct user input",
            availability = CapabilityAvailability.AVAILABLE,
            effect = CapabilityEffect.LOCAL_WRITE,
            retryPolicy = RetryPolicy.SAFE,
            inputs = capabilityIo(
                capabilityField("title", CapabilityDataType.TEXT, "Short problem or goal title"),
                capabilityField("originalDescription", CapabilityDataType.TEXT, "Original user-provided description"),
                capabilityField("desiredOutcome", CapabilityDataType.TEXT, "What a successful result would look like", required = false),
                capabilityField("constraints", CapabilityDataType.TEXT, "Known constraints and exclusions", required = false),
                capabilityField("category", CapabilityDataType.STRING, "Optional user-selected category", required = false)
            ),
            outputs = capabilityIo(
                capabilityField("problemCaseId", CapabilityDataType.IDENTIFIER, "ID of the persisted ProblemCase"),
                capabilityField("capturedAt", CapabilityDataType.STRING, "UTC capture timestamp"),
                capabilityField("status", CapabilityDataType.STRING, "Initial persisted case status")
            ),
            cost = capabilityCost(CapabilityCostKind.DEVICE_RESOURCES, "Uses local device storage and processing; no external service is called."),
            limits = capabilityLimits(CapabilityLimitMode.PLATFORM_BOUNDED, "Bounded by available device storage and Room/SQLite constraints. No product-specific byte cap is currently declared."),
            postcondition = capabilityPostcondition("Read the new ProblemCase back from Room and confirm the saved values and ID.", PostconditionVerification.LOCAL_READ_BACK),
            sideEffects = listOf("Creates one local ProblemCase and its initial activity record."),
            supportsOffline = true
        ),
        CapabilityDescriptor(
            id = "problem.capture.share",
            title = "Capture shared content",
            description = "Save URL or text received through Android share",
            availability = CapabilityAvailability.AVAILABLE,
            effect = CapabilityEffect.LOCAL_WRITE,
            retryPolicy = RetryPolicy.SAFE,
            inputs = capabilityIo(
                capabilityField("sharedText", CapabilityDataType.TEXT, "Text or URL supplied by the Android share intent", required = false),
                capabilityField("sharedUri", CapabilityDataType.URI, "Shared content URI, when supplied", required = false, sensitive = true),
                capabilityField("mimeType", CapabilityDataType.STRING, "Intent MIME type", required = false),
                capabilityField("sourcePackage", CapabilityDataType.STRING, "Originating package when Android provides it", required = false)
            , constraints = listOf("At least one of sharedText or sharedUri must be present.", "Only data actually granted by the Android share intent may be read.")),
            outputs = capabilityIo(
                capabilityField("problemCaseId", CapabilityDataType.IDENTIFIER, "ID of the created ProblemCase"),
                capabilityField("originalContent", CapabilityDataType.TEXT, "Preserved original shared text or URL"),
                capabilityField("inputKind", CapabilityDataType.STRING, "Recorded share input kind"),
                capabilityField("capturedAt", CapabilityDataType.STRING, "UTC capture timestamp")
            ),
            cost = capabilityCost(CapabilityCostKind.DEVICE_RESOURCES, "Uses local storage and Android share-intent access; no external service is called."),
            limits = capabilityLimits(CapabilityLimitMode.PLATFORM_BOUNDED, "Bounded by Android intent payloads, granted URI access and local database/storage limits. No additional product-specific cap is declared."),
            postcondition = capabilityPostcondition("Read the saved case back and verify that the original shared text/URL and source metadata are preserved.", PostconditionVerification.LOCAL_READ_BACK),
            sideEffects = listOf("Creates a local ProblemCase from user-shared content; does not fetch the shared URL."),
            requiredPermissions = setOf("android_share_intent_grant"),
            supportsOffline = true
        ),
        CapabilityDescriptor(
            id = "plan.step.manage",
            title = "Manage plan steps",
            description = "Create and update local plan steps",
            availability = CapabilityAvailability.AVAILABLE,
            effect = CapabilityEffect.LOCAL_WRITE,
            retryPolicy = RetryPolicy.SAFE,
            inputs = capabilityIo(
                capabilityField("problemCaseId", CapabilityDataType.IDENTIFIER, "Parent ProblemCase"),
                capabilityField("operation", CapabilityDataType.STRING, "One of create, update, archive or restore"),
                capabilityField("stepId", CapabilityDataType.IDENTIFIER, "Existing step for update/archive/restore", required = false),
                capabilityField("title", CapabilityDataType.TEXT, "Step title", required = false),
                capabilityField("expectedResult", CapabilityDataType.TEXT, "Expected result or postcondition", required = false),
                capabilityField("position", CapabilityDataType.INTEGER, "Requested order within the plan", required = false)
            , constraints = listOf("The required fields depend on operation: create requires title; update/archive/restore require stepId.")),
            outputs = capabilityIo(
                capabilityField("stepId", CapabilityDataType.IDENTIFIER, "Affected plan-step ID"),
                capabilityField("persistedState", CapabilityDataType.STRING, "State after the operation"),
                capabilityField("position", CapabilityDataType.INTEGER, "Persisted step order")
            ),
            cost = capabilityCost(CapabilityCostKind.DEVICE_RESOURCES, "Uses local Room storage; no external service is called."),
            limits = capabilityLimits(CapabilityLimitMode.PLATFORM_BOUNDED, "Bounded by local database/storage limits. No product-specific plan-size cap is currently declared."),
            postcondition = capabilityPostcondition("Read the step and its parent relationship back from Room; verify the requested order and state.", PostconditionVerification.LOCAL_READ_BACK),
            sideEffects = listOf("Creates or updates local plan-step records and append-only activity/workflow history."),
            supportsOffline = true
        ),
        CapabilityDescriptor(
            id = "activity.read",
            title = "Read activity history",
            description = "Inspect locally stored activity and workflow events",
            availability = CapabilityAvailability.AVAILABLE,
            effect = CapabilityEffect.LOCAL_READ,
            retryPolicy = RetryPolicy.SAFE,
            inputs = capabilityIo(
                capabilityField("problemCaseId", CapabilityDataType.IDENTIFIER, "Limit results to one ProblemCase", required = false),
                capabilityField("eventTypes", CapabilityDataType.STRING_LIST, "Filter by event type", required = false),
                capabilityField("after", CapabilityDataType.STRING, "UTC lower time bound", required = false),
                capabilityField("limit", CapabilityDataType.INTEGER, "Maximum records requested", required = false)
            ),
            outputs = capabilityIo(
                capabilityField("events", CapabilityDataType.OBJECT_LIST, "Ordered activity/workflow event records"),
                capabilityField("returnedCount", CapabilityDataType.INTEGER, "Number of returned records"),
                capabilityField("hasMore", CapabilityDataType.BOOLEAN, "Whether more records are available")
            ),
            cost = capabilityCost(CapabilityCostKind.DEVICE_RESOURCES, "Reads local Room data; no external service is called."),
            limits = capabilityLimits(CapabilityLimitMode.PLATFORM_BOUNDED, "Bounded by the local database and caller-supplied page size. A hard page-size maximum is not yet declared."),
            postcondition = capabilityPostcondition("Return records matching the filters in deterministic chronological order.", PostconditionVerification.STRUCTURAL_VALIDATION),
            sideEffects = listOf("No data is changed; reads local activity and workflow history."),
            supportsOffline = true
        ),
        CapabilityDescriptor(
            id = "browser.visible.open",
            title = "Open visible browser session",
            description = "Navigate in a user-visible browser session with pause and takeover",
            availability = CapabilityAvailability.NOT_IMPLEMENTED,
            effect = CapabilityEffect.BROWSER_INTERACTION,
            retryPolicy = RetryPolicy.SAFE,
            inputs = capabilityIo(
                capabilityField("url", CapabilityDataType.URI, "URL to open"),
                capabilityField("sessionPurpose", CapabilityDataType.TEXT, "Why the page is being opened"),
                capabilityField("allowedOrigins", CapabilityDataType.STRING_LIST, "Optional origin scope for the session", required = false)
            , constraints = listOf("Only HTTP/HTTPS URLs are permitted.", "The user must be able to see, pause and take over the browser session.", "Authentication, paywall, CAPTCHA and other access controls must not be bypassed.")),
            outputs = capabilityIo(
                capabilityField("sessionId", CapabilityDataType.IDENTIFIER, "Visible browser session ID"),
                capabilityField("currentUrl", CapabilityDataType.URI, "Current page URL"),
                capabilityField("pageTitle", CapabilityDataType.TEXT, "Current page title", required = false),
                capabilityField("sessionState", CapabilityDataType.STRING, "Visible, paused, stopped or user-takeover state")
            ),
            cost = capabilityCost(CapabilityCostKind.NETWORK_DATA, "May use mobile or Wi-Fi data while loading the user-requested page.", requiresConsent = true),
            limits = capabilityLimits(CapabilityLimitMode.ADAPTER_MUST_DECLARE, "Browser adapter must declare URL scope, navigation timeout, page/resource ceilings and concurrent-session limit before availability can be promoted."),
            postcondition = capabilityPostcondition("Confirm that the visible browser reached the intended origin and exposes its current URL to the user.", PostconditionVerification.SOURCE_READ_BACK),
            sideEffects = listOf("Loads a remote page in a visible browser; page scripts may have ordinary browser-visible effects, but Trader must not submit forms or click consequential controls without a separate capability."),
            requiredPermissions = setOf("user_initiated_browser_session", "network_access"),
            requiresVisibleSurface = true
        ),
        CapabilityDescriptor(
            id = "web.public.search",
            title = "Search permitted public sources",
            description = "Discover public information with source attribution and rate limits",
            availability = CapabilityAvailability.NOT_IMPLEMENTED,
            effect = CapabilityEffect.NETWORK_READ,
            retryPolicy = RetryPolicy.SAFE,
            inputs = capabilityIo(
                capabilityField("query", CapabilityDataType.TEXT, "Search terms or question"),
                capabilityField("sourceScope", CapabilityDataType.STRING_LIST, "Allowed public source domains or source IDs"),
                capabilityField("locale", CapabilityDataType.STRING, "Preferred result language/locale", required = false),
                capabilityField("maxResults", CapabilityDataType.INTEGER, "Maximum number of results", required = false)
            , constraints = listOf("Only permitted public sources may be searched.", "Source terms, robots directives, rate limits and access restrictions must be checked by the adapter.")),
            outputs = capabilityIo(
                capabilityField("results", CapabilityDataType.OBJECT_LIST, "Results with title, URL, excerpt and source identifier"),
                capabilityField("capturedAt", CapabilityDataType.STRING, "UTC retrieval timestamp"),
                capabilityField("sourceHealth", CapabilityDataType.STRING, "Per-source success, blocked, limited or failed state")
            ),
            cost = capabilityCost(CapabilityCostKind.UNKNOWN_UNTIL_ADAPTER_SELECTED, "Search provider pricing and network-data use are not known until a permitted adapter is selected.", requiresConsent = true),
            limits = capabilityLimits(CapabilityLimitMode.ADAPTER_MUST_DECLARE, "Adapter must declare provider quotas, per-source rate limits, request timeout, result ceiling, response-size ceiling and concurrency."),
            postcondition = capabilityPostcondition("Each returned result retains a source URL and capture time; blocked/failed sources remain explicit.", PostconditionVerification.STRUCTURAL_VALIDATION, evidenceRequired = true),
            sideEffects = listOf("Makes read-only requests to permitted public sources and stores source-linked result metadata."),
            requiredPermissions = setOf("network_access", "public_source_policy_approved")
        ),
        CapabilityDescriptor(
            id = "web.page.fetch",
            title = "Fetch a public page",
            description = "Read a permitted public page and preserve its source URL",
            availability = CapabilityAvailability.NOT_IMPLEMENTED,
            effect = CapabilityEffect.NETWORK_READ,
            retryPolicy = RetryPolicy.SAFE,
            inputs = capabilityIo(
                capabilityField("url", CapabilityDataType.URI, "Public page URL"),
                capabilityField("sourcePolicyId", CapabilityDataType.IDENTIFIER, "Approved source access-policy record"),
                capabilityField("maxBytes", CapabilityDataType.INTEGER, "Requested response-byte ceiling", required = false)
            , constraints = listOf("Only permitted public pages may be fetched.", "The adapter must not follow redirects outside the approved origin scope without re-evaluation.", "Do not bypass authentication, paywalls, CAPTCHAs or technical access controls.")),
            outputs = capabilityIo(
                capabilityField("finalUrl", CapabilityDataType.URI, "Final URL after allowed redirects"),
                capabilityField("httpStatus", CapabilityDataType.INTEGER, "HTTP response status"),
                capabilityField("mimeType", CapabilityDataType.STRING, "Response content type"),
                capabilityField("rawEvidenceRef", CapabilityDataType.IDENTIFIER, "Reference to preserved raw response/evidence"),
                capabilityField("capturedAt", CapabilityDataType.STRING, "UTC retrieval timestamp")
            ),
            cost = capabilityCost(CapabilityCostKind.UNKNOWN_UNTIL_ADAPTER_SELECTED, "Network-data use and any provider charge depend on the selected fetch adapter.", requiresConsent = true),
            limits = capabilityLimits(CapabilityLimitMode.ADAPTER_MUST_DECLARE, "Adapter must enforce request/response byte ceilings, timeout, redirect count, per-origin rate limit and concurrency."),
            postcondition = capabilityPostcondition("Preserve the source URL, final URL, response metadata and raw evidence reference; record blocked and failed outcomes distinctly.", PostconditionVerification.SOURCE_READ_BACK, evidenceRequired = true),
            sideEffects = listOf("Makes a read-only public network request and stores source-linked response evidence."),
            requiredPermissions = setOf("network_access", "public_source_policy_approved")
        ),
        CapabilityDescriptor(
            id = "android.app.handoff",
            title = "Hand off to an installed app",
            description = "Open a supported app or deep link for a user-visible action",
            availability = CapabilityAvailability.NOT_IMPLEMENTED,
            effect = CapabilityEffect.APP_HANDOFF,
            retryPolicy = RetryPolicy.USER_CONFIRMED,
            inputs = capabilityIo(
                capabilityField("targetUri", CapabilityDataType.URI, "Deep link or content URI to open"),
                capabilityField("intentAction", CapabilityDataType.STRING, "Android intent action"),
                capabilityField("packageName", CapabilityDataType.STRING, "Preferred target package", required = false),
                capabilityField("handoffPurpose", CapabilityDataType.TEXT, "User-visible reason for opening the app")
            , constraints = listOf("The handoff must remain user initiated and visible.", "Trader must not claim the target action succeeded merely because an intent was launched.")),
            outputs = capabilityIo(
                capabilityField("launchState", CapabilityDataType.STRING, "Launched, no handler, cancelled or failed"),
                capabilityField("resolvedPackage", CapabilityDataType.STRING, "Resolved target package", required = false),
                capabilityField("userReportedOutcome", CapabilityDataType.TEXT, "Outcome confirmed by the user", required = false)
            ),
            cost = capabilityCost(CapabilityCostKind.DEVICE_RESOURCES, "Uses Android intent resolution and the target app; any target-app/network charges are outside Trader's control."),
            limits = capabilityLimits(CapabilityLimitMode.PLATFORM_BOUNDED, "Bounded by Android intent resolution, URI grants and target-app behavior. No app-specific timeout or completion guarantee is available."),
            postcondition = capabilityPostcondition("Distinguish successful intent launch from completion of the task in the target app; require user confirmation for the latter.", PostconditionVerification.USER_CONFIRMATION),
            sideEffects = listOf("Launches another installed app or opens a deep link in a visible user-controlled surface."),
            requiredPermissions = setOf("user_initiated_app_handoff"),
            requiresVisibleSurface = true
        ),
        CapabilityDescriptor(
            id = "android.accessibility.action",
            title = "Bounded installed-app interaction",
            description = "Perform a narrowly scoped, visible interaction through a user-enabled accessibility service",
            availability = CapabilityAvailability.NOT_IMPLEMENTED,
            effect = CapabilityEffect.APP_INTERACTION,
            retryPolicy = RetryPolicy.NEVER,
            inputs = capabilityIo(
                capabilityField("packageName", CapabilityDataType.STRING, "Target app package"),
                capabilityField("screenScope", CapabilityDataType.TEXT, "Allowed screen or task scope"),
                capabilityField("targetDescription", CapabilityDataType.TEXT, "User-reviewed target element description"),
                capabilityField("action", CapabilityDataType.STRING, "Specific bounded UI action"),
                capabilityField("approvedPayloadHash", CapabilityDataType.STRING, "Hash of the exact approved action payload")
            , constraints = listOf("Only the reviewed screen scope and target action may be used.", "No password, secure-field, access-control or anti-abuse bypass.", "An ambiguous action outcome must be sent to NEEDS_REVIEW and never automatically retried.")),
            outputs = capabilityIo(
                capabilityField("observedActionState", CapabilityDataType.STRING, "Observed, not observed, blocked or uncertain"),
                capabilityField("resultingScreenEvidenceRef", CapabilityDataType.IDENTIFIER, "Evidence reference for the resulting visible state", required = false),
                capabilityField("userReviewRequired", CapabilityDataType.BOOLEAN, "Whether the outcome needs human review")
            ),
            cost = capabilityCost(CapabilityCostKind.DEVICE_RESOURCES, "Uses device accessibility and UI resources; target-app and network costs remain outside Trader's control."),
            limits = capabilityLimits(CapabilityLimitMode.ADAPTER_MUST_DECLARE, "Adapter must declare allowed packages/actions, per-action timeout, UI observation timeout, concurrency and evidence-size ceilings."),
            postcondition = capabilityPostcondition("Observe the expected resulting UI state and preserve evidence; uncertain outcomes require human review.", PostconditionVerification.USER_CONFIRMATION, evidenceRequired = true),
            sideEffects = listOf("Interacts with a third-party app UI and may cause consequential changes within the exact approved scope."),
            requiresApproval = true,
            requiredPermissions = setOf("accessibility_service_enabled", "explicit_action_approval"),
            requiresVisibleSurface = true
        ),
        CapabilityDescriptor(
            id = "ai.context.prepare",
            title = "Prepare bounded AI context",
            description = "Assemble a selected, inspectable context packet from problem data and evidence",
            availability = CapabilityAvailability.NOT_IMPLEMENTED,
            effect = CapabilityEffect.LOCAL_COMPUTE,
            retryPolicy = RetryPolicy.SAFE,
            inputs = capabilityIo(
                capabilityField("problemCaseId", CapabilityDataType.IDENTIFIER, "ProblemCase to prepare"),
                capabilityField("evidenceIds", CapabilityDataType.STRING_LIST, "Explicitly selected evidence IDs", required = false),
                capabilityField("planStepIds", CapabilityDataType.STRING_LIST, "Explicitly selected plan-step IDs", required = false),
                capabilityField("instructions", CapabilityDataType.TEXT, "User-editable task instructions", required = false, sensitive = true),
                capabilityField("maxContextBytes", CapabilityDataType.INTEGER, "User-selected context size ceiling")
            , constraints = listOf("Only selected records may enter the packet.", "Original evidence and provenance must remain linked; redaction must not overwrite source records.")),
            outputs = capabilityIo(
                capabilityField("contextPacketId", CapabilityDataType.IDENTIFIER, "Packet identifier"),
                capabilityField("content", CapabilityDataType.TEXT, "Inspectable context content", sensitive = true),
                capabilityField("sourceIds", CapabilityDataType.STRING_LIST, "Source/evidence identifiers included"),
                capabilityField("omittedCount", CapabilityDataType.INTEGER, "Number of records omitted by the size boundary"),
                capabilityField("truncated", CapabilityDataType.BOOLEAN, "Whether content was truncated to stay within the requested limit")
            ),
            cost = capabilityCost(CapabilityCostKind.DEVICE_RESOURCES, "Local formatting and redaction use device memory/CPU; no model provider is called."),
            limits = capabilityLimits(CapabilityLimitMode.PLATFORM_BOUNDED, "Must honor the caller's maxContextBytes and device memory limits; enforce deterministic truncation and report omissions."),
            postcondition = capabilityPostcondition("Validate the byte ceiling and verify every included claim retains its source/evidence reference.", PostconditionVerification.STRUCTURAL_VALIDATION),
            sideEffects = listOf("Builds an inspectable context packet; does not transmit it or alter original evidence."),
            supportsOffline = true
        ),
        CapabilityDescriptor(
            id = "ai.inference.local",
            title = "Run local model inference",
            description = "Use a compatible on-device model without requiring paid API credits",
            availability = CapabilityAvailability.NOT_IMPLEMENTED,
            effect = CapabilityEffect.LOCAL_COMPUTE,
            retryPolicy = RetryPolicy.USER_CONFIRMED,
            inputs = capabilityIo(
                capabilityField("modelId", CapabilityDataType.IDENTIFIER, "Explicitly selected local model"),
                capabilityField("contextPacketId", CapabilityDataType.IDENTIFIER, "Selected bounded context packet"),
                capabilityField("prompt", CapabilityDataType.TEXT, "Prompt/instructions", sensitive = true),
                capabilityField("maxOutputTokens", CapabilityDataType.INTEGER, "Requested generation ceiling")
            , constraints = listOf("Model files must be locally available and compatible with the device.", "No external network request may be made by this capability.")),
            outputs = capabilityIo(
                capabilityField("text", CapabilityDataType.TEXT, "Generated suggestion", sensitive = true),
                capabilityField("modelId", CapabilityDataType.IDENTIFIER, "Model that produced the output"),
                capabilityField("inputTokens", CapabilityDataType.INTEGER, "Reported input-token count", required = false),
                capabilityField("outputTokens", CapabilityDataType.INTEGER, "Reported output-token count", required = false),
                capabilityField("finishReason", CapabilityDataType.STRING, "Completed, length-limited, cancelled or failed")
            ),
            cost = capabilityCost(CapabilityCostKind.DEVICE_RESOURCES, "Uses local storage, memory, CPU/GPU and battery; no paid API is required."),
            limits = capabilityLimits(CapabilityLimitMode.ADAPTER_MUST_DECLARE, "Runtime must declare model compatibility, context window, output-token ceiling, memory requirement, timeout and concurrency for the selected device/model."),
            postcondition = capabilityPostcondition("Validate output bounds and structure; keep generated claims marked as suggestions until supported by evidence or human review.", PostconditionVerification.STRUCTURAL_VALIDATION),
            sideEffects = listOf("Runs inference locally and returns generated text; must not silently upload context or alter source records."),
            supportsOffline = true
        ),
        CapabilityDescriptor(
            id = "ai.inference.remote",
            title = "Run remote model inference",
            description = "Send an explicitly selected and consented context packet to a remote model",
            availability = CapabilityAvailability.NOT_IMPLEMENTED,
            effect = CapabilityEffect.NETWORK_READ,
            retryPolicy = RetryPolicy.USER_CONFIRMED,
            inputs = capabilityIo(
                capabilityField("providerId", CapabilityDataType.IDENTIFIER, "Selected remote provider"),
                capabilityField("modelId", CapabilityDataType.STRING, "Selected provider model"),
                capabilityField("contextPacketId", CapabilityDataType.IDENTIFIER, "Exact context packet to transmit"),
                capabilityField("prompt", CapabilityDataType.TEXT, "Prompt/instructions", sensitive = true),
                capabilityField("maxOutputTokens", CapabilityDataType.INTEGER, "Maximum requested output tokens"),
                capabilityField("remoteConsentId", CapabilityDataType.IDENTIFIER, "Consent record for this provider and context", sensitive = true)
            , constraints = listOf("Show the exact context, provider, model and current cost information before transmission.", "Paid credits must remain optional; do not make this capability a dependency of core workflows.", "Provider credentials must never be part of the context packet.")),
            outputs = capabilityIo(
                capabilityField("text", CapabilityDataType.TEXT, "Generated suggestion", sensitive = true),
                capabilityField("providerRequestId", CapabilityDataType.STRING, "Provider request identifier", required = false),
                capabilityField("usage", CapabilityDataType.OBJECT, "Provider-reported token/usage metadata", required = false),
                capabilityField("actualCost", CapabilityDataType.DECIMAL, "Provider-reported cost when available", required = false),
                capabilityField("finishReason", CapabilityDataType.STRING, "Completed, length-limited, cancelled or failed")
            ),
            cost = capabilityCost(CapabilityCostKind.PROVIDER_BILLED, "Provider pricing, free quotas and data-transfer charges vary. Obtain current pricing and explicit per-provider consent before enabling.", requiresConsent = true),
            limits = capabilityLimits(CapabilityLimitMode.ADAPTER_MUST_DECLARE, "Provider adapter must declare current quotas, rate limits, request/response byte limits, context/output-token ceilings, timeout, concurrency and cost ceiling."),
            postcondition = capabilityPostcondition("Validate provider response and usage metadata; record provider/model and consent reference; generated claims remain unverified suggestions.", PostconditionVerification.STRUCTURAL_VALIDATION),
            sideEffects = listOf("Transmits the exact selected context to a third-party provider and may incur provider charges."),
            requiresApproval = true,
            requiredPermissions = setOf("remote_model_consent", "network_access", "provider_credentials")
        ),
        CapabilityDescriptor(
            id = "communication.draft",
            title = "Prepare a communication draft",
            description = "Create a local draft from verified facts for user review",
            availability = CapabilityAvailability.NOT_IMPLEMENTED,
            effect = CapabilityEffect.LOCAL_WRITE,
            retryPolicy = RetryPolicy.USER_CONFIRMED,
            inputs = capabilityIo(
                capabilityField("recipientReference", CapabilityDataType.IDENTIFIER, "Intended recipient record", required = false, sensitive = true),
                capabilityField("channel", CapabilityDataType.STRING, "Intended communication channel"),
                capabilityField("purpose", CapabilityDataType.TEXT, "Reason for contacting the recipient"),
                capabilityField("language", CapabilityDataType.STRING, "Requested draft language"),
                capabilityField("verifiedFactIds", CapabilityDataType.STRING_LIST, "Evidence-backed facts permitted in the draft"),
                capabilityField("instructions", CapabilityDataType.TEXT, "User-provided drafting instructions", required = false, sensitive = true)
            , constraints = listOf("Do not invent relationships, authority, prices, stock, delivery, certifications or prior contact.", "Claims without supporting evidence must be visibly flagged, not stated as fact.")),
            outputs = capabilityIo(
                capabilityField("draftId", CapabilityDataType.IDENTIFIER, "Persisted draft ID"),
                capabilityField("content", CapabilityDataType.TEXT, "Draft message", sensitive = true),
                capabilityField("sourceFactIds", CapabilityDataType.STRING_LIST, "Evidence IDs supporting factual claims"),
                capabilityField("unverifiedClaims", CapabilityDataType.STRING_LIST, "Claims requiring user verification")
            ),
            cost = capabilityCost(CapabilityCostKind.DEVICE_RESOURCES, "Local draft preparation and storage; no message is transmitted."),
            limits = capabilityLimits(CapabilityLimitMode.PLATFORM_BOUNDED, "Bounded by local storage and the selected channel's message constraints. Channel-specific maximums must be checked before handoff."),
            postcondition = capabilityPostcondition("Read the stored draft back and verify that each factual claim maps to supplied evidence or is marked unverified.", PostconditionVerification.LOCAL_READ_BACK),
            sideEffects = listOf("Creates a local draft only; does not open a channel or send a message."),
            supportsOffline = true
        ),
        CapabilityDescriptor(
            id = "communication.send",
            title = "Send an external communication",
            description = "Hand off or send an exact approved message to a specific recipient and channel",
            availability = CapabilityAvailability.NOT_IMPLEMENTED,
            effect = CapabilityEffect.EXTERNAL_SIDE_EFFECT,
            retryPolicy = RetryPolicy.NEVER,
            inputs = capabilityIo(
                capabilityField("recipientReference", CapabilityDataType.IDENTIFIER, "Exact recipient record or address", sensitive = true),
                capabilityField("channel", CapabilityDataType.STRING, "Exact approved channel"),
                capabilityField("purpose", CapabilityDataType.TEXT, "Approved communication purpose"),
                capabilityField("payload", CapabilityDataType.TEXT, "Exact message content", sensitive = true),
                capabilityField("payloadSha256", CapabilityDataType.STRING, "Hash of the exact message payload"),
                capabilityField("approvalRecordId", CapabilityDataType.IDENTIFIER, "Approval record bound to recipient, channel, purpose and payload")
            , constraints = listOf("Immediately before sending, re-check recipient, channel, purpose and payload hash against the approval.", "Never automatically retry an uncertain send.", "Respect suppression, contact basis, consent and jurisdiction/channel policy.")),
            outputs = capabilityIo(
                capabilityField("outcomeState", CapabilityDataType.STRING, "Confirmed sent, rejected, failed or uncertain"),
                capabilityField("providerMessageId", CapabilityDataType.STRING, "Provider receipt/message identifier", required = false),
                capabilityField("sentAt", CapabilityDataType.STRING, "Provider-confirmed UTC send time", required = false),
                capabilityField("receiptEvidenceRef", CapabilityDataType.IDENTIFIER, "Evidence of the provider outcome", required = false)
            ),
            cost = capabilityCost(CapabilityCostKind.UNKNOWN_UNTIL_ADAPTER_SELECTED, "Channel fees and provider pricing are unknown until an authorized adapter is selected; disclose any charge before approval.", requiresConsent = true),
            limits = capabilityLimits(CapabilityLimitMode.ADAPTER_MUST_DECLARE, "Channel adapter must declare provider quota, per-recipient rate limits, timeout, maximum message size, concurrency and uncertain-result handling."),
            postcondition = capabilityPostcondition("Require provider confirmation or an explicit uncertain state with evidence; never infer delivery from a successful local call.", PostconditionVerification.PROVIDER_CONFIRMATION, evidenceRequired = true),
            sideEffects = listOf("Sends or hands off an external message to the exact recipient through the approved channel."),
            requiresApproval = true,
            requiredPermissions = setOf("explicit_action_approval", "authorized_channel"),
            requiresVisibleSurface = true
        ),
        CapabilityDescriptor(
            id = "browser.submit",
            title = "Submit an external form",
            description = "Submit a form only after exact payload approval and postcondition verification",
            availability = CapabilityAvailability.NOT_IMPLEMENTED,
            effect = CapabilityEffect.EXTERNAL_SIDE_EFFECT,
            retryPolicy = RetryPolicy.NEVER,
            inputs = capabilityIo(
                capabilityField("browserSessionId", CapabilityDataType.IDENTIFIER, "Visible browser session"),
                capabilityField("targetUrl", CapabilityDataType.URI, "Exact form submission destination"),
                capabilityField("formValues", CapabilityDataType.OBJECT, "Exact reviewed form field/value map", sensitive = true),
                capabilityField("payloadSha256", CapabilityDataType.STRING, "Hash of the exact form payload"),
                capabilityField("approvalRecordId", CapabilityDataType.IDENTIFIER, "Approval record bound to destination and payload")
            , constraints = listOf("Immediately before submission, re-check URL, form fields and payload hash against approval.", "Never automatically retry an uncertain submission.", "The user must be able to inspect and take over the visible browser session.")),
            outputs = capabilityIo(
                capabilityField("outcomeState", CapabilityDataType.STRING, "Confirmed submitted, rejected, failed or uncertain"),
                capabilityField("confirmationText", CapabilityDataType.TEXT, "Observed confirmation message", required = false),
                capabilityField("responseUrl", CapabilityDataType.URI, "Observed response/confirmation URL", required = false),
                capabilityField("evidenceRef", CapabilityDataType.IDENTIFIER, "Captured confirmation evidence", required = false)
            ),
            cost = capabilityCost(CapabilityCostKind.UNKNOWN_UNTIL_ADAPTER_SELECTED, "Any transaction, subscription or service cost is unknown until the destination and form purpose are reviewed.", requiresConsent = true),
            limits = capabilityLimits(CapabilityLimitMode.ADAPTER_MUST_DECLARE, "Browser adapter must declare host/origin allowlist, navigation and submit timeout, request-size ceiling, concurrency and recovery behavior."),
            postcondition = capabilityPostcondition("Require a source-observed confirmation or mark the result uncertain and route to human review.", PostconditionVerification.PROVIDER_CONFIRMATION, evidenceRequired = true),
            sideEffects = listOf("Submits data to an external website and may create an account, application, order or other consequential record."),
            requiresApproval = true,
            requiredPermissions = setOf("explicit_action_approval", "authorized_browser_session"),
            requiresVisibleSurface = true
        )
    )

    private val byId = descriptors.associateBy { it.id }

    init {
        check(byId.size == descriptors.size) { "Capability IDs must be unique" }
        check(descriptors.all { it.inputs.fields.isNotEmpty() && it.outputs.fields.isNotEmpty() }) {
            "Every capability must declare input and output fields"
        }
        check(descriptors.all { descriptor ->
            descriptor.inputs.fields.map { it.name }.toSet().size == descriptor.inputs.fields.size &&
                descriptor.outputs.fields.map { it.name }.toSet().size == descriptor.outputs.fields.size
        }) {
            "Capability input/output field names must be unique within each contract"
        }
        check(descriptors.all {
            it.cost.disclosure.isNotBlank() &&
                it.limits.description.isNotBlank() &&
                it.postcondition.required &&
                it.postcondition.description.isNotBlank() &&
                it.sideEffects.isNotEmpty()
        }) {
            "Every capability must disclose cost, limits, side effects and a required postcondition"
        }
        check(descriptors.filter { it.effect == CapabilityEffect.EXTERNAL_SIDE_EFFECT }
            .all {
                it.requiresApproval &&
                    it.retryPolicy == RetryPolicy.NEVER &&
                    it.cost.requiresExplicitConsent &&
                    it.postcondition.verification == PostconditionVerification.PROVIDER_CONFIRMATION
            }) {
            "External side effects require exact approval, explicit cost consent, no retries and provider confirmation"
        }
        check(descriptors.filter { it.availability == CapabilityAvailability.AVAILABLE }
            .none { it.limits.mode == CapabilityLimitMode.ADAPTER_MUST_DECLARE }) {
            "A capability cannot be executable until its adapter limits are declared"
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
            missing.isNotEmpty() -> "Missing permissions or consent: \${missing.sorted().joinToString()}"
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
