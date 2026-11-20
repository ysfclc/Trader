package com.yusufculcu.trader.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp

private fun browserTarget(raw: String): String {
    val input = raw.trim()
    if (input.isBlank()) return "https://www.google.com"
    if (input.startsWith("http://") || input.startsWith("https://")) return input
    val looksLikeHost = input.contains('.') && !input.contains(' ')
    return if (looksLikeHost) "https://$input" else "https://www.google.com/search?q=" + Uri.encode(input)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TraderBrowserScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var address by rememberSaveable { mutableStateOf("https://www.google.com") }
    var currentUrl by rememberSaveable { mutableStateOf("https://www.google.com") }
    var pageTitle by rememberSaveable { mutableStateOf("Browser") }
    var loading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Search or enter address") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Go),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onGo = { webViewRef?.loadUrl(browserTarget(address)); address = browserTarget(address) })
            )
            IconButton(onClick = { webViewRef?.loadUrl(browserTarget(address)); address = browserTarget(address) }) { Icon(Icons.Rounded.ArrowForward, "Go") }
        }
        if (loading) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { webViewRef?.let { if (it.canGoBack()) it.goBack() } }, enabled = webViewRef?.canGoBack() == true) { Icon(Icons.Rounded.ArrowBack, "Back") }
            IconButton(onClick = { webViewRef?.let { if (it.canGoForward()) it.goForward() } }, enabled = webViewRef?.canGoForward() == true) { Icon(Icons.Rounded.ArrowForward, "Forward") }
            IconButton(onClick = { webViewRef?.stopLoading() }, enabled = loading) { Icon(Icons.Rounded.Stop, "Stop") }
            IconButton(onClick = { webViewRef?.reload() }) { Icon(Icons.Rounded.Refresh, "Reload") }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                val share = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"; putExtra(android.content.Intent.EXTRA_TEXT, currentUrl)
                }
                context.startActivity(android.content.Intent.createChooser(share, "Share page"))
            }) { Icon(Icons.Rounded.Share, "Share page") }
        }
        Text(pageTitle, style = MaterialTheme.typography.titleSmall, maxLines = 1, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        Text(currentUrl, style = MaterialTheme.typography.labelSmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
        HorizontalDivider()
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    settings.setSupportMultipleWindows(false)
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                            currentUrl = url; address = url; loading = true
                        }
                        override fun onPageFinished(view: WebView, url: String) {
                            currentUrl = url; address = url; loading = false; progress = 100
                            pageTitle = view.title?.takeIf { it.isNotBlank() } ?: url
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView, newProgress: Int) { progress = newProgress; loading = newProgress < 100 }
                        override fun onReceivedTitle(view: WebView, title: String?) { pageTitle = title?.takeIf { it.isNotBlank() } ?: "Browser" }
                    }
                    webViewRef = this
                    loadUrl(currentUrl)
                }
            },
            update = { webViewRef = it }
        )
    }
}
