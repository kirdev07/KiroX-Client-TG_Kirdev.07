package com.kirox.app.ui.chat

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.os.Environment
import android.view.View
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebViewDatabase
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import com.kirox.app.i18n.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import com.kirox.app.i18n.I18n
import com.kirox.app.settings.KiroxTweaks
import com.kirox.app.ui.components.FusionToast
import com.kirox.app.ui.icons.KiroXIcon
import com.kirox.app.ui.theme.Fusion

/** The browser's stored data: shared by every WebView in kiroX, mini apps included. */
object BrowserData {
    fun clear(context: Context) {
        CookieManager.getInstance().apply { removeAllCookies(null); flush() }
        WebStorage.getInstance().deleteAllData()
        runCatching { WebViewDatabase.getInstance(context).clearHttpAuthUsernamePassword() }
        runCatching { WebView(context).apply { clearCache(true); clearFormData(); clearHistory(); destroy() } }
    }
}

private const val DESKTOP_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36"

/**
 * «Встроенный браузер»: a web link from a chat opens over kiroX instead of the browser app. Back
 * goes back in the page history and closes on the first page; Telegram links go to [onTelegramLink].
 * What it may load and keep follows the browser settings: traffic saving on mobile data, private
 * mode, third-party cookies, JavaScript. Sites get no location, camera or microphone.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun InAppBrowser(url: String, onTelegramLink: (String) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val tweaks = remember { KiroxTweaks.current }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var title by remember { mutableStateOf("") }
    var current by remember { mutableStateOf(url) }
    var progress by remember { mutableIntStateOf(0) }
    var canBack by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var desktop by remember { mutableStateOf(false) }
    var finding by remember { mutableStateOf(false) }
    var findText by remember { mutableStateOf("") }
    var findResult by remember { mutableStateOf(0 to 0) }
    val toast = com.kirox.app.ui.components.rememberToast()
    var notice by toast
    // A page's own full-screen view (a video), shown over everything while it lasts.
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    // «Выбрать файл» on a page.
    var fileCallback by remember { mutableStateOf<ValueCallback<Array<android.net.Uri>>?>(null) }
    val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        fileCallback?.onReceiveValue(uris.toTypedArray()); fileCallback = null
    }
    val metered = remember { context.getSystemService(ConnectivityManager::class.java)?.isActiveNetworkMetered == true }
    val noImages = tweaks.browserSaveTraffic && metered
    fun close() {
        if (tweaks.browserPrivate) BrowserData.clear(context)
        onClose()
    }
    fun openOutside() { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, current.toUri())) }; close() }
    fun exitFullscreen() { customViewCallback?.onCustomViewHidden(); customView = null; customViewCallback = null }
    Dialog(onDismissRequest = ::close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler {
            when {
                customView != null -> exitFullscreen()
                finding -> { finding = false; webView?.clearMatches() }
                canBack -> webView?.goBack()
                else -> close()
            }
        }
        Box(Modifier.fillMaxSize().background(Fusion.colors.background)) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                if (finding) FindBar(
                    text = findText, result = findResult,
                    onText = { findText = it; webView?.findAllAsync(it) },
                    onNext = { forward -> webView?.findNext(forward) },
                    onClose = { finding = false; findText = ""; webView?.clearMatches() }
                ) else Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    com.kirox.app.ui.components.FusionIconButton(KiroXIcon.Back, "Закрыть", onClick = ::close)
                    Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                        val host = current.toUri().host.orEmpty()
                        Text(title.ifBlank { host }, color = Fusion.colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // A lock for https, a warning for plain http.
                            val secure = current.startsWith("https://")
                            Text(if (secure) "🔒 " else "⚠ ", fontSize = 11.sp, color = if (secure) Fusion.colors.textTertiary else Fusion.colors.danger)
                            Text(host, color = Fusion.colors.textTertiary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (tweaks.browserPrivate) Text(" · приватно", color = Fusion.colors.textTertiary, fontSize = 12.sp)
                            if (noImages) Text(" · без картинок", color = Fusion.colors.textTertiary, fontSize = 12.sp)
                        }
                    }
                    Box {
                        com.kirox.app.ui.components.FusionIconButton(KiroXIcon.More, "Ещё", onClick = { menu = true })
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = Fusion.colors.surface1) {
                            @Composable fun Item(label: String, onClick: () -> Unit) =
                                DropdownMenuItem(text = { Text(label, color = Fusion.colors.textPrimary) }, onClick = { menu = false; onClick() })
                            Item("Обновить") { webView?.reload() }
                            if (webView?.canGoForward() == true) Item("Вперёд") { webView?.goForward() }
                            Item("Найти на странице") { finding = true }
                            Item(if (desktop) "Мобильная версия" else "Версия для ПК") {
                                desktop = !desktop
                                webView?.settings?.apply {
                                    userAgentString = if (desktop) DESKTOP_AGENT else null
                                    useWideViewPort = desktop; loadWithOverviewMode = desktop
                                }
                                webView?.reload()
                            }
                            if (noImages) Item("Загрузить картинки") { webView?.settings?.blockNetworkImage = false }
                            Item("Поделиться") {
                                runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, current), null)) }
                            }
                            Item("Копировать ссылку") { clipboard.setText(AnnotatedString(current)); notice = "Ссылка скопирована" }
                            Item("Открыть в браузере") { openOutside() }
                        }
                    }
                }
                if (progress in 1..99) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(2.dp), color = Fusion.colors.accent, trackColor = Fusion.colors.background)
                else Spacer(Modifier.height(2.dp))
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = tweaks.browserJs
                            settings.domStorageEnabled = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.blockNetworkImage = noImages
                            settings.setGeolocationEnabled(false)
                            settings.mediaPlaybackRequiresUserGesture = true
                            if (tweaks.browserPrivate) settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, tweaks.browserThirdPartyCookies)
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                    val target = request.url
                                    return when {
                                        target.scheme == "tg" || target.host in setOf("t.me", "telegram.me", "telegram.dog") -> { onTelegramLink(target.toString()); true }
                                        target.scheme == "http" || target.scheme == "https" -> false
                                        // mailto:, intent:, market: and the like belong to other apps.
                                        else -> { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, target)) }; true }
                                    }
                                }
                                override fun onPageStarted(view: WebView, pageUrl: String, favicon: Bitmap?) { current = pageUrl; canBack = view.canGoBack() }
                                override fun doUpdateVisitedHistory(view: WebView, pageUrl: String, isReload: Boolean) { current = pageUrl; canBack = view.canGoBack() }
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView, newProgress: Int) { progress = newProgress }
                                override fun onReceivedTitle(view: WebView, pageTitle: String?) { title = pageTitle.orEmpty() }
                                override fun onShowCustomView(view: View, callback: CustomViewCallback) { customView = view; customViewCallback = callback }
                                override fun onHideCustomView() { customView = null; customViewCallback = null }
                                override fun onShowFileChooser(webView: WebView, callback: ValueCallback<Array<android.net.Uri>>, params: FileChooserParams): Boolean {
                                    fileCallback?.onReceiveValue(null)
                                    fileCallback = callback
                                    val type = params.acceptTypes.firstOrNull { it.isNotBlank() } ?: "*/*"
                                    runCatching { pickFiles.launch(type) }.onFailure { fileCallback = null; callback.onReceiveValue(null) }
                                    return true
                                }
                                // Camera, microphone and the like: always refused.
                                override fun onPermissionRequest(request: android.webkit.PermissionRequest) = request.deny()
                            }
                            setFindListener { active, count, _ -> findResult = (if (count == 0) 0 else active + 1) to count }
                            setDownloadListener { link, userAgent, disposition, mime, _ ->
                                val name = URLUtil.guessFileName(link, disposition, mime)
                                runCatching {
                                    val request = DownloadManager.Request(link.toUri())
                                        .setMimeType(mime)
                                        .addRequestHeader("User-Agent", userAgent)
                                        .addRequestHeader("Cookie", CookieManager.getInstance().getCookie(link).orEmpty())
                                        .setTitle(name)
                                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
                                    ctx.getSystemService(DownloadManager::class.java).enqueue(request)
                                }.onSuccess { notice = I18n.t("Скачивается: ") + name }.onFailure { notice = "Не удалось скачать файл" }
                            }
                            loadUrl(url)
                            webView = this
                        }
                    },
                    onRelease = { it.destroy(); webView = null },
                    modifier = Modifier.fillMaxSize()
                )
            }
            customView?.let { view ->
                AndroidView(
                    factory = { ctx -> FrameLayout(ctx).apply { setBackgroundColor(android.graphics.Color.BLACK); (view.parent as? android.view.ViewGroup)?.removeView(view); addView(view) } },
                    modifier = Modifier.fillMaxSize()
                )
            }
            FusionToast(toast)
        }
    }
}

/** «Найти на странице»: the query, «2 из 5», up / down, close. */
@Composable
private fun FindBar(text: String, result: Pair<Int, Int>, onText: (String) -> Unit, onNext: (Boolean) -> Unit, onClose: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
            KiroXIcon(KiroXIcon.Back, "Закрыть", Modifier.size(22.dp), Fusion.colors.textPrimary)
        }
        Box(Modifier.weight(1f).padding(horizontal = 4.dp)) {
            if (text.isEmpty()) Text("Найти на странице", color = Fusion.colors.textTertiary, fontSize = 16.sp)
            BasicTextField(
                text, onText, singleLine = true,
                textStyle = TextStyle(color = Fusion.colors.textPrimary, fontSize = 16.sp),
                cursorBrush = SolidColor(Fusion.colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onNext(true) }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
            )
        }
        if (text.isNotEmpty()) Text("${result.first} из ${result.second}", color = Fusion.colors.textSecondary, fontSize = 13.sp)
        listOf("▲" to false, "▼" to true).forEach { (label, forward) ->
            Box(Modifier.size(40.dp).clip(CircleShape).clickable { onNext(forward) }, contentAlignment = Alignment.Center) {
                Text(label, color = Fusion.colors.textPrimary, fontSize = 14.sp)
            }
        }
    }
}
