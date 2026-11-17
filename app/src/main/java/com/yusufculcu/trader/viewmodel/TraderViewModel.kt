package com.yusufculcu.trader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yusufculcu.trader.data.PlanStepEntity
import com.yusufculcu.trader.data.ProblemEntity
import com.yusufculcu.trader.data.TraderRepository
import com.yusufculcu.trader.domain.ProblemStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TraderViewModel(private val repository: TraderRepository) : ViewModel() {
    val problems = repository.observeProblems().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val activity = repository.observeActivity().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val problemCount = repository.observeCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    fun stepsFor(problemId: String) = repository.observeSteps(problemId)

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
