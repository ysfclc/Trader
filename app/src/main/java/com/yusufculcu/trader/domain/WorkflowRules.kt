package com.yusufculcu.trader.domain

/**
 * Durable workflow lifecycle vocabulary and transition rules.
 *
 * These rules deliberately do not execute capabilities. They define which
 * state changes are legal so persistence and executors can share one contract.
 * Ambiguous external side effects must enter NEEDS_REVIEW, never be retried blindly.
 */
enum class WorkflowRunStatus {
    DRAFT,
    READY,
    RUNNING,
    PAUSED,
    WAITING_FOR_APPROVAL,
    NEEDS_REVIEW,
    FAILED,
    AWAITING_VERIFICATION,
    VERIFIED,
    CANCELLED;

    companion object {
        fun from(value: String): WorkflowRunStatus =
            entries.firstOrNull { it.name == value } ?: NEEDS_REVIEW
    }
}

enum class WorkflowStepStatus {
    PENDING,
    RUNNING,
    WAITING_FOR_APPROVAL,
    COMPLETED,
    FAILED,
    NEEDS_REVIEW,
    SKIPPED,
    CANCELLED;

    companion object {
        fun from(value: String): WorkflowStepStatus =
            entries.firstOrNull { it.name == value } ?: NEEDS_REVIEW
    }
}

object WorkflowRules {
    fun canTransitionRun(
        from: WorkflowRunStatus,
        to: WorkflowRunStatus,
        approvalGranted: Boolean = false,
        reviewResolved: Boolean = false,
        retryAuthorized: Boolean = false,
        outcomeVerified: Boolean = false,
        steps: List<WorkflowStepStatus> = emptyList()
    ): Boolean {
        if (from == to) return true

        return when (from) {
            WorkflowRunStatus.DRAFT ->
                to == WorkflowRunStatus.READY || to == WorkflowRunStatus.CANCELLED

            WorkflowRunStatus.READY ->
                to == WorkflowRunStatus.RUNNING || to == WorkflowRunStatus.CANCELLED

            WorkflowRunStatus.RUNNING ->
                to == WorkflowRunStatus.PAUSED ||
                    to == WorkflowRunStatus.WAITING_FOR_APPROVAL ||
                    to == WorkflowRunStatus.NEEDS_REVIEW ||
                    to == WorkflowRunStatus.FAILED ||
                    to == WorkflowRunStatus.CANCELLED ||
                    (to == WorkflowRunStatus.AWAITING_VERIFICATION && canAwaitVerification(steps))

            WorkflowRunStatus.PAUSED ->
                to == WorkflowRunStatus.RUNNING || to == WorkflowRunStatus.CANCELLED

            WorkflowRunStatus.WAITING_FOR_APPROVAL ->
                (to == WorkflowRunStatus.RUNNING && approvalGranted) ||
                    to == WorkflowRunStatus.NEEDS_REVIEW ||
                    to == WorkflowRunStatus.CANCELLED

            WorkflowRunStatus.NEEDS_REVIEW ->
                (to == WorkflowRunStatus.READY && reviewResolved) ||
                    to == WorkflowRunStatus.CANCELLED

            WorkflowRunStatus.FAILED ->
                (to == WorkflowRunStatus.READY && retryAuthorized) ||
                    to == WorkflowRunStatus.CANCELLED

            WorkflowRunStatus.AWAITING_VERIFICATION ->
                (to == WorkflowRunStatus.VERIFIED && outcomeVerified) ||
                    to == WorkflowRunStatus.NEEDS_REVIEW ||
                    to == WorkflowRunStatus.CANCELLED

            WorkflowRunStatus.VERIFIED,
            WorkflowRunStatus.CANCELLED -> false
        }
    }

    fun canTransitionStep(
        from: WorkflowStepStatus,
        to: WorkflowStepStatus,
        approvalGranted: Boolean = false,
        retrySafe: Boolean = false,
        reviewResolved: Boolean = false
    ): Boolean {
        if (from == to) return true

        return when (from) {
            WorkflowStepStatus.PENDING ->
                to == WorkflowStepStatus.RUNNING ||
                    to == WorkflowStepStatus.SKIPPED ||
                    to == WorkflowStepStatus.CANCELLED

            WorkflowStepStatus.RUNNING ->
                to == WorkflowStepStatus.COMPLETED ||
                    to == WorkflowStepStatus.WAITING_FOR_APPROVAL ||
                    to == WorkflowStepStatus.FAILED ||
                    to == WorkflowStepStatus.NEEDS_REVIEW ||
                    to == WorkflowStepStatus.CANCELLED

            WorkflowStepStatus.WAITING_FOR_APPROVAL ->
                (to == WorkflowStepStatus.RUNNING && approvalGranted) ||
                    to == WorkflowStepStatus.NEEDS_REVIEW ||
                    to == WorkflowStepStatus.CANCELLED

            WorkflowStepStatus.FAILED ->
                (to == WorkflowStepStatus.PENDING && retrySafe) ||
                    to == WorkflowStepStatus.NEEDS_REVIEW ||
                    to == WorkflowStepStatus.CANCELLED

            WorkflowStepStatus.NEEDS_REVIEW ->
                (to == WorkflowStepStatus.PENDING && reviewResolved) ||
                    to == WorkflowStepStatus.CANCELLED

            WorkflowStepStatus.COMPLETED,
            WorkflowStepStatus.SKIPPED,
            WorkflowStepStatus.CANCELLED -> false
        }
    }

    fun canAwaitVerification(steps: List<WorkflowStepStatus>): Boolean =
        steps.isNotEmpty() && steps.all {
            it == WorkflowStepStatus.COMPLETED || it == WorkflowStepStatus.SKIPPED
        }

    fun canVerifyOutcome(
        steps: List<WorkflowStepStatus>,
        outcomeEvidenceConfirmed: Boolean
    ): Boolean = outcomeEvidenceConfirmed && canAwaitVerification(steps)

    fun isTerminal(status: WorkflowRunStatus): Boolean =
        status == WorkflowRunStatus.VERIFIED || status == WorkflowRunStatus.CANCELLED

    fun isTerminal(status: WorkflowStepStatus): Boolean =
        status == WorkflowStepStatus.COMPLETED ||
            status == WorkflowStepStatus.SKIPPED ||
            status == WorkflowStepStatus.CANCELLED
}
