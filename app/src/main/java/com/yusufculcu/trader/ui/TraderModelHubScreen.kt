package com.yusufculcu.trader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yusufculcu.trader.TraderApplication
import com.yusufculcu.trader.ai.TraderModelCatalog
import com.yusufculcu.trader.ai.TraderModelStore
import java.util.Locale

@Composable
fun TraderModelHubScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as TraderApplication
    val store = remember(app) { app.modelStore }
    val states by store.states.collectAsState()
    val activeId = store.activeModelId()
    val scope = rememberCoroutineScope()
    var testOutput by remember { mutableStateOf("") }
    var testing by remember { mutableStateOf(false) }
    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text("Local AI · Model Hub", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Models run on your phone. Trader does not silently fall back to cloud AI.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(TraderModelCatalog.entries, key = { it.id }) { model ->
            val state = states[model.id] ?: TraderModelStore.ModelState(TraderModelStore.Status.NOT_INSTALLED)
            val enoughStorage = store.hasEnoughStorage(model)
            val ramOkay = store.hasRamHeadroom(model)
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(model.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(model.role.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() } + " · ${formatModelSize(model.sizeBytes)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(model.description, style = MaterialTheme.typography.bodyMedium)
                    Text(state.detail.ifBlank { state.status.name }, style = MaterialTheme.typography.labelMedium)
                    if (state.status == TraderModelStore.Status.DOWNLOADING || state.status == TraderModelStore.Status.VERIFYING) LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                    if (!enoughStorage) Text("Insufficient storage available for download plus reserve.", color = MaterialTheme.colorScheme.error)
                    if (!ramOkay) Text("Available RAM is below this model's preflight recommendation. Download is allowed, but inference may fail or be slow.", color = MaterialTheme.colorScheme.error)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (state.status) {
                            TraderModelStore.Status.INSTALLED -> {
                                if (activeId == model.id) Text("ACTIVE MODEL", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                else Button(onClick = { store.setActiveModel(model.id) }) { Text("Use model") }
                            }
                            TraderModelStore.Status.DOWNLOADING, TraderModelStore.Status.VERIFYING -> OutlinedButton(onClick = { store.pause(model.id) }) { Text("Pause") }
                            else -> Button(enabled = enoughStorage, onClick = { store.download(model) }) { Text(if (state.status == TraderModelStore.Status.PAUSED) "Resume download" else "Download") }
                        }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Inference smoke test", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Run a real prompt on the active model. This confirms local inference, not discovery automation.", style = MaterialTheme.typography.bodyMedium)
                    Button(enabled = activeId != null && !testing, onClick = {
                        val id = store.activeModelId()
                        val model = id?.let { TraderModelCatalog.find(it) }
                        val file = store.activeModelFile()
                        if (model == null || file == null) { testOutput = "No verified model is active. Download and select one above."; return@Button }
                        testing = true
                        testOutput = "Loading local model…"
                        scope.launch {
                            testOutput = runCatching {
                                com.yusufculcu.trader.ai.TraderLocalInference().generate(
                                    model, file,
                                    "You are Trader, a private on-device assistant. Answer briefly and truthfully.",
                                    "In one sentence, confirm that local inference is running and name one task you can help Trader with."
                                )
                            }.fold({ "Local inference succeeded:\\n\\n$it" }, { "Local inference failed: ${it.message ?: it.javaClass.simpleName}" })
                            testing = false
                        }
                    }) { Text(if (testing) "Running…" else "Run local AI test") }
                    if (testOutput.isNotBlank()) Text(testOutput, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

private fun formatModelSize(bytes: Long): String = String.format(Locale.getDefault(), "%.0f MB", bytes / 1_000_000f)