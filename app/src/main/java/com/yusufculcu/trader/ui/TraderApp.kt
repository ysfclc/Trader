package com.yusufculcu.trader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufculcu.trader.data.PlanStepEntity
import com.yusufculcu.trader.data.ProblemEntity
import com.yusufculcu.trader.domain.ProblemStatus
import com.yusufculcu.trader.viewmodel.TraderViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class AppTab { INBOX, ACTIVITY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraderApp(viewModel: TraderViewModel) {
    val problems by viewModel.problems.collectAsState()
    val events by viewModel.activity.collectAsState()
    var tab by rememberSaveable { mutableStateOf(AppTab.INBOX) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var showCreate by rememberSaveable { mutableStateOf(false) }
    val selected = problems.firstOrNull { it.id == selectedId }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (selected != null) "Problem" else "TRADER", fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        if (selected == null) Text("A workspace for getting things done", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    if (selected != null) IconButton(onClick = { selectedId = null }) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            if (selected == null) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(tab == AppTab.INBOX, { tab = AppTab.INBOX }, icon = { Icon(Icons.Rounded.Inbox, null) }, label = { Text("Inbox") })
                NavigationBarItem(tab == AppTab.ACTIVITY, { tab = AppTab.ACTIVITY }, icon = { Icon(Icons.Rounded.Timeline, null) }, label = { Text("Activity") })
            }
        },
        floatingActionButton = {
            if (selected == null && tab == AppTab.INBOX) FloatingActionButton(
                onClick = { showCreate = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Icon(Icons.Rounded.Add, contentDescription = "Capture a problem") }
        }
    ) { padding ->
        when {
            selected != null -> ProblemDetail(selected, viewModel, Modifier.padding(padding))
            tab == AppTab.INBOX -> InboxContent(problems, onOpen = { selectedId = it.id }, modifier = Modifier.padding(padding))
            else -> ActivityContent(events, Modifier.padding(padding))
        }
    }

    if (showCreate) CreateProblemDialog(
        onDismiss = { showCreate = false },
        onCreate = { title, description, outcome, constraints ->
            viewModel.createProblem(title, description, outcome, constraints)
            showCreate = false
        }
    )
}

@Composable
private fun InboxContent(problems: List<ProblemEntity>, onOpen: (ProblemEntity) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                Text("YOUR WORKSPACE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.8.sp)
                Text("Problem inbox", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            Text("${problems.size} captured", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(18.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Box(Modifier.background(Brush.linearGradient(listOf(Color(0xFF1C3430), Color(0xFF172226)))).padding(18.dp)) {
                Column {
                    Text("START WITH THE REAL THING", color = Color(0xFF9DE7C9), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.4.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("What needs to move forward?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Spacer(Modifier.height(5.dp))
                    Text("Capture a problem, goal, opportunity or piece of context. Trader keeps the thread and helps you work through it.", color = Color(0xFFB9C9C4), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Text("RECENT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.6.sp)
        Spacer(Modifier.height(10.dp))
        if (problems.isEmpty()) EmptyState()
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
            items(problems, key = { it.id }) { problem -> ProblemCard(problem) { onOpen(problem) } }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(Modifier.fillMaxWidth().padding(top = 34.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(64.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Inbox, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text("Nothing captured yet", fontWeight = FontWeight.SemiBold)
        Text("Add a problem or share something into Trader.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ProblemCard(problem: ProblemEntity, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(problem.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.2.sp)
                StatusPill(ProblemStatus.from(problem.status))
            }
            Spacer(Modifier.height(8.dp))
            Text(problem.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (problem.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(problem.description, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            Text("Updated ${formatDate(problem.updatedAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusPill(status: ProblemStatus) {
    val tint = when (status) {
        ProblemStatus.VERIFIED -> MaterialTheme.colorScheme.secondary
        ProblemStatus.RESOLVED -> MaterialTheme.colorScheme.primary
        ProblemStatus.WAITING -> Color(0xFFE7BE74)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = CircleShape, color = tint.copy(alpha = 0.13f)) {
        Text(status.label, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = tint, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ProblemDetail(problem: ProblemEntity, viewModel: TraderViewModel, modifier: Modifier = Modifier) {
    val steps by viewModel.stepsFor(problem.id).collectAsState(initial = emptyList())
    var showStep by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            Text(problem.category.uppercase(), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.5.sp)
            Text(problem.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            StatusPill(ProblemStatus.from(problem.status))
        }
        if (problem.description.isNotBlank()) item { DetailSection("CONTEXT", problem.description) }
        if (problem.desiredOutcome.isNotBlank()) item { DetailSection("DESIRED OUTCOME", problem.desiredOutcome) }
        if (problem.constraintsText.isNotBlank()) item { DetailSection("CONSTRAINTS", problem.constraintsText) }
        if (problem.originalContent.isNotBlank()) item { DetailSection("ORIGINAL CAPTURE", problem.originalContent) }
        item {
            Text("PLAN", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.5.sp)
            Text("Next steps", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        if (steps.isEmpty()) item { Text("No steps yet. Add the next concrete action.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(steps, key = { it.id }) { step ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = step.status == "DONE", onCheckedChange = { viewModel.toggleStep(step) })
                    Column(Modifier.weight(1f)) {
                        Text(step.title, fontWeight = FontWeight.Medium)
                        if (step.expectedResult.isNotBlank()) Text(step.expectedResult, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { showStep = true }) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(5.dp)); Text("Add step") }
                val status = ProblemStatus.from(problem.status)
                val next = when (status) {
                    ProblemStatus.OPEN -> ProblemStatus.IN_PROGRESS
                    ProblemStatus.IN_PROGRESS -> ProblemStatus.RESOLVED
                    ProblemStatus.WAITING -> ProblemStatus.IN_PROGRESS
                    ProblemStatus.RESOLVED -> ProblemStatus.VERIFIED
                    ProblemStatus.VERIFIED -> ProblemStatus.IN_PROGRESS
                }
                TextButton(onClick = { viewModel.setStatus(problem, next, if (next == ProblemStatus.VERIFIED) "Outcome confirmed by user" else "") }) {
                    Text(when (status) {
                        ProblemStatus.RESOLVED -> "Confirm outcome"
                        ProblemStatus.VERIFIED -> "Reopen"
                        else -> "Advance"
                    })
                }
            }
        }
    }
    if (showStep) AddStepDialog(
        onDismiss = { showStep = false },
        onAdd = { title -> viewModel.addStep(problem.id, title); showStep = false }
    )
}

@Composable
private fun DetailSection(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.4.sp)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Text(value, Modifier.fillMaxWidth().padding(14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActivityContent(events: List<com.yusufculcu.trader.data.ActivityEventEntity>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text("HISTORY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.6.sp)
        Text("Activity", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        if (events.isEmpty()) Text("Your actions and decisions will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(events, key = { it.id }) { event ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.secondary)
                        Column(Modifier.weight(1f)) {
                            Text(event.summary, fontWeight = FontWeight.SemiBold)
                            if (event.details.isNotBlank()) Text(event.details, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            Text(formatDate(event.occurredAt), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateProblemDialog(onDismiss: () -> Unit, onCreate: (String, String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var outcome by remember { mutableStateOf("") }
    var constraints by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Capture a problem") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("What needs solving?") }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("Context / what's happening") }, minLines = 2)
                OutlinedTextField(outcome, { outcome = it }, label = { Text("What would a good result look like?") }, minLines = 2)
                OutlinedTextField(constraints, { constraints = it }, label = { Text("Constraints (optional)") }, minLines = 1)
                Text("Any kind of problem is welcome. You can classify it later.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { Button(onClick = { if (title.isNotBlank()) onCreate(title, description, outcome, constraints) }, enabled = title.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddStepDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a next step") },
        text = { OutlinedTextField(title, { title = it }, label = { Text("Action") }, singleLine = true) },
        confirmButton = { Button(onClick = { if (title.isNotBlank()) onAdd(title) }, enabled = title.isNotBlank()) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatDate(time: Long): String = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(time))
