package com.yusufculcu.trader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yusufculcu.trader.data.EvidenceEntity
import com.yusufculcu.trader.data.OpportunityDraft
import com.yusufculcu.trader.data.OpportunityListItem
import com.yusufculcu.trader.data.PlanStepEntity
import com.yusufculcu.trader.data.ProblemEntity
import com.yusufculcu.trader.data.TraderRepository
import com.yusufculcu.trader.data.WorkflowStepDraft
import com.yusufculcu.trader.domain.ProblemStatus
import com.yusufculcu.trader.domain.WorkflowRunStatus
import com.yusufculcu.trader.domain.WorkflowStepStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TraderViewModel(private val repository: TraderRepository) : ViewModel() {
    init {
        viewModelScope.launch { repository.recoverInterruptedWorkflows() }
    }

    val problems = repository.observeProblems().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val archivedProblems = repository.observeArchivedProblems().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val activity = repository.observeActivity().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val opportunities = combine(problems, repository.observeOpportunities()) { activeProblems, profiles ->
        profiles.mapNotNull { profile ->
            activeProblems.firstOrNull { it.id == profile.problemId }?.let { problem ->
                OpportunityListItem(problem, profile)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val problemCount = repository.observeCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    fun stepsFor(problemId: String) = repository.observeSteps(problemId)
    fun evidenceFor(problemId: String) = repository.observeEvidence(problemId)
    fun opportunityFor(problemId: String) = repository.observeOpportunity(problemId)
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

    fun updateProblem(
        problemId: String,
        title: String,
        description: String,
        outcome: String,
        constraints: String,
        onSaved: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            onSaved(repository.updateProblemDetails(problemId, title, description, outcome, constraints))
        }
    }

    fun archiveProblem(problemId: String, onSaved: (Boolean) -> Unit = {}) {
        viewModelScope.launch { onSaved(repository.archiveProblem(problemId)) }
    }

    fun restoreProblem(problemId: String, onSaved: (Boolean) -> Unit = {}) {
        viewModelScope.launch { onSaved(repository.restoreProblem(problemId)) }
    }

    fun addEvidence(
        problemId: String,
        title: String,
        content: String,
        sourceUri: String?,
        onSaved: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch { onSaved(repository.addEvidence(problemId, title, content, sourceUri)) }
    }

    fun setEvidenceVerified(evidence: EvidenceEntity, verified: Boolean) {
        viewModelScope.launch { repository.setEvidenceVerification(evidence, verified) }
    }

    fun saveOpportunity(problemId: String, draft: OpportunityDraft, onSaved: (Boolean) -> Unit = {}) {
        viewModelScope.launch { onSaved(repository.saveOpportunity(problemId, draft)) }
    }

    fun addStep(problemId: String, title: String, expectedResult: String = "") {
        viewModelScope.launch { repository.addStep(problemId, title, expectedResult) }
    }
    fun toggleStep(step: PlanStepEntity) { viewModelScope.launch { repository.toggleStep(step) } }
    fun setStatus(problem: ProblemEntity, target: ProblemStatus, note: String = "") {
        viewModelScope.launch { repository.setStatus(problem.id, target, note) }
    }

    fun createWorkflow(problemId: String, title: String, steps: List<WorkflowStepDraft>, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            repository.createWorkflow(problemId, title, steps)?.let(onCreated)
        }
    }

    fun transitionWorkflowRun(
        runId: String,
        target: WorkflowRunStatus,
        outcomeVerified: Boolean = false,
        reviewResolved: Boolean = false,
        details: String = ""
    ) {
        viewModelScope.launch {
            repository.transitionWorkflowRun(
                runId = runId,
                target = target,
                outcomeVerified = outcomeVerified,
                reviewResolved = reviewResolved,
                details = details
            )
        }
    }

    fun transitionWorkflowStep(stepId: String, target: WorkflowStepStatus, details: String = "", reviewResolved: Boolean = false) {
        viewModelScope.launch { repository.transitionWorkflowStep(stepId, target, reviewResolved = reviewResolved, details = details) }
    }

    fun approveWorkflowStep(stepId: String) {
        viewModelScope.launch { repository.approveWorkflowStep(stepId, "Approved by user in workflow review") }
    }
}

class TraderViewModelFactory(private val repository: TraderRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TraderViewModel::class.java)) return TraderViewModel(repository) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
