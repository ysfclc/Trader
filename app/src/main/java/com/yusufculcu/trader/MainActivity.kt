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
        val shared = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_VIEW -> intent.dataString
            else -> null
        }?.trim()?.takeIf { it.isNotEmpty() }
        if (shared != null) viewModel.captureSharedContent(shared)
    }
}
