package com.yusufculcu.trader.ui

import android.content.Intent
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.widget.Toast
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.yusufculcu.trader.TraderApplication
import com.yusufculcu.trader.data.TraderBackupManager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.yusufculcu.trader.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufculcu.trader.data.EvidenceEntity
import com.yusufculcu.trader.data.OpportunityDraft
import com.yusufculcu.trader.data.OpportunityEntity
import com.yusufculcu.trader.data.OpportunityListItem
import com.yusufculcu.trader.data.PlanStepEntity
import com.yusufculcu.trader.data.ProblemEntity
import com.yusufculcu.trader.domain.ContextPacketBuilder
import com.yusufculcu.trader.domain.SharedBrowserCapture
import com.yusufculcu.trader.domain.SharedBrowserCaptureParser
import com.yusufculcu.trader.domain.ProblemStatus
import com.yusufculcu.trader.domain.OpportunityLane
import com.yusufculcu.trader.domain.OpportunityStage
import com.yusufculcu.trader.viewmodel.TraderViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale

private enum class AppTab { INBOX, DISCOVER, PIPELINE, ACTIVITY, BROWSER, AI }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraderApp(
    viewModel: TraderViewModel,
    incomingBrowserCapture: SharedBrowserCapture? = null,
    onBrowserCaptureHandled: () -> Unit = {}
) {
    val problems by viewModel.problems.collectAsState()
    val archivedProblems by viewModel.archivedProblems.collectAsState()
    val events by viewModel.activity.collectAsState()
    val opportunities by viewModel.opportunities.collectAsState()
    var tab by rememberSaveable { mutableStateOf(AppTab.DISCOVER) }
    var requestedBrowserUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var showBackupDialog by rememberSaveable { mutableStateOf(false) }
    var pendingBackupPassphrase by remember { mutableStateOf("") }
    var backupResult by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val passphrase = pendingBackupPassphrase
                backupResult = runCatching {
                    val app = context.applicationContext as TraderApplication
                    val output = context.contentResolver.openOutputStream(uri)
                        ?: error("The selected destination could not be opened.")
                    output.use { TraderBackupManager(app.database).exportEncrypted(it, passphrase.toCharArray()) }
                    context.getString(R.string.backup_export_success)
                }.getOrElse { context.getString(R.string.backup_failed) + " " + (it.message ?: "") }
                pendingBackupPassphrase = ""
            }
        } else {
            pendingBackupPassphrase = ""
        }
    }
    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val passphrase = pendingBackupPassphrase
                backupResult = runCatching {
                    val app = context.applicationContext as TraderApplication
                    val input = context.contentResolver.openInputStream(uri)
                        ?: error("The selected backup file could not be opened.")
                    input.use { TraderBackupManager(app.database).restoreEncrypted(it, passphrase.toCharArray()) }
                    context.getString(R.string.backup_restore_success)
                }.getOrElse { context.getString(R.string.backup_failed) + " " + (it.message ?: "") }
                pendingBackupPassphrase = ""
            }
        } else {
            pendingBackupPassphrase = ""
        }
    }
    val selected = (problems + archivedProblems).firstOrNull { it.id == selectedId }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (selected != null) stringResource(R.string.problem_label) else stringResource(R.string.app_name), fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        if (selected == null) Text(stringResource(R.string.workspace_subtitle), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    if (selected != null) IconButton(onClick = { selectedId = null }) { Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.back)) }
                },
                actions = {
                    if (selected == null) IconButton(onClick = { showBackupDialog = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.backup_restore))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            if (selected == null) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(tab == AppTab.INBOX, { tab = AppTab.INBOX }, icon = { Icon(Icons.Rounded.Inbox, null) }, label = { Text(stringResource(R.string.inbox)) })
                NavigationBarItem(tab == AppTab.DISCOVER, { tab = AppTab.DISCOVER }, icon = { Icon(Icons.Rounded.Search, null) }, label = { Text(stringResource(R.string.discover_nav)) })
                NavigationBarItem(tab == AppTab.PIPELINE, { tab = AppTab.PIPELINE }, icon = { Icon(Icons.Rounded.BusinessCenter, null) }, label = { Text(stringResource(R.string.pipeline)) })
                NavigationBarItem(tab == AppTab.ACTIVITY, { tab = AppTab.ACTIVITY }, icon = { Icon(Icons.Rounded.Timeline, null) }, label = { Text(stringResource(R.string.activity)) })
                NavigationBarItem(tab == AppTab.BROWSER, { tab = AppTab.BROWSER }, icon = { Icon(Icons.Rounded.Public, null) }, label = { Text("Browser") })
                NavigationBarItem(tab == AppTab.AI, { tab = AppTab.AI }, icon = { Text("AI") }, label = { Text("Models") })
            }
        },
        floatingActionButton = {
            if (selected == null && tab == AppTab.INBOX && !showArchived) FloatingActionButton(
                onClick = { showCreate = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.capture_problem)) }
        }
    ) { padding ->
        when {
            selected != null -> ProblemDetail(
                selected,
                viewModel,
                onBack = { selectedId = null },
                modifier = Modifier.padding(padding)
            )
            tab == AppTab.INBOX -> InboxContent(
                problems = if (showArchived) archivedProblems else problems,
                showArchived = showArchived,
                onShowArchived = { showArchived = it },
                onOpen = { selectedId = it.id },
                modifier = Modifier.padding(padding)
            )
            tab == AppTab.DISCOVER -> DiscoveryScreen(
                repository = (context.applicationContext as TraderApplication).repository,
                sourceStore = (context.applicationContext as TraderApplication).discoverySourceStore,
                onOpenSource = { url -> requestedBrowserUrl = url; tab = AppTab.BROWSER },
                modifier = Modifier.padding(padding)
            )
            tab == AppTab.BROWSER -> TraderBrowserScreen(
                modifier = Modifier.padding(padding),
                initialUrl = requestedBrowserUrl,
                onInitialUrlHandled = { requestedBrowserUrl = null },
                problems = problems,
                onSaveCapture = { problemId, title, url, content, onSaved ->
                    viewModel.captureBrowserPage(problemId, title, url, content, onSaved)
                }
            )
            tab == AppTab.AI -> TraderModelHubScreen(Modifier.padding(padding))
            tab == AppTab.PIPELINE -> OpportunityPipelineContent(
                opportunities = opportunities,
                onOpen = { selectedId = it.id },
                modifier = Modifier.padding(padding)
            )
            else -> ActivityContent(events, Modifier.padding(padding))
        }
    }

    incomingBrowserCapture?.let { capture ->
        SharedBrowserCaptureReviewDialog(
            capture = capture,
            problems = problems,
            onDismiss = onBrowserCaptureHandled,
            onSave = { problemId, title, content, onSaved ->
                viewModel.captureBrowserPage(
                    problemId = problemId,
                    title = title,
                    sourceUrl = capture.sourceUrl,
                    content = content,
                    onSaved = onSaved
                )
            }
        )
    }

    if (showCreate) CreateProblemDialog(
        onDismiss = { showCreate = false },
        onCreate = { title, description, outcome, constraints ->
            viewModel.createProblem(title, description, outcome, constraints)
            showCreate = false
        }
    )
    if (showBackupDialog) BackupDialog(
        onDismiss = { showBackupDialog = false },
        onExport = { passphrase ->
            pendingBackupPassphrase = passphrase
            showBackupDialog = false
            val fileName = "Trader-backup-${SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())}.traderbackup"
            exportBackupLauncher.launch(fileName)
        },
        onRestore = { passphrase ->
            pendingBackupPassphrase = passphrase
            showBackupDialog = false
            importBackupLauncher.launch(arrayOf("*/*"))
        }
    )
    backupResult?.let { message ->
        AlertDialog(
            onDismissRequest = { backupResult = null },
            title = { Text(stringResource(R.string.backup_result_title)) },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { backupResult = null }) { Text(stringResource(R.string.close)) } }
        )
    }
}
 

@Composable
private fun SharedBrowserCaptureReviewDialog(
    capture: SharedBrowserCapture,
    problems: List<ProblemEntity>,
    onDismiss: () -> Unit,
    onSave: (String?, String, String, (String?) -> Unit) -> Unit,
) {
    var title by remember(capture) { mutableStateOf(capture.title.take(180)) }
    var selectedProblemId by remember(capture) { mutableStateOf<String?>(null) }
    var destinationExpanded by remember(capture) { mutableStateOf(false) }
    var content by remember(capture) {
        mutableStateOf(SharedBrowserCaptureParser.toEvidenceContent(capture).take(18_000))
    }
    var saving by remember(capture) { mutableStateOf(false) }
    var error by remember(capture) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Review imported page") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Memo browser capture · Review before saving",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    capture.sourceUrl,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    "This page text came from a signed-in browser. It may contain private details. Remove anything you do not want stored. Account credentials and browser cookies are not included.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(180); error = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Evidence title") },
                    maxLines = 2
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it.take(18_000); error = null },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 300.dp),
                    label = { Text("Captured page text") },
                    minLines = 6,
                    maxLines = 14
                )
                Text("Save to", style = MaterialTheme.typography.labelMedium)
                Box {
                    OutlinedButton(
                        onClick = { destinationExpanded = true },
                        enabled = !saving
                    ) {
                        val destinationTitle = selectedProblemId?.let { id ->
                            problems.firstOrNull { it.id == id }?.title
                        } ?: "New research record"
                        Text(destinationTitle, maxLines = 1)
                    }
                    DropdownMenu(
                        expanded = destinationExpanded,
                        onDismissRequest = { destinationExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("New research record") },
                            onClick = {
                                selectedProblemId = null
                                destinationExpanded = false
                            }
                        )
                        problems.take(40).forEach { problem ->
                            DropdownMenuItem(
                                text = { Text(problem.title, maxLines = 1) },
                                onClick = {
                                    selectedProblemId = problem.id
                                    destinationExpanded = false
                                }
                            )
                        }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank() || content.trim().length < 20) {
                        error = "Add a title and at least 20 characters of evidence."
                    } else {
                        saving = true
                        onSave(selectedProblemId, title.trim(), content.trim()) { savedId ->
                            saving = false
                            if (savedId != null) {
                                onDismiss()
                            } else {
                                error = "Trader could not save this capture. Review the source and try again."
                            }
                        }
                    }
                },
                enabled = !saving && title.isNotBlank() && content.trim().length >= 20
            ) { Text(if (saving) "Saving…" else "Save to Trader") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) { Text("Discard") }
        }
    )
}

@Composable
private fun BackupDialog(
    onDismiss: () -> Unit,
    onExport: (String) -> Unit,
    onRestore: (String) -> Unit
) {
    var passphrase by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var restoreAcknowledged by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val tooShort = stringResource(R.string.backup_passphrase_too_short)
    val mismatch = stringResource(R.string.backup_passphrase_mismatch)
    val acknowledge = stringResource(R.string.backup_ack_required)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_restore)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.backup_intro), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it; validationError = null },
                    label = { Text(stringResource(R.string.backup_passphrase)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it; validationError = null },
                    label = { Text(stringResource(R.string.backup_confirm_passphrase)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                Text(stringResource(R.string.backup_passphrase_hint), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider()
                Text(stringResource(R.string.backup_restore_warning), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = restoreAcknowledged, onCheckedChange = { restoreAcknowledged = it })
                    Text(stringResource(R.string.backup_restore_ack), style = MaterialTheme.typography.labelSmall)
                }
                validationError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                Button(
                    onClick = {
                        when {
                            passphrase.length < 12 -> validationError = tooShort
                            passphrase != confirmation -> validationError = mismatch
                            else -> onExport(passphrase)
                        }
                    },
                    enabled = passphrase.isNotEmpty() && confirmation.isNotEmpty()
                ) { Text(stringResource(R.string.backup_export)) }
                OutlinedButton(
                    onClick = {
                        when {
                            passphrase.length < 12 -> validationError = tooShort
                            !restoreAcknowledged -> validationError = acknowledge
                            else -> onRestore(passphrase)
                        }
                    },
                    enabled = passphrase.isNotEmpty() && restoreAcknowledged
                ) { Text(stringResource(R.string.backup_import)) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun InboxContent(
    problems: List<ProblemEntity>,
    showArchived: Boolean,
    onShowArchived: (Boolean) -> Unit,
    onOpen: (ProblemEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var search by rememberSaveable { mutableStateOf("") }
    val visible = remember(problems, search) {
        val query = search.trim()
        if (query.isBlank()) problems
        else problems.filter { problem ->
            listOf(
                problem.title,
                problem.description,
                problem.desiredOutcome,
                problem.constraintsText,
                problem.originalContent,
                problem.sourceUri.orEmpty()
            ).any { it.contains(query, ignoreCase = true) }
        }
    }
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                Text(stringResource(R.string.your_workspace), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.8.sp)
                Text(stringResource(if (showArchived) R.string.archived_cases else R.string.problem_inbox), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            Text(stringResource(R.string.problems_captured, problems.size), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !showArchived, onClick = { onShowArchived(false) }, label = { Text(stringResource(R.string.active_cases)) })
            FilterChip(selected = showArchived, onClick = { onShowArchived(true) }, label = { Text(stringResource(R.string.archived_cases)) })
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            placeholder = { Text(stringResource(R.string.search_cases)) }
        )
        if (!showArchived && search.isBlank()) {
            Spacer(Modifier.height(18.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Box(Modifier.background(Brush.linearGradient(listOf(Color(0xFF1C3430), Color(0xFF172226)))).padding(18.dp)) {
                    Column {
                        Text(stringResource(R.string.start_with_real_thing), color = Color(0xFF9DE7C9), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.4.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.what_needs_to_move), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Spacer(Modifier.height(5.dp))
                        Text(stringResource(R.string.capture_intro), color = Color(0xFFB9C9C4), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(stringResource(if (showArchived) R.string.archived_cases else R.string.recent), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.6.sp)
        Spacer(Modifier.height(10.dp))
        when {
            visible.isEmpty() && search.isNotBlank() -> Text(stringResource(R.string.no_search_results), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 28.dp))
            visible.isEmpty() && showArchived -> Text(stringResource(R.string.no_archived_cases), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 28.dp))
            visible.isEmpty() -> EmptyState()
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
                items(visible, key = { it.id }) { problem -> ProblemCard(problem, showArchived) { onOpen(problem) } }
            }
        }
    }
}


@Composable
private fun OpportunityPipelineContent(
    opportunities: List<OpportunityListItem>,
    onOpen: (ProblemEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var search by rememberSaveable { mutableStateOf("") }
    var showClosed by rememberSaveable { mutableStateOf(false) }
    val visible = remember(opportunities, search, showClosed) {
        val query = search.trim()
        opportunities.filter { item ->
            val closed = item.opportunity.stage == OpportunityStage.WON.name ||
                item.opportunity.stage == OpportunityStage.LOST.name
            val matchesQuery = query.isBlank() || listOf(
                item.problem.title,
                item.opportunity.organizationName,
                item.opportunity.opportunityName,
                item.opportunity.contactName,
                item.opportunity.fitNotes,
                item.opportunity.nextAction
            ).any { it.contains(query, ignoreCase = true) }
            closed == showClosed && matchesQuery
        }
    }

    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(18.dp))
        Text(stringResource(R.string.your_workspace), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.8.sp)
        Text(stringResource(R.string.pipeline), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.pipeline_intro), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !showClosed, onClick = { showClosed = false }, label = { Text(stringResource(R.string.pipeline_active)) })
            FilterChip(selected = showClosed, onClick = { showClosed = true }, label = { Text(stringResource(R.string.pipeline_closed)) })
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            placeholder = { Text(stringResource(R.string.pipeline_search)) }
        )
        Spacer(Modifier.height(16.dp))
        when {
            visible.isEmpty() && opportunities.isEmpty() -> {
                Text(stringResource(R.string.pipeline_empty), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.pipeline_empty_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            visible.isEmpty() && search.isNotBlank() -> Text(stringResource(R.string.pipeline_no_results), color = MaterialTheme.colorScheme.onSurfaceVariant)
            visible.isEmpty() -> Text(stringResource(if (showClosed) R.string.pipeline_no_closed else R.string.pipeline_no_active), color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
                items(visible, key = { it.opportunity.id }) { item ->
                    OpportunityPipelineCard(item, onClick = { onOpen(item.problem) })
                }
            }
        }
    }
}

@Composable
private fun OpportunityPipelineCard(item: OpportunityListItem, onClick: () -> Unit) {
    val opportunity = item.opportunity
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(laneLabelRes(opportunity.lane)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.2.sp)
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(stringResource(stageLabelRes(opportunity.stage)), Modifier.padding(horizontal = 9.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(opportunity.organizationName.ifBlank { item.problem.title }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(opportunity.opportunityName, style = MaterialTheme.typography.bodyMedium)
            if (opportunity.nextAction.isNotBlank()) {
                Text(stringResource(R.string.next_action_prefix, opportunity.nextAction), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            opportunity.followUpAt?.let { due ->
                val overdue = isBeforeToday(due)
                Text(
                    stringResource(if (overdue) R.string.follow_up_overdue else R.string.follow_up_on, formatDateOnly(due)),
                    color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }
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
        Text(stringResource(R.string.nothing_captured), fontWeight = FontWeight.SemiBold)
        Text(stringResource(R.string.empty_inbox_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ProblemCard(problem: ProblemEntity, isArchived: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(when (problem.category) { "General" -> R.string.category_general; "Captured" -> R.string.category_captured; else -> R.string.category_other }), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.2.sp)
                if (!isArchived) StatusPill(ProblemStatus.from(problem.status))
                else Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(stringResource(R.string.archived_label), Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(problem.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (problem.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(problem.description, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.updated_at, formatDate(problem.updatedAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Text(stringResource(when (status) { ProblemStatus.OPEN -> R.string.status_open; ProblemStatus.IN_PROGRESS -> R.string.status_in_progress; ProblemStatus.WAITING -> R.string.status_waiting; ProblemStatus.RESOLVED -> R.string.status_resolved; ProblemStatus.VERIFIED -> R.string.status_verified }), Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = tint, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ProblemDetail(
    problem: ProblemEntity,
    viewModel: TraderViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps by viewModel.stepsFor(problem.id).collectAsState(initial = emptyList())
    val evidence by viewModel.evidenceFor(problem.id).collectAsState(initial = emptyList())
    val opportunity by viewModel.opportunityFor(problem.id).collectAsState(initial = null)
    val context = LocalContext.current
    val isArchived = problem.archivedAt != null
    var showStep by rememberSaveable(problem.id) { mutableStateOf(false) }
    var showEdit by rememberSaveable(problem.id) { mutableStateOf(false) }
    var showEvidence by rememberSaveable(problem.id) { mutableStateOf(false) }
    var showOpportunityEditor by rememberSaveable(problem.id) { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            Text(stringResource(when (problem.category) { "General" -> R.string.category_general; "Captured" -> R.string.category_captured; else -> R.string.category_other }), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.5.sp)
            Text(problem.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (isArchived) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(stringResource(R.string.archived_label), Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall)
                }
            } else StatusPill(ProblemStatus.from(problem.status))
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!isArchived) {
                    OutlinedButton(onClick = { showEdit = true }) {
                        Icon(Icons.Rounded.Edit, null)
                        Spacer(Modifier.width(5.dp))
                        Text(stringResource(R.string.edit_case))
                    }
                }
                OutlinedButton(onClick = {
                    val packet = ContextPacketBuilder.build(problem, steps, evidence)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Trader · ${problem.title}")
                        putExtra(Intent.EXTRA_TEXT, packet)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_context)))
                }) {
                    Icon(Icons.Rounded.Share, null)
                    Spacer(Modifier.width(5.dp))
                    Text(stringResource(R.string.share_context))
                }
            }
            if (isArchived) {
                TextButton(onClick = { viewModel.restoreProblem(problem.id) }) {
                    Icon(Icons.Rounded.Unarchive, null)
                    Spacer(Modifier.width(5.dp))
                    Text(stringResource(R.string.restore_case))
                }
            } else {
                TextButton(onClick = { viewModel.archiveProblem(problem.id, onSaved = { if (it) onBack() }) }) {
                    Icon(Icons.Rounded.Archive, null)
                    Spacer(Modifier.width(5.dp))
                    Text(stringResource(R.string.archive_case))
                }
            }
        }
        if (problem.description.isNotBlank()) item { DetailSection(stringResource(R.string.context), problem.description) }
        if (problem.desiredOutcome.isNotBlank()) item { DetailSection(stringResource(R.string.desired_outcome), problem.desiredOutcome) }
        if (problem.constraintsText.isNotBlank()) item { DetailSection(stringResource(R.string.constraints), problem.constraintsText) }
        if (problem.originalContent.isNotBlank()) item { DetailSection(stringResource(R.string.original_capture), problem.originalContent) }
        if (!problem.sourceUri.isNullOrBlank()) {
            item {
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(problem.sourceUri)))
                }) {
                    Icon(Icons.Rounded.Link, null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.open_original_source))
                }
            }
        }


        item {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.opportunity_section), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.5.sp)
                        Text(stringResource(R.string.opportunity_section_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    }
                    if (!isArchived) {
                        OutlinedButton(onClick = { showOpportunityEditor = true }) {
                            Icon(Icons.Rounded.Edit, null)
                            Spacer(Modifier.width(5.dp))
                            Text(stringResource(if (opportunity == null) R.string.track_opportunity else R.string.edit_opportunity))
                        }
                    }
                }
                if (opportunity == null) {
                    Text(stringResource(R.string.opportunity_not_tracked), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                } else {
                    OpportunityDetailCard(opportunity!!)
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(stringResource(R.string.evidence), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.5.sp)
                    Text(stringResource(R.string.evidence_count, evidence.size), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
                if (!isArchived) Button(onClick = { showEvidence = true }) {
                    Icon(Icons.Rounded.Add, null)
                    Spacer(Modifier.width(5.dp))
                    Text(stringResource(R.string.add_evidence))
                }
            }
        }
        if (evidence.isEmpty()) item { Text(stringResource(R.string.no_evidence), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
        items(evidence, key = { it.id }) { item ->
            EvidenceCard(
                evidence = item,
                onToggleVerified = { viewModel.setEvidenceVerified(item, item.verificationState != "VERIFIED") },
                onOpenSource = {
                    val uri = item.sourceUri?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
                    if (uri != null) context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri)))
                }
            )
        }

        item {
            Text(stringResource(R.string.plan), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.5.sp)
            Text(stringResource(R.string.next_steps), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        if (steps.isEmpty()) item { Text(stringResource(R.string.no_steps), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(steps, key = { it.id }) { step ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = step.status == "DONE",
                        onCheckedChange = { if (!isArchived) viewModel.toggleStep(step) },
                        enabled = !isArchived
                    )
                    Column(Modifier.weight(1f)) {
                        Text(step.title, fontWeight = FontWeight.Medium)
                        if (step.expectedResult.isNotBlank()) Text(step.expectedResult, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (!isArchived) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { showStep = true }) {
                        Icon(Icons.Rounded.Add, null)
                        Spacer(Modifier.width(5.dp))
                        Text(stringResource(R.string.add_step))
                    }
                    val status = ProblemStatus.from(problem.status)
                    val next = when (status) {
                        ProblemStatus.OPEN -> ProblemStatus.IN_PROGRESS
                        ProblemStatus.IN_PROGRESS -> ProblemStatus.RESOLVED
                        ProblemStatus.WAITING -> ProblemStatus.IN_PROGRESS
                        ProblemStatus.RESOLVED -> ProblemStatus.VERIFIED
                        ProblemStatus.VERIFIED -> ProblemStatus.IN_PROGRESS
                    }
                    val verifiedNote = stringResource(R.string.outcome_confirmed_by_user)
                    TextButton(onClick = { viewModel.setStatus(problem, next, if (next == ProblemStatus.VERIFIED) verifiedNote else "") }) {
                        Text(when (status) {
                            ProblemStatus.RESOLVED -> stringResource(R.string.confirm_outcome)
                            ProblemStatus.VERIFIED -> stringResource(R.string.reopen)
                            else -> stringResource(R.string.advance)
                        })
                    }
                }
            }
            item { WorkflowPanel(problem, viewModel) }
        }
    }
    if (showStep) AddStepDialog(
        onDismiss = { showStep = false },
        onAdd = { title, expected -> viewModel.addStep(problem.id, title, expected); showStep = false }
    )
    if (showEdit) EditProblemDialog(
        problem = problem,
        onDismiss = { showEdit = false },
        onSave = { title, description, outcome, constraints ->
            viewModel.updateProblem(problem.id, title, description, outcome, constraints) { saved ->
                if (saved) showEdit = false
            }
        }
    )
    if (showEvidence) AddEvidenceDialog(
        onDismiss = { showEvidence = false },
        onAdd = { title, content, source ->
            viewModel.addEvidence(problem.id, title, content, source) { saved ->
                if (saved) showEvidence = false
            }
        }
    )
    if (showOpportunityEditor) OpportunityEditorDialog(
        existing = opportunity,
        onDismiss = { showOpportunityEditor = false },
        onSave = { draft ->
            viewModel.saveOpportunity(problem.id, draft) { saved ->
                if (saved) showOpportunityEditor = false
            }
        }
    )
}

@Composable
private fun EvidenceCard(
    evidence: EvidenceEntity,
    onToggleVerified: () -> Unit,
    onOpenSource: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(evidence.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                Surface(
                    shape = CircleShape,
                    color = if (evidence.verificationState == "VERIFIED") MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        stringResource(if (evidence.verificationState == "VERIFIED") R.string.evidence_verified else R.string.evidence_unreviewed),
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            if (evidence.content.isNotBlank()) Text(evidence.content, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            if (!evidence.sourceUri.isNullOrBlank()) {
                TextButton(onClick = onOpenSource) {
                    Icon(Icons.Rounded.Link, null)
                    Spacer(Modifier.width(4.dp))
                    Text(evidence.sourceUri, maxLines = 1)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onToggleVerified) {
                    Text(stringResource(if (evidence.verificationState == "VERIFIED") R.string.undo_evidence_verification else R.string.mark_evidence_verified))
                }
            }
            Text(formatDate(evidence.capturedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
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
        Text(stringResource(R.string.history), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.6.sp)
        Text(stringResource(R.string.activity), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        if (events.isEmpty()) Text(stringResource(R.string.activity_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(events, key = { it.id }) { event ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.secondary)
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(when (event.eventType) {
                                "created" -> R.string.event_problem_captured
                                "plan_step_added" -> R.string.event_plan_step_added
                                "plan_step_done" -> R.string.event_step_completed
                                "plan_step_reopened" -> R.string.event_step_reopened
                                "status_changed" -> R.string.event_status_updated
                                "details_updated" -> R.string.event_details_updated
                                "archived" -> R.string.event_archived
                                "restored" -> R.string.event_restored
                                "evidence_added" -> R.string.event_evidence_added
                                "evidence_verified" -> R.string.event_evidence_verified
                                "evidence_unreviewed" -> R.string.event_evidence_unreviewed
                                "opportunity_created" -> R.string.event_opportunity_created
                                "opportunity_stage_changed" -> R.string.event_opportunity_stage_changed
                                "opportunity_updated" -> R.string.event_opportunity_updated
                                else -> R.string.event_activity_recorded
                            }), fontWeight = FontWeight.SemiBold)
                            if (event.details.isNotBlank() && event.eventType != "created") Text(event.details, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
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
        title = { Text(stringResource(R.string.capture_problem)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.what_needs_solving)) }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text(stringResource(R.string.context_prompt)) }, minLines = 2)
                OutlinedTextField(outcome, { outcome = it }, label = { Text(stringResource(R.string.good_result_prompt)) }, minLines = 2)
                OutlinedTextField(constraints, { constraints = it }, label = { Text(stringResource(R.string.constraints_optional)) }, minLines = 1)
                Text(stringResource(R.string.classify_later), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { Button(onClick = { if (title.isNotBlank()) onCreate(title, description, outcome, constraints) }, enabled = title.isNotBlank()) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun EditProblemDialog(
    problem: ProblemEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var title by remember(problem.id) { mutableStateOf(problem.title) }
    var description by remember(problem.id) { mutableStateOf(problem.description) }
    var outcome by remember(problem.id) { mutableStateOf(problem.desiredOutcome) }
    var constraints by remember(problem.id) { mutableStateOf(problem.constraintsText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_case)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.what_needs_solving)) }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text(stringResource(R.string.context_prompt)) }, minLines = 2)
                OutlinedTextField(outcome, { outcome = it }, label = { Text(stringResource(R.string.good_result_prompt)) }, minLines = 2)
                OutlinedTextField(constraints, { constraints = it }, label = { Text(stringResource(R.string.constraints_optional)) }, minLines = 2)
            }
        },
        confirmButton = { Button(onClick = { if (title.isNotBlank()) onSave(title, description, outcome, constraints) }, enabled = title.isNotBlank()) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun AddEvidenceDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    val canSave = title.isNotBlank() && (content.isNotBlank() || source.isNotBlank())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_evidence)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.evidence_title)) }, singleLine = true)
                OutlinedTextField(content, { content = it }, label = { Text(stringResource(R.string.evidence_note)) }, minLines = 3)
                OutlinedTextField(source, { source = it }, label = { Text(stringResource(R.string.evidence_source_url)) }, singleLine = true)
                Text(stringResource(R.string.evidence_note_hint), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { Button(onClick = { if (canSave) onAdd(title, content, source.trim().ifBlank { null }) }, enabled = canSave) { Text(stringResource(R.string.add)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun AddStepDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var expected by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_next_step)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.action_label)) }, singleLine = true)
                OutlinedTextField(expected, { expected = it }, label = { Text(stringResource(R.string.expected_result)) }, minLines = 2)
            }
        },
        confirmButton = { Button(onClick = { if (title.isNotBlank()) onAdd(title, expected) }, enabled = title.isNotBlank()) { Text(stringResource(R.string.add)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}


@Composable
private fun OpportunityDetailCard(opportunity: OpportunityEntity) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(laneLabelRes(opportunity.lane)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(stringResource(stageLabelRes(opportunity.stage)), Modifier.padding(horizontal = 9.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
            if (opportunity.organizationName.isNotBlank()) Text(opportunity.organizationName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(opportunity.opportunityName, fontWeight = FontWeight.Medium)
            if (opportunity.fitNotes.isNotBlank()) DetailSection(stringResource(R.string.fit_notes), opportunity.fitNotes)
            if (opportunity.contactName.isNotBlank()) Text(stringResource(R.string.contact_value, opportunity.contactName), style = MaterialTheme.typography.bodySmall)
            if (opportunity.contactRoute.isNotBlank()) {
                Text(stringResource(R.string.contact_route_value, opportunity.contactRoute), style = MaterialTheme.typography.bodySmall)
                val context = LocalContext.current
                TextButton(onClick = {
                    val intent = OpportunityContactRoute.createIntent(opportunity.contactRoute)
                    if (intent == null) {
                        Toast.makeText(context, context.getString(R.string.contact_route_unsupported), Toast.LENGTH_SHORT).show()
                    } else {
                        runCatching { context.startActivity(intent) }.onFailure {
                            if (it is ActivityNotFoundException) {
                                Toast.makeText(context, context.getString(R.string.contact_route_unsupported), Toast.LENGTH_SHORT).show()
                            } else throw it
                        }
                    }
                }) { Text(stringResource(R.string.open_contact_route)) }
            }
            if (opportunity.nextAction.isNotBlank()) Text(stringResource(R.string.next_action_prefix, opportunity.nextAction), style = MaterialTheme.typography.bodySmall)
            opportunity.followUpAt?.let { due ->
                Text(
                    stringResource(if (isBeforeToday(due)) R.string.follow_up_overdue else R.string.follow_up_on, formatDateOnly(due)),
                    color = if (isBeforeToday(due)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun OpportunityEditorDialog(
    existing: OpportunityEntity?,
    onDismiss: () -> Unit,
    onSave: (OpportunityDraft) -> Unit
) {
    var lane by remember(existing?.id) { mutableStateOf(existing?.lane ?: OpportunityLane.SERVICE.name) }
    var organizationName by remember(existing?.id) { mutableStateOf(existing?.organizationName.orEmpty()) }
    var opportunityName by remember(existing?.id) { mutableStateOf(existing?.opportunityName.orEmpty()) }
    var contactName by remember(existing?.id) { mutableStateOf(existing?.contactName.orEmpty()) }
    var contactRoute by remember(existing?.id) { mutableStateOf(existing?.contactRoute.orEmpty()) }
    var fitNotes by remember(existing?.id) { mutableStateOf(existing?.fitNotes.orEmpty()) }
    var stage by remember(existing?.id) { mutableStateOf(existing?.stage ?: OpportunityStage.DISCOVERED.name) }
    var nextAction by remember(existing?.id) { mutableStateOf(existing?.nextAction.orEmpty()) }
    var followUpAt by remember(existing?.id) { mutableStateOf(existing?.followUpAt) }
    val context = LocalContext.current
    val lanes = OpportunityLane.entries.map { it.name to stringResource(laneLabelRes(it.name)) }
    val stages = OpportunityStage.entries.map { it.name to stringResource(stageLabelRes(it.name)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing == null) R.string.track_opportunity else R.string.edit_opportunity)) },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                OpportunityChoice(stringResource(R.string.opportunity_lane), lane, lanes) { lane = it }
                OutlinedTextField(
                    value = organizationName,
                    onValueChange = { organizationName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.organization_name)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = opportunityName,
                    onValueChange = { opportunityName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.opportunity_name_required)) },
                    minLines = 2
                )
                OutlinedTextField(
                    value = fitNotes,
                    onValueChange = { fitNotes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.fit_notes)) },
                    minLines = 2
                )
                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.contact_name)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = contactRoute,
                    onValueChange = { contactRoute = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.contact_route)) },
                    singleLine = true
                )
                OpportunityChoice(stringResource(R.string.opportunity_stage), stage, stages) { stage = it }
                OutlinedTextField(
                    value = nextAction,
                    onValueChange = { nextAction = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.next_action)) },
                    minLines = 2
                )
                Text(stringResource(R.string.follow_up_date), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Button(
                    onClick = {
                        val current = Calendar.getInstance().apply { timeInMillis = followUpAt ?: System.currentTimeMillis() }
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                followUpAt = Calendar.getInstance().apply {
                                    set(year, month, day, 9, 0, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                            },
                            current.get(Calendar.YEAR),
                            current.get(Calendar.MONTH),
                            current.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(followUpAt?.let { formatDateOnly(it) } ?: stringResource(R.string.choose_follow_up_date))
                }
                if (followUpAt != null) {
                    TextButton(onClick = { followUpAt = null }) { Text(stringResource(R.string.clear_follow_up_date)) }
                }
                Text(stringResource(R.string.opportunity_manual_actions_note), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        OpportunityDraft(
                            lane = lane,
                            organizationName = organizationName,
                            opportunityName = opportunityName,
                            contactName = contactName,
                            contactRoute = contactRoute,
                            fitNotes = fitNotes,
                            stage = stage,
                            nextAction = nextAction,
                            followUpAt = followUpAt
                        )
                    )
                },
                enabled = opportunityName.isNotBlank()
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun OpportunityChoice(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit
) {
    var expanded by remember(selected) { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second ?: selected
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selectedLabel, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.MoreVert, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.second) },
                        onClick = {
                            onSelected(option.first)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

private fun laneLabelRes(lane: String): Int = when (lane) {
    OpportunityLane.TRADE.name -> R.string.lane_trade
    OpportunityLane.SERVICE.name -> R.string.lane_service
    OpportunityLane.CAREER.name -> R.string.lane_career
    else -> R.string.lane_other
}

private fun stageLabelRes(stage: String): Int = when (stage) {
    OpportunityStage.DISCOVERED.name -> R.string.stage_discovered
    OpportunityStage.QUALIFYING.name -> R.string.stage_qualifying
    OpportunityStage.READY_TO_CONTACT.name -> R.string.stage_ready_to_contact
    OpportunityStage.CONTACTED.name -> R.string.stage_contacted
    OpportunityStage.WAITING.name -> R.string.stage_waiting
    OpportunityStage.WON.name -> R.string.stage_won
    else -> R.string.stage_lost
}

private fun isBeforeToday(time: Long): Boolean {
    val due = Calendar.getInstance().apply {
        timeInMillis = time
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return due.before(today)
}

private fun formatDateOnly(time: Long): String = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(time))

private fun formatDate(time: Long): String = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(time))
