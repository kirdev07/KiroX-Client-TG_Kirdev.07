package com.kirox.app.ui.chat

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.net.toUri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kirox.app.ui.icons.KiroXIcon
import com.kirox.app.ui.theme.Fusion

/**
 * «Встроенный браузер»: a web link from a chat opens over kiroX instead of the browser app. Back
 * goes back in the page history and closes on the first page; Telegram links go to [onTelegramLink].
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun InAppBrowser(url: String, onTelegramLink: (String) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var title by remember { mutableStateOf("") }
    var current by remember { mutableStateOf(url) }
    var progress by remember { mutableIntStateOf(0) }
    var canBack by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    fun openOutside() { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, current.toUri())) }; onClose() }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler { if (canBack) webView?.goBack() else onClose() }
        Column(Modifier.fillMaxSize().background(Fusion.colors.background).systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                    KiroXIcon(KiroXIcon.Back, "Закрыть", Modifier.size(22.dp), Fusion.colors.textPrimary)
                }
                Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    Text(title.ifBlank { current.toUri().host.orEmpty() }, color = Fusion.colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(current.toUri().host.orEmpty(), color = Fusion.colors.textTertiary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box {
                    Box(Modifier.size(44.dp).clip(CircleShape).clickable { menu = true }, contentAlignment = Alignment.Center) {
                        KiroXIcon(KiroXIcon.More, "Ещё", Modifier.size(22.dp), Fusion.colors.textPrimary)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = Fusion.colors.surface1) {
                        DropdownMenuItem(text = { Text("Обновить", color = Fusion.colors.textPrimary) }, onClick = { menu = false; webView?.reload() })
                        if (webView?.canGoForward() == true) DropdownMenuItem(text = { Text("Вперёд", color = Fusion.colors.textPrimary) }, onClick = { menu = false; webView?.goForward() })
                        DropdownMenuItem(text = { Text("Копировать ссылку", color = Fusion.colors.textPrimary) }, onClick = { menu = false; clipboard.setText(AnnotatedString(current)) })
                        DropdownMenuItem(text = { Text("Открыть в браузере", color = Fusion.colors.textPrimary) }, onClick = { menu = false; openOutside() })
                    }
                }
            }
            if (progress in 1..99) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(2.dp), color = Fusion.colors.accent, trackColor = Fusion.colors.background)
            else Spacer(Modifier.height(2.dp))
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
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
                        }
                        loadUrl(url)
                        webView = this
                    }
                },
                onRelease = { it.destroy(); webView = null },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
