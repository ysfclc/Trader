package com.yusufculcu.trader

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateOf
import com.yusufculcu.trader.ui.TraderApp
import com.yusufculcu.trader.domain.SharedBrowserCaptureParser
import com.yusufculcu.trader.ui.theme.TraderTheme
import com.yusufculcu.trader.viewmodel.TraderViewModel
import com.yusufculcu.trader.viewmodel.TraderViewModelFactory

class MainActivity : ComponentActivity() {
    private val pendingBrowserCapturePayload = mutableStateOf<String?>(null)

    companion object {
        private const val STATE_PENDING_BROWSER_CAPTURE = "pending_browser_capture_v1"
    }

    private val viewModel: TraderViewModel by viewModels {
        TraderViewModelFactory((application as TraderApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val restoredCapture = savedInstanceState?.getString(STATE_PENDING_BROWSER_CAPTURE)
        if (!restoredCapture.isNullOrBlank()) {
            pendingBrowserCapturePayload.value = restoredCapture
        } else {
            handleIncomingIntent(intent)
        }
        setContent {
            TraderTheme {
                TraderApp(
                    viewModel = viewModel,
                    incomingBrowserCapture = pendingBrowserCapturePayload.value?.let(SharedBrowserCaptureParser::parse),
                    onBrowserCaptureHandled = { pendingBrowserCapturePayload.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_PENDING_BROWSER_CAPTURE, pendingBrowserCapturePayload.value)
        super.onSaveInstanceState(outState)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val content = extractIncomingContent(intent) ?: return
        // Consume the incoming share intent so rotation cannot create duplicate records.
        setIntent(Intent())
        if (SharedBrowserCaptureParser.parse(content) != null) {
            // Memo page captures require an explicit in-app review before persistence.
            pendingBrowserCapturePayload.value = content
        } else {
            viewModel.captureSharedContent(content)
        }
    }
}

/**
 * Extracts only content explicitly shared with Trader. Android's EXTRA_TEXT is a
 * CharSequence by contract, not necessarily a String (for example, styled text).
 */
internal fun extractIncomingContent(intent: Intent?): String? {
    val raw = when (intent?.action) {
        Intent.ACTION_SEND -> intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        Intent.ACTION_VIEW -> intent.dataString
        else -> null
    }
    return raw?.trim()?.takeIf(String::isNotEmpty)
}
