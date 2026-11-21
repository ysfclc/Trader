package com.yusufculcu.trader.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.util.UUID

object DiscoveryRunStatus {
    const val CREATED = "CREATED"
    const val RUNNING = "RUNNING"
    const val COMPLETED = "COMPLETED"
    const val PARTIAL = "PARTIAL"
    const val FAILED = "FAILED"
    const val CANCELLED = "CANCELLED"
}
object SourceRunStatus {
    const val PENDING = "PENDING"
    const val RUNNING = "RUNNING"
    const val SUCCEEDED = "SUCCEEDED"
    const val FAILED = "FAILED"
    const val SKIPPED = "SKIPPED"
    const val CANCELLED = "CANCELLED"
}

data class DiscoveryRunDraft(val strategyId: String?, val objective: String, val createdAt: Long)
data class SourceRunDraft(val sourceId: String, val adapterId: String, val startedAt: Long)

class DiscoveryRunRepository(private val database: TraderDatabase) {
    fun observeRuns(): Flow<List<DiscoveryRunEntity>> = database.discoveryRunDao().observeAll()
    fun observeSources(runId: String): Flow<List<SourceRunEntity>> = database.sourceRunDao().observeForRun(runId)

    suspend fun create(draft: DiscoveryRunDraft): DiscoveryRunEntity {
        require(draft.objective.isNotBlank() && draft.objective.length <= 2000)
        require(draft.strategyId == null || database.discoveryStrategyDao().getById(draft.strategyId) != null)
        val run = DiscoveryRunEntity(
            id = UUID.randomUUID().toString(), strategyId = draft.strategyId,
            objective = draft.objective.trim(), status = DiscoveryRunStatus.CREATED,
            createdAt = draft.createdAt, startedAt = null, finishedAt = null,
            cancellationRequestedAt = null, lastError = null
        )
        database.discoveryRunDao().insert(run)
        return run
    }

    suspend fun start(runId: String, at: Long): Boolean = database.withTransaction {
        val run = database.discoveryRunDao().getById(runId) ?: return@withTransaction false
        if (run.status != DiscoveryRunStatus.CREATED) return@withTransaction false
        database.discoveryRunDao().update(run.copy(status = DiscoveryRunStatus.RUNNING, startedAt = at))
        true
    }

    suspend fun addSource(runId: String, draft: SourceRunDraft): SourceRunEntity {
        require(draft.sourceId.isNotBlank() && draft.adapterId.isNotBlank())
        val parent = database.discoveryRunDao().getById(runId) ?: error("Discovery run does not exist.")
        require(parent.status == DiscoveryRunStatus.CREATED || parent.status == DiscoveryRunStatus.RUNNING)
        val row = SourceRunEntity(
            id = UUID.randomUUID().toString(), discoveryRunId = runId,
            sourceId = draft.sourceId, adapterId = draft.adapterId,
            status = SourceRunStatus.PENDING, startedAt = null, finishedAt = null,
            resultCount = 0, errorCode = null, errorMessage = null
        )
        database.sourceRunDao().insert(row)
        return row
    }

    suspend fun startSource(sourceRunId: String, at: Long): Boolean = database.withTransaction {
        val row = database.sourceRunDao().getById(sourceRunId) ?: return@withTransaction false
        if (row.status != SourceRunStatus.PENDING) return@withTransaction false
        database.sourceRunDao().update(row.copy(status = SourceRunStatus.RUNNING, startedAt = at))
        true
    }

    suspend fun finishSource(sourceRunId: String, resultCount: Int, at: Long): Boolean {
        require(resultCount >= 0)
        return database.withTransaction {
            val row = database.sourceRunDao().getById(sourceRunId) ?: return@withTransaction false
            if (row.status != SourceRunStatus.RUNNING) return@withTransaction false
            database.sourceRunDao().update(row.copy(status = SourceRunStatus.SUCCEEDED, resultCount = resultCount, finishedAt = at))
            true
        }
    }

    suspend fun failSource(sourceRunId: String, code: String, message: String, at: Long): Boolean {
        require(code.isNotBlank() && code.length <= 80)
        require(message.length <= 2000)
        return database.withTransaction {
            val row = database.sourceRunDao().getById(sourceRunId) ?: return@withTransaction false
            if (row.status != SourceRunStatus.RUNNING) return@withTransaction false
            database.sourceRunDao().update(row.copy(status = SourceRunStatus.FAILED, errorCode = code, errorMessage = message, finishedAt = at))
            true
        }
    }

    suspend fun requestCancellation(runId: String, at: Long): Boolean = database.withTransaction {
        val run = database.discoveryRunDao().getById(runId) ?: return@withTransaction false
        if (run.status != DiscoveryRunStatus.RUNNING && run.status != DiscoveryRunStatus.CREATED) return@withTransaction false
        database.discoveryRunDao().update(run.copy(status = DiscoveryRunStatus.CANCELLED, cancellationRequestedAt = at, finishedAt = at))
        database.sourceRunDao().cancelPending(runId, at)
        true
    }

    suspend fun reconcileInterrupted(at: Long): Int = database.withTransaction {
        val interrupted = database.discoveryRunDao().getRunning()
        interrupted.forEach { run ->
            database.discoveryRunDao().update(run.copy(status = DiscoveryRunStatus.PARTIAL, finishedAt = at, lastError = "Interrupted; inspect source runs before resuming."))
            database.sourceRunDao().markInterrupted(run.id, at)
        }
        interrupted.size
    }

    suspend fun complete(runId: String, at: Long): Boolean = database.withTransaction {
        val run = database.discoveryRunDao().getById(runId) ?: return@withTransaction false
        if (run.status != DiscoveryRunStatus.RUNNING) return@withTransaction false
        val sources = database.sourceRunDao().getForRun(runId)
        val status = when {
            sources.isEmpty() -> DiscoveryRunStatus.COMPLETED
            sources.all { it.status == SourceRunStatus.SUCCEEDED || it.status == SourceRunStatus.SKIPPED } -> DiscoveryRunStatus.COMPLETED
            sources.any { it.status == SourceRunStatus.SUCCEEDED } -> DiscoveryRunStatus.PARTIAL
            else -> DiscoveryRunStatus.FAILED
        }
        database.discoveryRunDao().update(run.copy(status = status, finishedAt = at))
        true
    }
}
