package com.yusufculcu.trader.data

import androidx.room.withTransaction
import com.yusufculcu.trader.domain.ProblemRules
import com.yusufculcu.trader.domain.ProblemStatus
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class TraderRepository(private val database: TraderDatabase) {
    fun observeProblems(): Flow<List<ProblemEntity>> = database.problemDao().observeAll()
    fun observeSteps(problemId: String): Flow<List<PlanStepEntity>> = database.planStepDao().observeForProblem(problemId)
    fun observeActivity(): Flow<List<ActivityEventEntity>> = database.activityDao().observeRecent()
    fun observeCount(): Flow<Int> = database.problemDao().observeCount()

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

    suspend fun addStep(problemId: String, title: String) {
        val clean = title.trim().take(180)
        if (clean.isBlank()) return
        val now = System.currentTimeMillis()
        database.withTransaction {
            val position = database.planStepDao().nextPosition(problemId)
            database.planStepDao().insert(PlanStepEntity(UUID.randomUUID().toString(), problemId, clean, "", "", "", "TODO", position, now, now))
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

    private suspend fun addEvent(problemId: String, type: String, summary: String, details: String, at: Long) {
        database.activityDao().insert(ActivityEventEntity(UUID.randomUUID().toString(), problemId, type, summary, details, at))
    }
}
