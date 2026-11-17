package com.yusufculcu.trader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yusufculcu.trader.R
import com.yusufculcu.trader.data.ProblemEntity
import com.yusufculcu.trader.data.PlanStepEntity
import com.yusufculcu.trader.data.WorkflowEventEntity
import com.yusufculcu.trader.data.WorkflowRunEntity
import com.yusufculcu.trader.data.WorkflowStepEntity
import com.yusufculcu.trader.domain.CapabilityRegistry
import com.yusufculcu.trader.domain.WorkflowRunStatus
import com.yusufculcu.trader.domain.WorkflowStepStatus
import com.yusufculcu.trader.viewmodel.TraderViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkflowContent(
    runs: List<WorkflowRunEntity>,
    problems: List<ProblemEntity>,
    onOpen: (WorkflowRunEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val problemTitles = problems.associate { it.id to it.title }
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(18.dp))
        Text(stringResource(R.string.workflow_workspace), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
        Text(stringResource(R.string.workflows), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        if (runs.isEmpty()) {
            Text(stringResource(R.string.workflow_empty), fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.workflow_empty_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
                items(runs, key = { it.id }) { run ->
                    WorkflowRunCard(
                        run = run,
                        problemTitle = problemTitles[run.problemId].orEmpty(),
                        onClick = { onOpen(run) }
                    )
                }
            }
        }
    }
}

@Composable
fun WorkflowSection(
    problem: ProblemEntity,
    planSteps: List<PlanStepEntity>,
    viewModel: TraderViewModel,
    onOpenWorkflow: (String) -> Unit
) {
    val runs by viewModel.workflowsFor(problem.id).collectAsState(initial = emptyList())
    val hasActiveRun = runs.any {
        WorkflowRunStatus.from(it.status) !in setOf(WorkflowRunStatus.VERIFIED, WorkflowRunStatus.CANCELLED)
    }
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(stringResource(R.string.workflows), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.4f)
        if (planSteps.isNotEmpty() && !hasActiveRun) {
            Button(
                onClick = {
                    viewModel.createWorkflowFromPlan(problem, planSteps) { id ->
                        if (id != null) onOpenWorkflow(id)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.create_workflow_from_plan))
            }
        }
        if (runs.isEmpty()) {
            Text(stringResource(R.string.workflow_none_for_problem), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        } else {
            runs.forEach { run ->
                WorkflowRunCard(run = run, problemTitle = problem.title, onClick = { onOpenWorkflow(run.id) })
            }
        }
    }
}

@Composable
fun WorkflowDetailScreen(
    run: WorkflowRunEntity,
    problemTitle: String,
    viewModel: TraderViewModel,
    modifier: Modifier = Modifier
) {
    val steps by viewModel.workflowStepsFor(run.id).collectAsState(initial = emptyList())
    val events by viewModel.workflowEventsFor(run.id).collectAsState(initial = emptyList())
    val status = WorkflowRunStatus.from(run.status)
    val hasExecutableWork = steps.any { step ->
        CapabilityRegistry.assess(step.capabilityId).canExecute &&
            WorkflowStepStatus.from(step.status) in setOf(
                WorkflowStepStatus.PENDING,
                WorkflowStepStatus.RUNNING,
                WorkflowStepStatus.WAITING_FOR_APPROVAL
            )
    }
    val waitingSteps = steps.filter { WorkflowStepStatus.from(it.status) == WorkflowStepStatus.WAITING_FOR_APPROVAL }
    val allWaitingApproved = waitingSteps.isNotEmpty() && waitingSteps.all {
        it.approvedPayloadHash == it.payloadHash && it.approvedAt != null
    }
    val failedSteps = steps.filter { WorkflowStepStatus.from(it.status) == WorkflowStepStatus.FAILED }
    val safeFailedSteps = failedSteps.isNotEmpty() && failedSteps.all { it.retrySafe }
    var showVerifyDialog by rememberSaveable(run.id) { mutableStateOf(false) }
    var showReviewDialog by rememberSaveable(run.id) { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            Text(stringResource(R.string.workflow_detail), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
            Text(run.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            WorkflowStatusPill(status)
            Spacer(Modifier.height(5.dp))
            Text(stringResource(R.string.workflow_parent_problem, problemTitle), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.workflow_manual_execution_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!run.lastError.isNullOrBlank()) Text(run.lastError.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            when (status) {
                WorkflowRunStatus.DRAFT -> Button(onClick = { viewModel.prepareWorkflow(run.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.workflow_prepare))
                }
                WorkflowRunStatus.READY -> {
                    if (hasExecutableWork) {
                        Button(onClick = { viewModel.startWorkflow(run.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.workflow_start))
                        }
                    } else {
                        Text(stringResource(R.string.workflow_no_executable_work), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
                WorkflowRunStatus.RUNNING -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.pauseWorkflow(run.id) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.workflow_pause)) }
                    TextButton(onClick = { viewModel.cancelWorkflow(run.id) }) { Text(stringResource(R.string.workflow_cancel)) }
                }
                WorkflowRunStatus.PAUSED -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.resumeWorkflow(run.id) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.workflow_resume)) }
                    TextButton(onClick = { viewModel.cancelWorkflow(run.id) }) { Text(stringResource(R.string.workflow_cancel)) }
                }
                WorkflowRunStatus.WAITING_FOR_APPROVAL -> {
                    if (allWaitingApproved) {
                        Button(onClick = { viewModel.resumeWorkflow(run.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.workflow_resume_approved))
                        }
                    } else {
                        Text(stringResource(R.string.workflow_waiting_approval_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { viewModel.cancelWorkflow(run.id) }) { Text(stringResource(R.string.workflow_cancel)) }
                }
                WorkflowRunStatus.NEEDS_REVIEW -> Button(onClick = { showReviewDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.workflow_review_continue))
                }
                WorkflowRunStatus.FAILED -> {
                    if (safeFailedSteps) {
                        Button(onClick = { viewModel.retryWorkflow(run.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.workflow_authorize_retry))
                        }
                    } else {
                        Text(stringResource(R.string.workflow_no_safe_retry), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { viewModel.cancelWorkflow(run.id) }) { Text(stringResource(R.string.workflow_cancel)) }
                }
                WorkflowRunStatus.AWAITING_VERIFICATION -> Button(onClick = { showVerifyDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.workflow_verify_outcome))
                }
                WorkflowRunStatus.VERIFIED -> Text(stringResource(R.string.workflow_outcome_verified), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                WorkflowRunStatus.CANCELLED -> Text(stringResource(R.string.workflow_cancelled), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Text(stringResource(R.string.workflow_steps), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        items(steps, key = { it.id }) { step ->
            WorkflowStepCard(step, status, viewModel)
        }
        item {
            Text(stringResource(R.string.workflow_history), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        if (events.isEmpty()) {
            item { Text(stringResource(R.string.workflow_no_history), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
        } else {
            items(events.asReversed(), key = { it.id }) { event -> WorkflowEventCard(event) }
        }
    }

    if (showVerifyDialog) WorkflowNoteDialog(
        title = stringResource(R.string.workflow_verify_title),
        hint = stringResource(R.string.workflow_verify_hint),
        confirmLabel = stringResource(R.string.workflow_verify_outcome),
        onDismiss = { showVerifyDialog = false },
        onConfirm = { note -> viewModel.verifyWorkflow(run.id, note); showVerifyDialog = false }
    )
    if (showReviewDialog) WorkflowNoteDialog(
        title = stringResource(R.string.workflow_review_title),
        hint = stringResource(R.string.workflow_review_hint),
        confirmLabel = stringResource(R.string.workflow_review_continue),
        onDismiss = { showReviewDialog = false },
        onConfirm = { note -> viewModel.resolveWorkflowReview(run.id, note); showReviewDialog = false }
    )
}

@Composable
private fun WorkflowStepCard(step: WorkflowStepEntity, runStatus: WorkflowRunStatus, viewModel: TraderViewModel) {
    val status = WorkflowStepStatus.from(step.status)
    val capability = CapabilityRegistry.assess(step.capabilityId)
    var showStepReview by rememberSaveable(step.id) { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(step.title, fontWeight = FontWeight.SemiBold)
                    Text(step.capabilityId, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                }
                WorkflowStepStatusPill(status)
            }
            if (step.expectedPostcondition.isNotBlank()) {
                Text(stringResource(R.string.workflow_expected_result, step.expectedPostcondition), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!step.lastError.isNullOrBlank()) Text(step.lastError.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            if (step.requiresApproval) {
                Text(stringResource(R.string.workflow_approval_required), color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                if (step.inputJson.isNotBlank() && step.inputJson != "{}") {
                    Text(step.inputJson, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!capability.canExecute && status == WorkflowStepStatus.PENDING) {
                Text(stringResource(R.string.workflow_capability_unavailable), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                if (runStatus == WorkflowRunStatus.RUNNING) {
                    TextButton(onClick = { viewModel.skipWorkflowStep(step.id) }) { Text(stringResource(R.string.workflow_skip_step)) }
                }
            }
            when {
                runStatus == WorkflowRunStatus.RUNNING && status == WorkflowStepStatus.PENDING && capability.canExecute -> {
                    Button(onClick = { viewModel.startWorkflowStep(step.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (step.requiresApproval) stringResource(R.string.workflow_approve_and_start) else stringResource(R.string.workflow_start_step))
                    }
                }
                runStatus == WorkflowRunStatus.WAITING_FOR_APPROVAL && status == WorkflowStepStatus.WAITING_FOR_APPROVAL -> {
                    Button(onClick = { viewModel.approveWorkflowStep(step.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.workflow_approve_step))
                    }
                }
                runStatus == WorkflowRunStatus.RUNNING && status == WorkflowStepStatus.RUNNING -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.completeWorkflowStep(step.id) }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.workflow_mark_complete))
                        }
                        TextButton(onClick = { showStepReview = true }) { Text(stringResource(R.string.workflow_needs_review)) }
                    }
                }
            }
        }
    }
    if (showStepReview) WorkflowNoteDialog(
        title = stringResource(R.string.workflow_step_review_title),
        hint = stringResource(R.string.workflow_step_review_hint),
        confirmLabel = stringResource(R.string.workflow_needs_review),
        onDismiss = { showStepReview = false },
        onConfirm = { note -> viewModel.flagWorkflowStepForReview(step.id, note); showStepReview = false }
    )
}

@Composable
private fun WorkflowRunCard(run: WorkflowRunEntity, problemTitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(run.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                WorkflowStatusPill(WorkflowRunStatus.from(run.status))
            }
            if (problemTitle.isNotBlank()) Text(problemTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.updated_at, formatWorkflowDate(run.updatedAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WorkflowStatusPill(status: WorkflowRunStatus) {
    val color = when (status) {
        WorkflowRunStatus.VERIFIED -> MaterialTheme.colorScheme.secondary
        WorkflowRunStatus.RUNNING -> MaterialTheme.colorScheme.primary
        WorkflowRunStatus.FAILED, WorkflowRunStatus.NEEDS_REVIEW -> MaterialTheme.colorScheme.error
        WorkflowRunStatus.WAITING_FOR_APPROVAL -> Color(0xFFE7BE74)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = CircleShape, color = color.copy(alpha = 0.14f)) {
        Text(
            stringResource(when (status) {
                WorkflowRunStatus.DRAFT -> R.string.workflow_status_draft
                WorkflowRunStatus.READY -> R.string.workflow_status_ready
                WorkflowRunStatus.RUNNING -> R.string.workflow_status_running
                WorkflowRunStatus.PAUSED -> R.string.workflow_status_paused
                WorkflowRunStatus.WAITING_FOR_APPROVAL -> R.string.workflow_status_waiting
                WorkflowRunStatus.NEEDS_REVIEW -> R.string.workflow_status_review
                WorkflowRunStatus.FAILED -> R.string.workflow_status_failed
                WorkflowRunStatus.AWAITING_VERIFICATION -> R.string.workflow_status_verification
                WorkflowRunStatus.VERIFIED -> R.string.workflow_status_verified
                WorkflowRunStatus.CANCELLED -> R.string.workflow_status_cancelled
            }),
            Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            color = color,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun WorkflowStepStatusPill(status: WorkflowStepStatus) {
    val color = when (status) {
        WorkflowStepStatus.COMPLETED -> MaterialTheme.colorScheme.secondary
        WorkflowStepStatus.RUNNING -> MaterialTheme.colorScheme.primary
        WorkflowStepStatus.FAILED, WorkflowStepStatus.NEEDS_REVIEW -> MaterialTheme.colorScheme.error
        WorkflowStepStatus.WAITING_FOR_APPROVAL -> Color(0xFFE7BE74)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = CircleShape, color = color.copy(alpha = 0.12f)) {
        Text(
            stringResource(when (status) {
                WorkflowStepStatus.PENDING -> R.string.workflow_step_pending
                WorkflowStepStatus.RUNNING -> R.string.workflow_status_running
                WorkflowStepStatus.WAITING_FOR_APPROVAL -> R.string.workflow_status_waiting
                WorkflowStepStatus.COMPLETED -> R.string.workflow_step_completed
                WorkflowStepStatus.FAILED -> R.string.workflow_status_failed
                WorkflowStepStatus.NEEDS_REVIEW -> R.string.workflow_status_review
                WorkflowStepStatus.SKIPPED -> R.string.workflow_step_skipped
                WorkflowStepStatus.CANCELLED -> R.string.workflow_status_cancelled
            }),
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = color,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun WorkflowEventCard(event: WorkflowEventEntity) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(event.summary, fontWeight = FontWeight.Medium)
            if (event.details.isNotBlank()) Text(event.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatWorkflowDate(event.occurredAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WorkflowNoteDialog(
    title: String,
    hint: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var note by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(note, { note = it }, label = { Text(hint) }, minLines = 2) },
        confirmButton = {
            Button(onClick = { if (note.isNotBlank()) onConfirm(note.trim()) }, enabled = note.isNotBlank()) {
                Text(confirmLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

private fun formatWorkflowDate(time: Long): String =
    SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(time))
