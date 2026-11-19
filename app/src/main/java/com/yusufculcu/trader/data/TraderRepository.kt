package com.yusufculcu.trader.data

import androidx.room.withTransaction
import com.yusufculcu.trader.domain.CapabilityRegistry
import com.yusufculcu.trader.domain.ProblemRules
import com.yusufculcu.trader.domain.OpportunityRules
import com.yusufculcu.trader.domain.ProblemStatus
import com.yusufculcu.trader.domain.WorkflowRunStatus
import com.yusufculcu.trader.domain.WorkflowRules
import com.yusufculcu.trader.domain.WorkflowStepStatus
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import java.util.UUID

data class WorkflowStepDraft(
    val title: String,
    val capabilityId: String = "manual.task",
    val inputJson: String = "{}",
    val expectedPostcondition: String = "",
    val retrySafe: Boolean = false,
    val requiresApproval: Boolean = false
)

data class OpportunityDraft(
    val lane: String,
    val organizationName: String,
    val opportunityName: String,
    val contactName: String,
    val contactRoute: String,
    val fitNotes: String,
    val stage: String,
    val nextAction: String,
    val followUpAt: Long?
)

data class OpportunityListItem(
    val problem: ProblemEntity,
    val opportunity: OpportunityEntity
)

class TraderRepository(private val database: TraderDatabase) {
    fun observeProblems(): Flow<List<ProblemEntity>> = database.problemDao().observeAll()
    fun observeArchivedProblems(): Flow<List<ProblemEntity>> = database.problemDao().observeArchived()
    fun observeSteps(problemId: String): Flow<List<PlanStepEntity>> = database.planStepDao().observeForProblem(problemId)
    fun observeActivity(): Flow<List<ActivityEventEntity>> = database.activityDao().observeRecent()
    fun observeEvidence(problemId: String): Flow<List<EvidenceEntity>> = database.evidenceDao().observeForProblem(problemId)
    fun observeOpportunity(problemId: String): Flow<OpportunityEntity?> = database.opportunityDao().observeForProblem(problemId)
    fun observeOpportunities(): Flow<List<OpportunityEntity>> = database.opportunityDao().observeAll()
    fun observeCount(): Flow<Int> = database.problemDao().observeCount()
    fun observeWorkflowRuns(problemId: String): Flow<List<WorkflowRunEntity>> =
        database.workflowRunDao().observeForProblem(problemId)
    fun observeWorkflowSteps(runId: String): Flow<List<WorkflowStepEntity>> =
        database.workflowStepDao().observeForRun(runId)
    fun observeWorkflowEvents(runId: String): Flow<List<WorkflowEventEntity>> =
        database.workflowEventDao().observeForRun(runId)

    suspend fun createProblem(
        title: String,
        description: String = "",
        desiredOutcome: String = "",
        constraints: String = "",
        category: String = "General",
        inputKind: String = "manual",
        originalContent: String = description,
        sourceUri: String? = null
    ): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        database.withTransaction {
            database.problemDao().insert(
                ProblemEntity(
                    id, ProblemRules.normalizeTitle(title).ifBlank { "Untitled problem" }, description.trim(),
                    desiredOutcome.trim(), constraints.trim(), category, ProblemStatus.OPEN.name, inputKind,
                    originalContent, sourceUri, "", now, now
                )
            )
            addEvent(id, "created", "Problem captured", "Created from $inputKind", now)
        }
        return id
    }

    suspend fun updateProblemDetails(
        id: String,
        title: String,
        description: String,
        desiredOutcome: String,
        constraints: String
    ): Boolean {
        val cleanTitle = ProblemRules.normalizeTitle(title)
        if (cleanTitle.isBlank()) return false
        val now = System.currentTimeMillis()
        return database.withTransaction {
            val existing = database.problemDao().getById(id) ?: return@withTransaction false
            if (existing.archivedAt != null) return@withTransaction false
            database.problemDao().updateDetails(
                id, cleanTitle, description.trim(), desiredOutcome.trim(), constraints.trim(), now
            )
            addEvent(id, "details_updated", "Problem details updated", cleanTitle, now)
            true
        }
    }

    suspend fun archiveProblem(id: String): Boolean = setArchiveState(id, true)
    suspend fun restoreProblem(id: String): Boolean = setArchiveState(id, false)

    private suspend fun setArchiveState(id: String, archived: Boolean): Boolean = database.withTransaction {
        val existing = database.problemDao().getById(id) ?: return@withTransaction false
        if ((existing.archivedAt != null) == archived) return@withTransaction true
        val now = System.currentTimeMillis()
        database.problemDao().setArchivedAt(id, if (archived) now else null, now)
        addEvent(
            id,
            if (archived) "archived" else "restored",
            if (archived) "Problem archived" else "Problem restored",
            existing.title,
            now
        )
        true
    }

    suspend fun addEvidence(
        problemId: String,
        title: String,
        content: String,
        sourceUri: String?
    ): Boolean {
        val cleanTitle = title.trim().take(180)
        val cleanContent = content.trim().take(20_000)
        val cleanUri = sourceUri?.trim()?.takeIf { it.isNotBlank() }?.take(2_000)
        if (cleanTitle.isBlank() || (cleanContent.isBlank() && cleanUri == null)) return false
        val now = System.currentTimeMillis()
        return database.withTransaction {
            val problem = database.problemDao().getById(problemId) ?: return@withTransaction false
            if (problem.archivedAt != null) return@withTransaction false
            val kind = when {
                cleanUri?.startsWith("http://") == true || cleanUri?.startsWith("https://") == true -> "URL"
                else -> "NOTE"
            }
            database.evidenceDao().insert(
                EvidenceEntity(
                    id = UUID.randomUUID().toString(),
                    problemId = problemId,
                    title = cleanTitle,
                    content = cleanContent,
                    sourceUri = cleanUri,
                    sourceKind = kind,
                    verificationState = "UNREVIEWED",
                    capturedAt = now
                )
            )
            database.problemDao().touch(problemId, now)
            addEvent(problemId, "evidence_added", "Evidence added", cleanTitle, now)
            true
        }
    }

    suspend fun setEvidenceVerification(evidence: EvidenceEntity, verified: Boolean): Boolean =
        database.withTransaction {
            val state = if (verified) "VERIFIED" else "UNREVIEWED"
            database.evidenceDao().updateVerificationState(evidence.id, state)
            val now = System.currentTimeMillis()
            database.problemDao().touch(evidence.problemId, now)
            addEvent(
                evidence.problemId,
                if (verified) "evidence_verified" else "evidence_unreviewed",
                if (verified) "Evidence marked verified by user" else "Evidence verification removed",
                evidence.title,
                now
            )
            true
        }

    suspend fun saveOpportunity(problemId: String, draft: OpportunityDraft): Boolean {
        val lane = draft.lane.trim().uppercase()
        val stage = draft.stage.trim().uppercase()
        val organizationName = draft.organizationName.trim().take(240)
        val opportunityName = draft.opportunityName.trim().take(300)
        val contactName = draft.contactName.trim().take(180)
        val contactRoute = draft.contactRoute.trim().take(500)
        val fitNotes = draft.fitNotes.trim().take(4_000)
        val nextAction = draft.nextAction.trim().take(500)
        val followUpAt = draft.followUpAt?.takeIf { it > 0L }

        if (!OpportunityRules.isValidLane(lane) || !OpportunityRules.isValidStage(stage) || opportunityName.isBlank()) {
            return false
        }

        return database.withTransaction {
            val problem = database.problemDao().getById(problemId) ?: return@withTransaction false
            if (problem.archivedAt != null) return@withTransaction false

            val existing = database.opportunityDao().getByProblemId(problemId)
            val now = System.currentTimeMillis()
            val saved = if (existing == null) {
                OpportunityEntity(
                    id = UUID.randomUUID().toString(),
                    problemId = problemId,
                    lane = lane,
                    organizationName = organizationName,
                    opportunityName = opportunityName,
                    contactName = contactName,
                    contactRoute = contactRoute,
                    fitNotes = fitNotes,
                    stage = stage,
                    nextAction = nextAction,
                    followUpAt = followUpAt,
                    createdAt = now,
                    updatedAt = now
                ).also { database.opportunityDao().insert(it) }
            } else {
                existing.copy(
                    lane = lane,
                    organizationName = organizationName,
                    opportunityName = opportunityName,
                    contactName = contactName,
                    contactRoute = contactRoute,
                    fitNotes = fitNotes,
                    stage = stage,
                    nextAction = nextAction,
                    followUpAt = followUpAt,
                    updatedAt = now
                ).also { database.opportunityDao().update(it) }
            }

            database.problemDao().touch(problemId, now)
            val eventType: String
            val summary: String
            val details: String
            when {
                existing == null -> {
                    eventType = "opportunity_created"
                    summary = "Opportunity tracking started"
                    details = saved.lane + " · " + saved.organizationName + " · " + saved.opportunityName
                }
                existing.stage != saved.stage -> {
                    eventType = "opportunity_stage_changed"
                    summary = "Opportunity stage changed"
                    details = existing.stage + " → " + saved.stage
                }
                else -> {
                    eventType = "opportunity_updated"
                    summary = "Opportunity details updated"
                    details = saved.organizationName + " · " + saved.opportunityName
                }
            }
            addEvent(problemId, eventType, summary, details, now)
            true
        }
    }

    suspend fun addStep(problemId: String, title: String, expectedResult: String = "") {
        val clean = title.trim().take(180)
        if (clean.isBlank()) return
        val cleanExpected = expectedResult.trim().take(2_000)
        val now = System.currentTimeMillis()
        database.withTransaction {
            val position = database.planStepDao().nextPosition(problemId)
            database.planStepDao().insert(
                PlanStepEntity(UUID.randomUUID().toString(), problemId, clean, "", "", cleanExpected, "TODO", position, now, now)
            )
            database.problemDao().touch(problemId, now)
            addEvent(problemId, "plan_step_added", "Plan step added", clean, now)
        }
    }

    suspend fun toggleStep(step: PlanStepEntity) {
        val next = if (step.status == "DONE") "TODO" else "DONE"
        val now = System.currentTimeMillis()
        database.withTransaction {
            database.planStepDao().updateStatus(step.id, next, now)
            database.problemDao().touch(step.problemId, now)
            addEvent(step.problemId, if (next == "DONE") "plan_step_done" else "plan_step_reopened", if (next == "DONE") "Step completed" else "Step reopened", step.title, now)
        }
    }

    suspend fun setStatus(id: String, target: ProblemStatus, note: String = ""): Boolean {
        val current = database.problemDao().getById(id) ?: return false
        if (!ProblemRules.canTransition(ProblemStatus.from(current.status), target)) return false
        val now = System.currentTimeMillis()
        database.withTransaction {
            database.problemDao().updateStatus(id, target.name, note, now)
            addEvent(id, "status_changed", "Status changed to ${target.label}", note, now)
        }
        return true
    }

    /**
     * Reconciles persisted RUNNING workflows after process death without replaying actions.
     * An in-flight step is ambiguous and requires review; a run interrupted between steps pauses.
     */
    suspend fun recoverInterruptedWorkflows(): Int = database.withTransaction {
        val interruptedRuns = database.workflowRunDao().getAll().filter {
            WorkflowRunStatus.from(it.status) == WorkflowRunStatus.RUNNING
        }
        var recovered = 0
        val now = System.currentTimeMillis()
        interruptedRuns.forEach { run ->
            val activeSteps = database.workflowStepDao().getForRun(run.id).filter {
                WorkflowStepStatus.from(it.status) == WorkflowStepStatus.RUNNING
            }
            if (activeSteps.isNotEmpty()) {
                val reason = "App process stopped while this step was running. Confirm whether its action completed before retrying."
                activeSteps.forEach { step ->
                    database.workflowStepDao().update(step.copy(
                        status = WorkflowStepStatus.NEEDS_REVIEW.name,
                        approvedPayloadHash = null,
                        approvedAt = null,
                        lastError = reason,
                        updatedAt = now,
                        finishedAt = null
                    ))
                    addWorkflowEvent(run.id, step.id, "step_interrupted_needs_review",
                        WorkflowStepStatus.RUNNING.name, WorkflowStepStatus.NEEDS_REVIEW.name,
                        "Interrupted step requires review", reason, now)
                }
                database.workflowRunDao().update(run.copy(
                    status = WorkflowRunStatus.NEEDS_REVIEW.name,
                    updatedAt = now,
                    finishedAt = null,
                    lastError = reason
                ))
                addWorkflowEvent(run.id, null, "run_interrupted_needs_review",
                    WorkflowRunStatus.RUNNING.name, WorkflowRunStatus.NEEDS_REVIEW.name,
                    "Interrupted workflow requires review", reason, now)
            } else {
                val reason = "App process stopped between workflow steps. Review saved state before resuming."
                database.workflowRunDao().update(run.copy(
                    status = WorkflowRunStatus.PAUSED.name,
                    updatedAt = now,
                    finishedAt = null,
                    lastError = reason
                ))
                addWorkflowEvent(run.id, null, "run_interrupted_paused",
                    WorkflowRunStatus.RUNNING.name, WorkflowRunStatus.PAUSED.name,
                    "Interrupted workflow paused safely", reason, now)
            }
            recovered++
        }
        recovered
    }

    suspend fun createWorkflow(
        problemId: String,
        title: String,
        steps: List<WorkflowStepDraft>,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): String? {
        val cleanTitle = title.trim().take(180)
        val cleanSteps = steps.mapNotNull { draft ->
            val cleanStepTitle = draft.title.trim().take(180)
            if (cleanStepTitle.isBlank()) null else draft.copy(
                title = cleanStepTitle,
                capabilityId = draft.capabilityId.trim().ifBlank { "manual.task" }.take(120),
                inputJson = draft.inputJson.take(20_000),
                expectedPostcondition = draft.expectedPostcondition.trim().take(2_000),
                retrySafe = CapabilityRegistry.normalizedRetrySafe(draft.capabilityId, draft.retrySafe),
                requiresApproval = CapabilityRegistry.requiresApproval(draft.capabilityId, draft.requiresApproval)
            )
        }
        val key = idempotencyKey.trim().take(180)
        if (cleanTitle.isBlank() || cleanSteps.isEmpty() || key.isBlank()) return null

        return database.withTransaction {
            if (database.problemDao().getById(problemId) == null) return@withTransaction null

            val existing = database.workflowRunDao().getByIdempotencyKey(key)
            if (existing != null) {
                val existingSteps = database.workflowStepDao().getForRun(existing.id)
                val sameDefinition = existing.problemId == problemId &&
                    existing.title == cleanTitle &&
                    existingSteps.map {
                        WorkflowStepDraft(it.title, it.capabilityId, it.inputJson, it.expectedPostcondition, it.retrySafe, it.requiresApproval)
                    } == cleanSteps
                check(sameDefinition) { "Idempotency key was already used for a different workflow definition" }
                return@withTransaction existing.id
            }

            val now = System.currentTimeMillis()
            val runId = UUID.randomUUID().toString()
            database.workflowRunDao().insert(
                WorkflowRunEntity(
                    id = runId,
                    problemId = problemId,
                    title = cleanTitle,
                    status = WorkflowRunStatus.DRAFT.name,
                    idempotencyKey = key,
                    createdAt = now,
                    updatedAt = now,
                    startedAt = null,
                    finishedAt = null,
                    lastError = null
                )
            )
            cleanSteps.forEachIndexed { position, draft ->
                val payloadHash = payloadHash(draft.capabilityId, draft.inputJson)
                database.workflowStepDao().insert(
                    WorkflowStepEntity(
                        id = UUID.randomUUID().toString(),
                        workflowRunId = runId,
                        title = draft.title,
                        capabilityId = draft.capabilityId,
                        inputJson = draft.inputJson,
                        expectedPostcondition = draft.expectedPostcondition,
                        status = WorkflowStepStatus.PENDING.name,
                        position = position,
                        retrySafe = draft.retrySafe,
                        requiresApproval = draft.requiresApproval,
                        payloadHash = payloadHash,
                        approvedPayloadHash = null,
                        approvedAt = null,
                        attemptCount = 0,
                        lastError = null,
                        createdAt = now,
                        updatedAt = now,
                        startedAt = null,
                        finishedAt = null
                    )
                )
            }
            addWorkflowEvent(runId, null, "workflow_created", null, WorkflowRunStatus.DRAFT.name, cleanTitle, "Workflow plan captured", now)
            database.problemDao().touch(problemId, now)
            addEvent(problemId, "workflow_created", "Workflow created", cleanTitle, now)
            runId
        }
    }

    suspend fun approveWorkflowStep(stepId: String, details: String = ""): Boolean =
        database.withTransaction {
            val step = database.workflowStepDao().getById(stepId) ?: return@withTransaction false
            if (!step.requiresApproval) return@withTransaction false
            if (WorkflowStepStatus.from(step.status) !in setOf(WorkflowStepStatus.PENDING, WorkflowStepStatus.WAITING_FOR_APPROVAL)) {
                return@withTransaction false
            }
            if (step.approvedPayloadHash == step.payloadHash && step.approvedAt != null) return@withTransaction true

            val now = System.currentTimeMillis()
            database.workflowStepDao().update(step.copy(
                approvedPayloadHash = step.payloadHash,
                approvedAt = now,
                updatedAt = now
            ))
            addWorkflowEvent(step.workflowRunId, step.id, "approval_granted", step.status, step.status, "Exact step payload approved", details, now)
            true
        }

    suspend fun updateWorkflowStepInput(stepId: String, inputJson: String): Boolean =
        database.withTransaction {
            val step = database.workflowStepDao().getById(stepId) ?: return@withTransaction false
            if (WorkflowStepStatus.from(step.status) != WorkflowStepStatus.PENDING) return@withTransaction false
            val nextInput = inputJson.take(20_000)
            if (nextInput == step.inputJson) return@withTransaction true
            val now = System.currentTimeMillis()
            val nextHash = payloadHash(step.capabilityId, nextInput)
            database.workflowStepDao().update(step.copy(
                inputJson = nextInput,
                payloadHash = nextHash,
                approvedPayloadHash = null,
                approvedAt = null,
                updatedAt = now
            ))
            addWorkflowEvent(step.workflowRunId, step.id, "step_input_changed", step.status, step.status, "Step input changed; prior approval invalidated", "", now)
            true
        }

    suspend fun transitionWorkflowRun(
        runId: String,
        target: WorkflowRunStatus,
        approvalGranted: Boolean = false,
        reviewResolved: Boolean = false,
        retryAuthorized: Boolean = false,
        outcomeVerified: Boolean = false,
        details: String = ""
    ): Boolean = database.withTransaction {
        val run = database.workflowRunDao().getById(runId) ?: return@withTransaction false
        val from = WorkflowRunStatus.from(run.status)
        if (from == target) return@withTransaction true
        val steps = database.workflowStepDao().getForRun(runId)
        val stepStatuses = steps.map { WorkflowStepStatus.from(it.status) }

        if (target == WorkflowRunStatus.VERIFIED && details.isBlank()) return@withTransaction false
        if (target == WorkflowRunStatus.RUNNING) {
            val hasExecutableWork = steps.any { step ->
                val stepStatus = WorkflowStepStatus.from(step.status)
                val capabilityAvailable = CapabilityRegistry.assess(step.capabilityId).canExecute
                capabilityAvailable && stepStatus in setOf(
                    WorkflowStepStatus.PENDING,
                    WorkflowStepStatus.RUNNING,
                    WorkflowStepStatus.WAITING_FOR_APPROVAL
                )
            }
            if (!hasExecutableWork) return@withTransaction false
        }
        if (from == WorkflowRunStatus.WAITING_FOR_APPROVAL && target == WorkflowRunStatus.RUNNING) {
            val waiting = steps.filter { WorkflowStepStatus.from(it.status) == WorkflowStepStatus.WAITING_FOR_APPROVAL }
            if (waiting.isEmpty() || waiting.any {
                    !it.requiresApproval || it.approvedPayloadHash != it.payloadHash || it.approvedAt == null
                }) return@withTransaction false
        }
        if (!WorkflowRules.canTransitionRun(
                from, target,
                approvalGranted = approvalGranted,
                reviewResolved = reviewResolved,
                retryAuthorized = retryAuthorized,
                outcomeVerified = outcomeVerified,
                steps = stepStatuses
            )
        ) return@withTransaction false

        val runningStepsAtCancellation = if (target == WorkflowRunStatus.CANCELLED) {
            steps.filter { WorkflowStepStatus.from(it.status) == WorkflowStepStatus.RUNNING }
        } else emptyList()
        // Cancelling while a step is active cannot prove that its side effect stopped.
        // Preserve that uncertainty for review and cancel only the remaining work.
        val effectiveTarget = if (runningStepsAtCancellation.isNotEmpty()) {
            WorkflowRunStatus.NEEDS_REVIEW
        } else target
        val cancellationDetails = if (runningStepsAtCancellation.isNotEmpty()) {
            "Workflow cancellation requested while a step was running. The active step may already have acted; verify its outcome. Remaining steps were cancelled."
        } else details

        val now = System.currentTimeMillis()
        val finishedAt = when (effectiveTarget) {
            WorkflowRunStatus.VERIFIED, WorkflowRunStatus.CANCELLED, WorkflowRunStatus.FAILED -> now
            WorkflowRunStatus.READY, WorkflowRunStatus.RUNNING -> null
            else -> run.finishedAt
        }
        val lastError = when (effectiveTarget) {
            WorkflowRunStatus.FAILED, WorkflowRunStatus.NEEDS_REVIEW -> cancellationDetails.takeIf { it.isNotBlank() } ?: run.lastError
            WorkflowRunStatus.READY, WorkflowRunStatus.RUNNING, WorkflowRunStatus.VERIFIED -> null
            else -> run.lastError
        }
        database.workflowRunDao().update(run.copy(
            status = effectiveTarget.name,
            updatedAt = now,
            startedAt = if (effectiveTarget == WorkflowRunStatus.RUNNING && run.startedAt == null) now else run.startedAt,
            finishedAt = finishedAt,
            lastError = lastError
        ))
        addWorkflowEvent(runId, null, "run_status_changed", from.name, effectiveTarget.name, "Workflow status changed", cancellationDetails, now)

        if (from == WorkflowRunStatus.WAITING_FOR_APPROVAL && effectiveTarget == WorkflowRunStatus.RUNNING) {
            steps.filter { WorkflowStepStatus.from(it.status) == WorkflowStepStatus.WAITING_FOR_APPROVAL }.forEach { step ->
                database.workflowStepDao().update(step.copy(
                    status = WorkflowStepStatus.RUNNING.name,
                    updatedAt = now,
                    lastError = null
                ))
                addWorkflowEvent(runId, step.id, "step_resumed_after_approval", step.status, WorkflowStepStatus.RUNNING.name, "Approved step resumed", "", now)
            }
        } else if (target == WorkflowRunStatus.CANCELLED) {
            steps.filter { !WorkflowRules.isTerminal(WorkflowStepStatus.from(it.status)) }.forEach { step ->
                val previous = WorkflowStepStatus.from(step.status)
                val next = if (previous == WorkflowStepStatus.RUNNING) {
                    WorkflowStepStatus.NEEDS_REVIEW
                } else WorkflowStepStatus.CANCELLED
                if (WorkflowRules.canTransitionStep(previous, next, reviewResolved = false)) {
                    database.workflowStepDao().update(step.copy(
                        status = next.name,
                        updatedAt = now,
                        finishedAt = if (next == WorkflowStepStatus.CANCELLED) now else null,
                        lastError = if (next == WorkflowStepStatus.NEEDS_REVIEW) cancellationDetails else null,
                        approvedPayloadHash = if (next == WorkflowStepStatus.NEEDS_REVIEW) null else step.approvedPayloadHash,
                        approvedAt = if (next == WorkflowStepStatus.NEEDS_REVIEW) null else step.approvedAt
                    ))
                    val eventType = if (next == WorkflowStepStatus.NEEDS_REVIEW) "step_cancelled_while_running_needs_review" else "step_cancelled"
                    val summary = if (next == WorkflowStepStatus.NEEDS_REVIEW) "Cancellation left an active step uncertain" else "Step cancelled with workflow"
                    addWorkflowEvent(runId, step.id, eventType, previous.name, next.name, summary, cancellationDetails, now)
                }
            }
        }
        true
    }

    suspend fun transitionWorkflowStep(
        stepId: String,
        target: WorkflowStepStatus,
        reviewResolved: Boolean = false,
        errorMessage: String = "",
        details: String = ""
    ): Boolean = database.withTransaction {
        val step = database.workflowStepDao().getById(stepId) ?: return@withTransaction false
        val run = database.workflowRunDao().getById(step.workflowRunId) ?: return@withTransaction false
        val runStatus = WorkflowRunStatus.from(run.status)
        val from = WorkflowStepStatus.from(step.status)
        if (from == target) return@withTransaction true

        val allowedRunState = runStatus == WorkflowRunStatus.RUNNING ||
            (runStatus == WorkflowRunStatus.READY && target == WorkflowStepStatus.PENDING)
        if (!allowedRunState) return@withTransaction false
        if (target == WorkflowStepStatus.WAITING_FOR_APPROVAL && !step.requiresApproval) return@withTransaction false
        if (target == WorkflowStepStatus.RUNNING) {
            val capability = CapabilityRegistry.assess(step.capabilityId)
            if (!capability.canExecute) return@withTransaction false
            if (capability.requiresApproval && !step.requiresApproval) return@withTransaction false
            if (capability.retryPolicy == com.yusufculcu.trader.domain.RetryPolicy.NEVER && step.retrySafe) {
                return@withTransaction false
            }
            if (step.requiresApproval && (step.approvedPayloadHash != step.payloadHash || step.approvedAt == null)) {
                return@withTransaction false
            }
        }
        if (!WorkflowRules.canTransitionStep(
                from, target,
                retrySafe = step.retrySafe,
                reviewResolved = reviewResolved
            )
        ) return@withTransaction false

        val now = System.currentTimeMillis()
        val nextStep = step.copy(
            status = target.name,
            attemptCount = if (from == WorkflowStepStatus.PENDING && target == WorkflowStepStatus.RUNNING) step.attemptCount + 1 else step.attemptCount,
            lastError = if (target == WorkflowStepStatus.FAILED || target == WorkflowStepStatus.NEEDS_REVIEW) errorMessage.takeIf { it.isNotBlank() } else null,
            approvedPayloadHash = if (target == WorkflowStepStatus.FAILED || target == WorkflowStepStatus.NEEDS_REVIEW) null else step.approvedPayloadHash,
            approvedAt = if (target == WorkflowStepStatus.FAILED || target == WorkflowStepStatus.NEEDS_REVIEW) null else step.approvedAt,
            updatedAt = now,
            startedAt = if (target == WorkflowStepStatus.RUNNING && step.startedAt == null) now else step.startedAt,
            finishedAt = if (target == WorkflowStepStatus.COMPLETED || target == WorkflowStepStatus.SKIPPED || target == WorkflowStepStatus.CANCELLED) now else null
        )
        database.workflowStepDao().update(nextStep)
        addWorkflowEvent(step.workflowRunId, step.id, "step_status_changed", from.name, target.name, "Workflow step status changed", details.ifBlank { errorMessage }, now)

        val runTarget = when (target) {
            WorkflowStepStatus.WAITING_FOR_APPROVAL -> WorkflowRunStatus.WAITING_FOR_APPROVAL
            WorkflowStepStatus.NEEDS_REVIEW -> WorkflowRunStatus.NEEDS_REVIEW
            WorkflowStepStatus.FAILED -> WorkflowRunStatus.FAILED
            else -> null
        }
        if (runTarget != null && runStatus == WorkflowRunStatus.RUNNING &&
            WorkflowRules.canTransitionRun(runStatus, runTarget)
        ) {
            database.workflowRunDao().update(run.copy(
                status = runTarget.name,
                updatedAt = now,
                finishedAt = if (runTarget == WorkflowRunStatus.FAILED) now else null,
                lastError = if (runTarget == WorkflowRunStatus.FAILED || runTarget == WorkflowRunStatus.NEEDS_REVIEW) errorMessage.takeIf { it.isNotBlank() } else null
            ))
            addWorkflowEvent(run.id, null, "run_status_changed", runStatus.name, runTarget.name, "Workflow paused for a gate or failure", errorMessage, now)
        } else if (target == WorkflowStepStatus.COMPLETED || target == WorkflowStepStatus.SKIPPED) {
            val currentStatuses = database.workflowStepDao().getForRun(run.id).map { WorkflowStepStatus.from(it.status) }
            if (runStatus == WorkflowRunStatus.RUNNING && WorkflowRules.canAwaitVerification(currentStatuses)) {
                database.workflowRunDao().update(run.copy(
                    status = WorkflowRunStatus.AWAITING_VERIFICATION.name,
                    updatedAt = now,
                    finishedAt = null
                ))
                addWorkflowEvent(run.id, null, "awaiting_verification", runStatus.name, WorkflowRunStatus.AWAITING_VERIFICATION.name, "All workflow steps finished; outcome verification is required", "", now)
            }
        }
        true
    }

    private suspend fun addEvent(problemId: String, type: String, summary: String, details: String, at: Long) {
        database.activityDao().insert(ActivityEventEntity(UUID.randomUUID().toString(), problemId, type, summary, details, at))
    }

    private suspend fun addWorkflowEvent(
        runId: String,
        stepId: String?,
        type: String,
        fromStatus: String?,
        toStatus: String?,
        summary: String,
        details: String,
        at: Long
    ) {
        database.workflowEventDao().insert(
            WorkflowEventEntity(UUID.randomUUID().toString(), runId, stepId, type, fromStatus, toStatus, summary, details, at)
        )
    }

    private fun payloadHash(capabilityId: String, inputJson: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest("$capabilityId\n$inputJson".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
