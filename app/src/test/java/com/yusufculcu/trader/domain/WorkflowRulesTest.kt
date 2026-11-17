package com.yusufculcu.trader.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkflowRulesTest {
    @Test
    fun runCanMoveThroughReadyRunningAndPauseResume() {
        assertTrue(WorkflowRules.canTransitionRun(WorkflowRunStatus.DRAFT, WorkflowRunStatus.READY))
        assertTrue(WorkflowRules.canTransitionRun(WorkflowRunStatus.READY, WorkflowRunStatus.RUNNING))
        assertTrue(WorkflowRules.canTransitionRun(WorkflowRunStatus.RUNNING, WorkflowRunStatus.PAUSED))
        assertTrue(WorkflowRules.canTransitionRun(WorkflowRunStatus.PAUSED, WorkflowRunStatus.RUNNING))
    }

    @Test
    fun runCannotSkipRequiredApproval() {
        assertFalse(WorkflowRules.canTransitionRun(
            WorkflowRunStatus.WAITING_FOR_APPROVAL,
            WorkflowRunStatus.RUNNING
        ))
        assertTrue(WorkflowRules.canTransitionRun(
            WorkflowRunStatus.WAITING_FOR_APPROVAL,
            WorkflowRunStatus.RUNNING,
            approvalGranted = true
        ))
    }

    @Test
    fun failedRunRequiresExplicitRetryAuthorization() {
        assertFalse(WorkflowRules.canTransitionRun(WorkflowRunStatus.FAILED, WorkflowRunStatus.READY))
        assertTrue(WorkflowRules.canTransitionRun(
            WorkflowRunStatus.FAILED,
            WorkflowRunStatus.READY,
            retryAuthorized = true
        ))
    }

    @Test
    fun reviewRunRequiresResolutionBeforeReturningToReady() {
        assertFalse(WorkflowRules.canTransitionRun(WorkflowRunStatus.NEEDS_REVIEW, WorkflowRunStatus.READY))
        assertTrue(WorkflowRules.canTransitionRun(
            WorkflowRunStatus.NEEDS_REVIEW,
            WorkflowRunStatus.READY,
            reviewResolved = true
        ))
    }

    @Test
    fun runningRunCannotEnterVerificationWhileStepsRemain() {
        assertFalse(WorkflowRules.canTransitionRun(
            WorkflowRunStatus.RUNNING,
            WorkflowRunStatus.AWAITING_VERIFICATION,
            steps = listOf(WorkflowStepStatus.COMPLETED, WorkflowStepStatus.PENDING)
        ))
        assertTrue(WorkflowRules.canTransitionRun(
            WorkflowRunStatus.RUNNING,
            WorkflowRunStatus.AWAITING_VERIFICATION,
            steps = listOf(WorkflowStepStatus.COMPLETED, WorkflowStepStatus.SKIPPED)
        ))
    }

    @Test
    fun verificationRequiresOutcomeEvidence() {
        val successfulSteps = listOf(WorkflowStepStatus.COMPLETED, WorkflowStepStatus.SKIPPED)
        assertFalse(WorkflowRules.canVerifyOutcome(successfulSteps, outcomeEvidenceConfirmed = false))
        assertTrue(WorkflowRules.canVerifyOutcome(successfulSteps, outcomeEvidenceConfirmed = true))
        assertFalse(WorkflowRules.canVerifyOutcome(emptyList(), outcomeEvidenceConfirmed = true))
    }

    @Test
    fun verifiedAndCancelledRunsAreTerminal() {
        assertTrue(WorkflowRules.isTerminal(WorkflowRunStatus.VERIFIED))
        assertTrue(WorkflowRules.isTerminal(WorkflowRunStatus.CANCELLED))
        assertFalse(WorkflowRules.canTransitionRun(WorkflowRunStatus.VERIFIED, WorkflowRunStatus.RUNNING))
        assertFalse(WorkflowRules.canTransitionRun(WorkflowRunStatus.CANCELLED, WorkflowRunStatus.READY))
    }

    @Test
    fun approvalStepCannotResumeWithoutApproval() {
        assertFalse(WorkflowRules.canTransitionStep(
            WorkflowStepStatus.WAITING_FOR_APPROVAL,
            WorkflowStepStatus.RUNNING
        ))
        assertTrue(WorkflowRules.canTransitionStep(
            WorkflowStepStatus.WAITING_FOR_APPROVAL,
            WorkflowStepStatus.RUNNING,
            approvalGranted = true
        ))
    }

    @Test
    fun failedStepCanOnlyRetryWhenDeclaredSafe() {
        assertFalse(WorkflowRules.canTransitionStep(
            WorkflowStepStatus.FAILED,
            WorkflowStepStatus.PENDING
        ))
        assertTrue(WorkflowRules.canTransitionStep(
            WorkflowStepStatus.FAILED,
            WorkflowStepStatus.PENDING,
            retrySafe = true
        ))
    }

    @Test
    fun ambiguousStepMustBeReviewedBeforeRetry() {
        assertFalse(WorkflowRules.canTransitionStep(
            WorkflowStepStatus.NEEDS_REVIEW,
            WorkflowStepStatus.PENDING
        ))
        assertTrue(WorkflowRules.canTransitionStep(
            WorkflowStepStatus.NEEDS_REVIEW,
            WorkflowStepStatus.PENDING,
            reviewResolved = true
        ))
    }

    @Test
    fun successfulStepStatesAreTerminal() {
        assertTrue(WorkflowRules.isTerminal(WorkflowStepStatus.COMPLETED))
        assertTrue(WorkflowRules.isTerminal(WorkflowStepStatus.SKIPPED))
        assertFalse(WorkflowRules.canTransitionStep(
            WorkflowStepStatus.COMPLETED,
            WorkflowStepStatus.RUNNING
        ))
    }

    @Test
    fun sameStateTransitionsAreIdempotent() {
        assertTrue(WorkflowRules.canTransitionRun(WorkflowRunStatus.RUNNING, WorkflowRunStatus.RUNNING))
        assertTrue(WorkflowRules.canTransitionStep(WorkflowStepStatus.PENDING, WorkflowStepStatus.PENDING))
    }
}
