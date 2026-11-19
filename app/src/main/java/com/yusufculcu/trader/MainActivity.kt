package com.yusufculcu.trader

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.yusufculcu.trader.ui.TraderApp
import com.yusufculcu.trader.ui.theme.TraderTheme
import com.yusufculcu.trader.viewmodel.TraderViewModel
import com.yusufculcu.trader.viewmodel.TraderViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel: TraderViewModel by viewModels {
        TraderViewModelFactory((application as TraderApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)
        setContent { TraderTheme { TraderApp(viewModel) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        extractIncomingContent(intent)?.let(viewModel::captureSharedContent)
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
