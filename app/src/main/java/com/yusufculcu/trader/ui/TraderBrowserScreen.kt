package com.yusufculcu.trader.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TraderBrowserScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cookieManager = remember { CookieManager.getInstance() }

    var address by rememberSaveable { mutableStateOf("https://www.google.com") }
    var currentUrl by rememberSaveable { mutableStateOf("https://www.google.com") }
    var lastFirstPartyUrl by rememberSaveable { mutableStateOf("") }
    var authFallbackUrl by rememberSaveable { mutableStateOf("") }
    var pageTitle by rememberSaveable { mutableStateOf("Browser") }
    var loading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var browserMessage by remember { mutableStateOf("Sign in to websites here. Compatible sites keep their browser session on this device.") }
    var requiresExternalAuth by remember { mutableStateOf(false) }

    var browserContainer by remember { mutableStateOf<FrameLayout?>(null) }
    var mainWebView by remember { mutableStateOf<WebView?>(null) }
    var popupWebView by remember { mutableStateOf<WebView?>(null) }
    var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val callback = pendingFileCallback
        pendingFileCallback = null
        val data = result.data
        val uris = if (result.resultCode == Activity.RESULT_OK && data != null) {
            val clip = data.clipData
            when {
                clip != null -> Array(clip.itemCount) { index -> clip.getItemAt(index).uri }
                data.data != null -> arrayOf(data.data!!)
                else -> null
            }
        } else null
        callback?.onReceiveValue(uris)
    }

    fun openInSystemBrowser(rawTarget: String) {
        val target = TraderBrowserRouting.externalBrowserTarget(rawTarget, lastFirstPartyUrl)
        if (!TraderBrowserRouting.isWebUrl(target)) {
            browserMessage = "Open a website before sending it to your system browser."
            return
        }
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(target)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            browserMessage = "Opened in your system browser. That browser has its own login session; Android does not expose its cookies to Trader's embedded browser."
        } catch (_: ActivityNotFoundException) {
            browserMessage = "No compatible browser is installed."
        } catch (error: Exception) {
            browserMessage = "Could not open the system browser: ${error.message?.take(120) ?: "unknown error"}"
        }
    }

    fun closePopup(window: WebView? = popupWebView) {
        val popup = window ?: return
        if (popup !== popupWebView) return
        runCatching {
            popup.stopLoading()
            popup.webChromeClient = null
            popup.webViewClient = WebViewClient()
            browserContainer?.removeView(popup)
            popup.destroy()
        }
        popupWebView = null
        mainWebView?.onResume()
        currentUrl = mainWebView?.url ?: currentUrl
        address = currentUrl
        pageTitle = mainWebView?.title?.takeIf { it.isNotBlank() } ?: "Browser"
        cookieManager.flush()
        browserMessage = "Sign-in window closed. The website can now reflect any successful sign-in."
    }

    lateinit var configureWebView: (WebView, Boolean) -> Unit
    configureWebView = { view, supportsPopups ->
        TraderBrowserCookiePolicy.configure(view, cookieManager)

        view.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
        view.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(supportsPopups)
            allowFileAccess = false
            allowContentAccess = true
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            safeBrowsingEnabled = true
            loadsImagesAutomatically = true
            mediaPlaybackRequiresUserGesture = true
        }

        view.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                val pageUrl = url.orEmpty()
                if (pageUrl.isNotBlank()) {
                    currentUrl = pageUrl
                    address = pageUrl
                    if (TraderBrowserRouting.isGoogleIdentityUrl(pageUrl)) {
                        if (authFallbackUrl.isBlank()) {
                            authFallbackUrl = lastFirstPartyUrl.ifBlank { currentUrl }
                        }
                        requiresExternalAuth = true
                        browserMessage = "Google blocks OAuth sign-in inside embedded WebViews. Restart this website's sign-in in your system browser instead."
                    } else if (TraderBrowserRouting.isWebUrl(pageUrl)) {
                        lastFirstPartyUrl = pageUrl
                        authFallbackUrl = ""
                        requiresExternalAuth = false
                    }
                }
                loading = true
                progress = 0
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                val finishedUrl = url ?: view?.url.orEmpty()
                if (finishedUrl.isNotBlank()) {
                    currentUrl = finishedUrl
                    address = finishedUrl
                    pageTitle = view?.title?.takeIf { it.isNotBlank() } ?: finishedUrl
                }
                loading = false
                progress = 100
                // Flush the WebView cookie jar so an app/process restart does not discard a new login.
                cookieManager.flush()
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return true
                when (uri.scheme?.lowercase()) {
                    "https", "http" -> return false
                    "mailto", "tel", "sms", "geo" -> {
                        return try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            true
                        } catch (_: Exception) {
                            browserMessage = "No compatible app can open this link."
                            true
                        }
                    }
                    else -> {
                        browserMessage = "This page requested an app-specific link. Use the page's normal browser sign-in option or open the website in your system browser."
                        return true
                    }
                }
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: android.net.http.SslError?) {
                handler?.cancel()
                browserMessage = "Secure connection failed. Trader blocked the page rather than bypassing certificate validation."
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (request?.isForMainFrame == true) {
                    browserMessage = "Page could not load: ${error?.description?.toString()?.take(160) ?: "network error"}"
                }
            }
        }

        view.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress = newProgress.coerceIn(0, 100)
                loading = newProgress < 100
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!title.isNullOrBlank()) pageTitle = title.take(240)
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                // Only honour popups initiated by a real user gesture. This enables sign-in/consent
                // windows without allowing arbitrary background windows from page scripts.
                if (!supportsPopups || !isUserGesture || popupWebView != null) return false
                val container = browserContainer ?: return false
                val message = resultMsg ?: return false
                val transport = message.obj as? WebView.WebViewTransport ?: return false
                val popup = WebView(context)
                configureWebView(popup, false)
                container.addView(
                    popup,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
                popupWebView = popup
                transport.webView = popup
                message.sendToTarget()
                mainWebView?.onPause()
                browserMessage = "A sign-in or verification window opened. Complete it, then close this window to return to the website."
                return true
            }

            override fun onCloseWindow(window: WebView) {
                closePopup(window)
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                if (filePathCallback == null || fileChooserParams == null) return false
                pendingFileCallback?.onReceiveValue(null)
                pendingFileCallback = filePathCallback
                return runCatching {
                    filePicker.launch(fileChooserParams.createIntent())
                    true
                }.getOrElse {
                    pendingFileCallback?.onReceiveValue(null)
                    pendingFileCallback = null
                    browserMessage = "Could not open the file picker."
                    false
                }
            }

            override fun onPermissionRequest(request: PermissionRequest) {
                // Website camera/microphone permissions are deliberately not auto-granted.
                request.deny()
                browserMessage = "The website requested camera or microphone access. Trader has not granted site permissions; use your system browser for that step."
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    mainWebView?.onPause()
                    popupWebView?.onPause()
                    cookieManager.flush()
                }
                Lifecycle.Event.ON_RESUME -> {
                    mainWebView?.onResume()
                    popupWebView?.onResume()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(Unit) {
        onDispose {
            pendingFileCallback?.onReceiveValue(null)
            pendingFileCallback = null
            cookieManager.flush()
            listOfNotNull(popupWebView, mainWebView).forEach { view ->
                runCatching {
                    view.stopLoading()
                    view.onPause()
                    view.webChromeClient = null
                    view.webViewClient = WebViewClient()
                    (view.parent as? ViewGroup)?.removeView(view)
                    view.destroy()
                }
            }
            popupWebView = null
            mainWebView = null
            browserContainer = null
        }
    }

    val activeWebView = popupWebView ?: mainWebView
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Search or enter address") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    val target = TraderBrowserRouting.target(address)
                    address = target
                    activeWebView?.loadUrl(target)
                })
            )
            IconButton(onClick = {
                val target = TraderBrowserRouting.target(address)
                address = target
                activeWebView?.loadUrl(target)
            }) { Icon(Icons.Rounded.ArrowForward, "Go") }
        }

        if (loading) {
            LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { activeWebView?.let { if (it.canGoBack()) it.goBack() } }, enabled = activeWebView?.canGoBack() == true) {
                Icon(Icons.Rounded.ArrowBack, "Back")
            }
            IconButton(onClick = { activeWebView?.let { if (it.canGoForward()) it.goForward() } }, enabled = activeWebView?.canGoForward() == true) {
                Icon(Icons.Rounded.ArrowForward, "Forward")
            }
            IconButton(onClick = { activeWebView?.stopLoading() }, enabled = loading) {
                Icon(Icons.Rounded.Stop, "Stop")
            }
            IconButton(onClick = { activeWebView?.reload() }, enabled = activeWebView != null) {
                Icon(Icons.Rounded.Refresh, "Reload")
            }
            if (popupWebView != null) {
                TextButton(onClick = { closePopup() }) {
                    Icon(Icons.Rounded.Close, contentDescription = null)
                    Text("Close sign-in")
                }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { openInSystemBrowser(currentUrl) }, enabled = TraderBrowserRouting.isWebUrl(currentUrl)) {
                Text("Open in browser")
            }
            IconButton(onClick = {
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, currentUrl)
                }
                context.startActivity(Intent.createChooser(share, "Share page"))
            }) { Icon(Icons.Rounded.Share, "Share page") }
        }

        if (requiresExternalAuth) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Google sign-in needs a system browser", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Google blocks OAuth inside embedded WebViews. Trader cannot safely copy Chrome's login cookies into this browser. Restart the website in your system browser to complete that sign-in.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(onClick = {
                        val target = authFallbackUrl.ifBlank {
                            TraderBrowserRouting.externalBrowserTarget(currentUrl, lastFirstPartyUrl)
                        }
                        openInSystemBrowser(target)
                    }) { Text("Restart sign-in in system browser") }
                }
            }
        } else if (browserMessage.isNotBlank()) {
            Text(
                browserMessage,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)
            )
        }

        Text(pageTitle, style = MaterialTheme.typography.titleSmall, maxLines = 1, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        Text(currentUrl, style = MaterialTheme.typography.labelSmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
        HorizontalDivider()

        AndroidView(
            modifier = Modifier.fillMaxWidth().weight(1f),
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    browserContainer = this
                    val webView = WebView(ctx)
                    configureWebView(webView, true)
                    mainWebView = webView
                    addView(
                        webView,
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    )
                    val startUrl = if (TraderBrowserRouting.isGoogleIdentityUrl(currentUrl)) {
                        lastFirstPartyUrl.ifBlank { "https://www.google.com" }
                    } else currentUrl
                    webView.loadUrl(startUrl)
                }
            },
            update = { browserContainer = it }
        )
    }
}
