package com.yusufculcu.trader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yusufculcu.trader.R
import com.yusufculcu.trader.data.ProblemEntity
import com.yusufculcu.trader.data.WorkflowStepDraft
import com.yusufculcu.trader.data.WorkflowStepEntity
import com.yusufculcu.trader.domain.CapabilityAvailability
import com.yusufculcu.trader.domain.CapabilityRegistry
import com.yusufculcu.trader.domain.WorkflowRunStatus
import com.yusufculcu.trader.domain.WorkflowStepStatus
import com.yusufculcu.trader.viewmodel.TraderViewModel

@Composable
fun WorkflowPanel(problem: ProblemEntity, viewModel: TraderViewModel) {
    val plan by viewModel.stepsFor(problem.id).collectAsState(initial = emptyList())
    val runs by viewModel.workflowsFor(problem.id).collectAsState(initial = emptyList())
    var selectedRunId by rememberSaveable(problem.id) { mutableStateOf<String?>(null) }
    val selectedRun = runs.firstOrNull { it.id == selectedRunId } ?: runs.firstOrNull()
    val workflowSteps by viewModel.workflowStepsFor(selectedRun?.id.orEmpty()).collectAsState(initial = emptyList())
    val workflowEvents by viewModel.workflowEventsFor(selectedRun?.id.orEmpty()).collectAsState(initial = emptyList())
    var verificationNote by remember(selectedRun?.id) { mutableStateOf("") }

    LaunchedEffect(runs) {
        if (selectedRunId == null && runs.isNotEmpty()) selectedRunId = runs.first().id
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Divider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(stringResource(R.string.workflow_section), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
        Text(stringResource(R.string.workflow_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(stringResource(R.string.workflow_intro), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)

        if (runs.isEmpty()) {
            if (plan.isEmpty()) {
                Text(stringResource(R.string.workflow_add_plan_first), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            } else {
                Button(onClick = {
                    viewModel.createWorkflow(
                        problem.id,
                        problem.title,
                        plan.map { step ->
                            WorkflowStepDraft(
                                title = step.title,
                                capabilityId = step.requiredCapability.ifBlank { "manual.task" },
                                expectedPostcondition = step.expectedResult
                            )
                        }
                    ) { createdId -> selectedRunId = createdId }
                }) {
                    IconLabel(R.string.workflow_create)
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                runs.forEach { run ->
                    val active = run.id == selectedRun?.id
                    Surface(
                        onClick = { selectedRunId = run.id },
                        shape = RoundedCornerShape(50),
                        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            run.title,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    }
                }
            }
            selectedRun?.let { run ->
                StatusLabel(run.status)
                when (WorkflowRunStatus.from(run.status)) {
                    WorkflowRunStatus.DRAFT -> Button(onClick = { viewModel.transitionWorkflowRun(run.id, WorkflowRunStatus.READY) }) { Text(stringResource(R.string.workflow_mark_ready)) }
                    WorkflowRunStatus.READY -> Button(onClick = { viewModel.transitionWorkflowRun(run.id, WorkflowRunStatus.RUNNING) }) { Text(stringResource(R.string.workflow_start)) }
                    WorkflowRunStatus.RUNNING -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.transitionWorkflowRun(run.id, WorkflowRunStatus.PAUSED) }) {
                            androidx.compose.material3.Icon(Icons.Rounded.Pause, null)
                            Spacer(Modifier.width(5.dp))
                            Text(stringResource(R.string.workflow_pause))
                        }
                        TextButton(onClick = { viewModel.transitionWorkflowRun(run.id, WorkflowRunStatus.CANCELLED, details = "Cancelled by user") }) {
                            androidx.compose.material3.Icon(Icons.Rounded.Stop, null)
                            Text(stringResource(R.string.workflow_cancel))
                        }
                    }
                    WorkflowRunStatus.PAUSED -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.transitionWorkflowRun(run.id, WorkflowRunStatus.RUNNING) }) {
                            androidx.compose.material3.Icon(Icons.Rounded.PlayArrow, null)
                            Text(stringResource(R.string.workflow_resume))
                        }
                        TextButton(onClick = { viewModel.transitionWorkflowRun(run.id, WorkflowRunStatus.CANCELLED, details = "Cancelled by user") }) { Text(stringResource(R.string.workflow_cancel)) }
                    }
                    WorkflowRunStatus.AWAITING_VERIFICATION -> {
                        OutlinedTextField(
                            value = verificationNote,
                            onValueChange = { verificationNote = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.workflow_verification_note)) },
                            minLines = 2
                        )
                        Button(
                            onClick = {
                                viewModel.transitionWorkflowRun(
                                    run.id,
                                    WorkflowRunStatus.VERIFIED,
                                    outcomeVerified = true,
                                    details = verificationNote.trim()
                                )
                            },
                            enabled = verificationNote.isNotBlank()
                        ) { Text(stringResource(R.string.workflow_confirm_outcome)) }
                    }
                    WorkflowRunStatus.WAITING_FOR_APPROVAL -> Text(stringResource(R.string.workflow_waiting_approval), color = MaterialTheme.colorScheme.tertiary)
                    WorkflowRunStatus.NEEDS_REVIEW, WorkflowRunStatus.FAILED -> Text(run.lastError ?: stringResource(R.string.workflow_needs_review), color = MaterialTheme.colorScheme.error)
                    WorkflowRunStatus.VERIFIED -> Text(stringResource(R.string.workflow_verified), color = MaterialTheme.colorScheme.secondary)
                    WorkflowRunStatus.CANCELLED -> Text(stringResource(R.string.workflow_cancelled), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (workflowSteps.isEmpty()) {
                    CircularProgressIndicator()
                } else {
                    workflowSteps.forEach { step ->
                        WorkflowStepCard(step, run.status, viewModel)
                    }
                }
                if (workflowEvents.isNotEmpty()) {
                    Text(stringResource(R.string.workflow_history), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    workflowEvents.takeLast(4).reversed().forEach { event ->
                        Text(
                            event.summary + if (event.details.isNotBlank()) " · ${event.details}" else "",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        CapabilityAvailabilityList()
    }
}

@Composable
private fun WorkflowStepCard(step: WorkflowStepEntity, runStatus: String, viewModel: TraderViewModel) {
    val capability = CapabilityRegistry.assess(step.capabilityId)
    val stepStatus = WorkflowStepStatus.from(step.status)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(step.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                StatusLabel(step.status)
            }
            Text(
                capability.descriptor?.title ?: step.capabilityId,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
            if (!capability.canExecute) {
                Text(capability.reason ?: stringResource(R.string.workflow_capability_unavailable), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (step.expectedPostcondition.isNotBlank()) {
                Text(step.expectedPostcondition, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            if (runStatus == WorkflowRunStatus.RUNNING.name) {
                when {
                    step.requiresApproval && step.approvedPayloadHash != step.payloadHash ->
                        Button(onClick = { viewModel.approveWorkflowStep(step.id) }) { Text(stringResource(R.string.workflow_approve_exact_step)) }
                    stepStatus == WorkflowStepStatus.PENDING && capability.canExecute ->
                        Button(onClick = { viewModel.transitionWorkflowStep(step.id, WorkflowStepStatus.RUNNING) }) { Text(stringResource(R.string.workflow_mark_started)) }
                    stepStatus == WorkflowStepStatus.RUNNING ->
                        Button(onClick = { viewModel.transitionWorkflowStep(step.id, WorkflowStepStatus.COMPLETED, details = "Marked complete by user") }) { Text(stringResource(R.string.workflow_mark_completed)) }
                }
            }
        }
    }
}

@Composable
private fun CapabilityAvailabilityList() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TextButton(onClick = { expanded = !expanded }) {
            Text(stringResource(if (expanded) R.string.workflow_hide_capabilities else R.string.workflow_show_capabilities))
        }
        if (expanded) {
            CapabilityRegistry.all().forEach { capability ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(capability.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text(capability.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        stringResource(
                            when (capability.availability) {
                                CapabilityAvailability.AVAILABLE -> R.string.workflow_available
                                CapabilityAvailability.NOT_IMPLEMENTED -> R.string.workflow_not_implemented
                                CapabilityAvailability.BLOCKED -> R.string.workflow_blocked
                            }
                        ),
                        color = if (capability.availability == CapabilityAvailability.AVAILABLE) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusLabel(value: String) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(value.replace('_', ' '), Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun IconLabel(label: Int) {
    androidx.compose.material3.Icon(Icons.Rounded.CheckCircle, null)
    Spacer(Modifier.width(6.dp))
    Text(stringResource(label))
}
