package com.yusufculcu.trader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yusufculcu.trader.data.PlanStepEntity
import com.yusufculcu.trader.data.ProblemEntity
import com.yusufculcu.trader.data.TraderRepository
import com.yusufculcu.trader.data.WorkflowStepDraft
import com.yusufculcu.trader.domain.WorkflowRunStatus
import com.yusufculcu.trader.domain.WorkflowStepStatus
import java.util.UUID
import com.yusufculcu.trader.domain.ProblemStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TraderViewModel(private val repository: TraderRepository) : ViewModel() {
    val problems = repository.observeProblems().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val activity = repository.observeActivity().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val problemCount = repository.observeCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val workflowRuns = repository.observeAllWorkflowRuns().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun stepsFor(problemId: String) = repository.observeSteps(problemId)
    fun workflowsFor(problemId: String) = repository.observeWorkflowRuns(problemId)
    fun workflowStepsFor(runId: String) = repository.observeWorkflowSteps(runId)
    fun workflowEventsFor(runId: String) = repository.observeWorkflowEvents(runId)

    fun createProblem(title: String, description: String, outcome: String, constraints: String) {
        viewModelScope.launch { repository.createProblem(title, description, outcome, constraints) }
    }

    fun captureSharedContent(content: String) {
        val isUrl = content.startsWith("https://") || content.startsWith("http://")
        val title = if (isUrl) content.removePrefix("https://").removePrefix("http://").substringBefore('/').ifBlank { "Shared link" }
        else content.lineSequence().firstOrNull().orEmpty().take(100)
        viewModelScope.launch {
            repository.createProblem(
                title = title.ifBlank { "Shared content" },
                description = content,
                desiredOutcome = "Review and decide what to do with this",
                category = "Captured",
                inputKind = if (isUrl) "shared_url" else "shared_text",
                originalContent = content,
                sourceUri = if (isUrl) content else null
            )
        }
    }

    fun addStep(problemId: String, title: String) { viewModelScope.launch { repository.addStep(problemId, title) } }
    fun toggleStep(step: PlanStepEntity) { viewModelScope.launch { repository.toggleStep(step) } }
    fun createWorkflowFromPlan(problem: ProblemEntity, planSteps: List<PlanStepEntity>, onCreated: (String?) -> Unit) {
        if (planSteps.isEmpty()) {
            onCreated(null)
            return
        }
        viewModelScope.launch {
            val drafts = planSteps.map { step ->
                WorkflowStepDraft(
                    title = step.title,
                    capabilityId = step.requiredCapability.ifBlank { "manual.task" },
                    inputJson = "{}",
                    expectedPostcondition = step.expectedResult.ifBlank { "User confirms this step is complete" }
                )
            }
            val id = repository.createWorkflow(
                problemId = problem.id,
                title = "Plan: ${problem.title}",
                steps = drafts,
                idempotencyKey = UUID.randomUUID().toString()
            )
            onCreated(id)
        }
    }

    fun prepareWorkflow(runId: String) {
        viewModelScope.launch { repository.transitionWorkflowRun(runId, WorkflowRunStatus.READY) }
    }

    fun startWorkflow(runId: String) {
        viewModelScope.launch { repository.transitionWorkflowRun(runId, WorkflowRunStatus.RUNNING) }
    }

    fun pauseWorkflow(runId: String) {
        viewModelScope.launch { repository.transitionWorkflowRun(runId, WorkflowRunStatus.PAUSED) }
    }

    fun resumeWorkflow(runId: String) {
        viewModelScope.launch {
            val run = repository.getWorkflowRun(runId) ?: return@launch
            val waitingForApproval = WorkflowRunStatus.from(run.status) == WorkflowRunStatus.WAITING_FOR_APPROVAL
            repository.transitionWorkflowRun(runId, WorkflowRunStatus.RUNNING, approvalGranted = waitingForApproval)
        }
    }

    fun cancelWorkflow(runId: String) {
        viewModelScope.launch { repository.transitionWorkflowRun(runId, WorkflowRunStatus.CANCELLED, details = "Cancelled by user") }
    }

    fun approveWorkflowStep(stepId: String) {
        viewModelScope.launch { repository.approveWorkflowStep(stepId, "User approved the exact step payload in Trader") }
    }

    fun startWorkflowStep(stepId: String) {
        viewModelScope.launch {
            val step = repository.getWorkflowStep(stepId) ?: return@launch
            val status = WorkflowStepStatus.from(step.status)
            if (status == WorkflowStepStatus.WAITING_FOR_APPROVAL) {
                repository.approveWorkflowStep(stepId, "User approved the exact step payload in Trader")
            } else {
                if (step.requiresApproval) repository.approveWorkflowStep(stepId, "User approved the exact step payload in Trader")
                repository.transitionWorkflowStep(stepId, WorkflowStepStatus.RUNNING, details = "Started by user")
            }
        }
    }

    fun completeWorkflowStep(stepId: String) {
        viewModelScope.launch { repository.transitionWorkflowStep(stepId, WorkflowStepStatus.COMPLETED, details = "User confirmed step completion") }
    }

    fun flagWorkflowStepForReview(stepId: String, note: String) {
        viewModelScope.launch {
            repository.transitionWorkflowStep(stepId, WorkflowStepStatus.NEEDS_REVIEW, errorMessage = note, details = note)
        }
    }

    fun retryWorkflow(runId: String) {
        viewModelScope.launch {
            val failedSteps = repository.getWorkflowSteps(runId)
                .filter { WorkflowStepStatus.from(it.status) == WorkflowStepStatus.FAILED }
            if (failedSteps.isEmpty() || failedSteps.any { !it.retrySafe }) return@launch
            if (repository.transitionWorkflowRun(runId, WorkflowRunStatus.READY, retryAuthorized = true, details = "User authorized safe retry")) {
                failedSteps.forEach { repository.transitionWorkflowStep(it.id, WorkflowStepStatus.PENDING, details = "Safe retry authorized") }
            }
        }
    }

    fun resolveWorkflowReview(runId: String, note: String) {
        viewModelScope.launch {
            if (repository.transitionWorkflowRun(runId, WorkflowRunStatus.READY, reviewResolved = true, details = note)) {
                repository.getWorkflowSteps(runId)
                    .filter { WorkflowStepStatus.from(it.status) == WorkflowStepStatus.NEEDS_REVIEW }
                    .forEach { repository.transitionWorkflowStep(it.id, WorkflowStepStatus.PENDING, reviewResolved = true, details = note) }
            }
        }
    }

    fun verifyWorkflow(runId: String, note: String) {
        viewModelScope.launch {
            repository.transitionWorkflowRun(runId, WorkflowRunStatus.VERIFIED, outcomeVerified = true, details = note)
        }
    }

    fun setStatus(problem: ProblemEntity, target: ProblemStatus, note: String = "") {
        viewModelScope.launch { repository.setStatus(problem.id, target, note) }
    }
}

class TraderViewModelFactory(private val repository: TraderRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TraderViewModel::class.java)) return TraderViewModel(repository) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
