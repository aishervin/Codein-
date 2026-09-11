package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewFeature
import androidx.webkit.WebSettingsCompat
import com.example.R
import com.example.bridge.PickedFile
import com.example.bridge.PickedItemResult
import com.example.bridge.SkippedFile
import com.example.bridge.WebViewBridge
import com.example.ui.StudioViewModel
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebEngineScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val targetUrl = remember { context.getString(R.string.bds_target_url) }
    val assetAuthority = remember { context.getString(R.string.bds_asset_authority) }

    var currentUrl by remember { mutableStateOf(targetUrl) }
    var isLoading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var bridgeRef by remember { mutableStateOf<WebViewBridge?>(null) }
    var pendingPickRequestId by remember { mutableStateOf<String?>(null) }
    var pendingPickMode by remember { mutableStateOf<String?>(null) }

    // Multi-file picker launcher
    val multiFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val reqId = pendingPickRequestId ?: return@rememberLauncherForActivityResult
        val bridge = bridgeRef ?: return@rememberLauncherForActivityResult
        val acceptImages = pendingPickMode?.endsWith("+images") == true
        pendingPickRequestId = null
        pendingPickMode = null

        if (uris.isEmpty()) {
            bridge.deliverPickError(reqId, "cancelled")
            return@rememberLauncherForActivityResult
        }

        bridge.deliverPickStatus(reqId, "reading")
        Thread {
            try {
                val files = mutableListOf<PickedFile>()
                val skipped = mutableListOf<SkippedFile>()
                for (uri in uris) {
                    when (val result = bridge.readPickedContentUri(uri, acceptImages)) {
                        is PickedItemResult.Ok -> files.add(result.file)
                        is PickedItemResult.Skipped -> skipped.add(SkippedFile(result.name, result.reason))
                    }
                }
                bridge.deliverPickedFiles(reqId, files, skipped, null)
            } catch (t: Throwable) {
                bridge.deliverPickError(reqId, "read-failed")
            }
        }.start()
    }

    // Folder picker launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        val reqId = pendingPickRequestId ?: return@rememberLauncherForActivityResult
        val bridge = bridgeRef ?: return@rememberLauncherForActivityResult
        val acceptImages = pendingPickMode?.endsWith("+images") == true
        pendingPickRequestId = null
        pendingPickMode = null

        if (treeUri == null) {
            bridge.deliverPickError(reqId, "cancelled")
            return@rememberLauncherForActivityResult
        }

        bridge.deliverPickStatus(reqId, "reading")
        Thread {
            try {
                val result = bridge.readPickedFolderTree(treeUri, acceptImages)
                bridge.deliverPickedFiles(reqId, result.files, result.skipped, result.folderName)
            } catch (t: Throwable) {
                bridge.deliverPickError(reqId, "read-failed")
            }
        }.start()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0E))
    ) {
        // Control & Address Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF16171A))
                .border(1.dp, Color(0xFF26282E))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = { webViewRef?.let { if (it.canGoBack()) it.goBack() } },
                enabled = canGoBack,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = if (canGoBack) Color(0xFFFF6B00) else Color(0xFF4A4D57),
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = { webViewRef?.reload() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reload",
                    tint = Color(0xFFFF6B00),
                    modifier = Modifier.size(16.dp)
                )
            }

            Surface(
                color = Color(0xFF0D0D0E),
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF26282E)),
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(if (isLoading) Color(0xFFFF8800) else Color(0xFF00FF88), RoundedCornerShape(3.dp))
                    )
                    Text(
                        text = currentUrl.removePrefix("https://").removePrefix("http://"),
                        color = Color(0xFF8E9099),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }

            // Sync Workspace to Web Bridge Button
            IconButton(
                onClick = {
                    bridgeRef?.let {
                        val js = "(function(){ window.__SHEN_STUDIO_ACTIVE__ = true; console.log('[SHΞN] Engine Connected'); })();"
                        webViewRef?.evaluateJavascript(js, null)
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Connect Studio",
                    tint = Color(0xFF00FF88),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = Color(0xFFFF6B00),
                trackColor = Color(0xFF16171A)
            )
        }

        // Upstream WebView Container
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = { ctx ->
                    val bridge = WebViewBridge(ctx.applicationContext)
                    bridgeRef = bridge

                    bridge.onPickFiles = { mode, reqId ->
                        (ctx as? Activity)?.runOnUiThread {
                            pendingPickRequestId = reqId
                            pendingPickMode = mode
                            when (mode) {
                                "folder", "folder+images" -> folderPickerLauncher.launch(null)
                                else -> multiFileLauncher.launch(arrayOf("*/*"))
                            }
                            bridge.deliverPickStatus(reqId, "opened")
                        }
                    }

                    val assetLoader = WebViewAssetLoader.Builder()
                        .setDomain(assetAuthority)
                        .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(ctx))
                        .build()

                    WebView(ctx).apply {
                        webViewRef = this
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            cacheMode = WebSettings.LOAD_DEFAULT
                            setSupportMultipleWindows(true)
                            javaScriptCanOpenWindowsAutomatically = true
                        }

                        val hostWebView = this
                        CookieManager.getInstance().apply {
                            setAcceptCookie(true)
                            setAcceptThirdPartyCookies(hostWebView, true)
                        }

                        addJavascriptInterface(bridge, "AndroidBridge")
                        bridge.evaluateJs = { script -> evaluateJavascript(script, null) }

                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView,
                                request: WebResourceRequest
                            ): WebResourceResponse? {
                                return assetLoader.shouldInterceptRequest(request.url)
                            }

                            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                url?.let { currentUrl = it }
                                canGoBack = view.canGoBack()
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                url?.let { currentUrl = it }
                                canGoBack = view.canGoBack()

                                // Inject upstream BDS bundled assets
                                try {
                                    val injected = ctx.assets.open("bds/injected.js").bufferedReader().readText()
                                    val css = ctx.assets.open("bds/content.css").bufferedReader().readText()
                                    val content = ctx.assets.open("bds/content.js").bufferedReader().readText()

                                    val cssEscaped = JSONObject.quote(css)
                                    val bootstrap = """
                                        (function () {
                                            if (window.__bdsAndroidBootstrapped) return;
                                            window.__bdsAndroidBootstrapped = true;
                                            try {
                                                var style = document.createElement('style');
                                                style.textContent = $cssEscaped;
                                                document.head.appendChild(style);
                                            } catch (e) { console.error('[SHΞN] css inject failed', e); }
                                        })();
                                    """.trimIndent()

                                    view.evaluateJavascript(injected, null)
                                    view.evaluateJavascript(bootstrap, null)
                                    view.evaluateJavascript(content, null)
                                } catch (e: Exception) {
                                    android.util.Log.e("ShenWebEngine", "Failed injecting bds assets", e)
                                }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                isLoading = newProgress < 100
                            }
                        }

                        setBackgroundColor(AndroidColor.parseColor("#0D0D0E"))
                        loadUrl(targetUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
