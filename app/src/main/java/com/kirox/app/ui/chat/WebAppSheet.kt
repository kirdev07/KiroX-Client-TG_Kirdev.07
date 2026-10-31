package com.kirox.app.ui.chat

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kirox.app.ui.icons.KiroXIcon
import com.kirox.app.ui.theme.Fusion
import org.json.JSONObject

/**
 * A bot's mini app in a full-screen web view. Telegram's web-app events the page sends through
 * window.TelegramWebviewProxy are understood for closing and opening links; the rest is ignored.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun WebAppSheet(title: String, url: String, onLink: (String) -> Unit, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler(onBack = onClose)
        val main = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
        Column(Modifier.fillMaxSize().background(Fusion.colors.background).systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                    KiroXIcon(KiroXIcon.Back, "Закрыть", Modifier.size(22.dp), Fusion.colors.textPrimary)
                }
                androidx.compose.material3.Text(title, color = Fusion.colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        webViewClient = object : WebViewClient() {
                            // Links to Telegram open in kiroX; the mini app itself stays in the view.
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val target = request.url.toString()
                                if (target.startsWith("tg:") || request.url.host in setOf("t.me", "telegram.me")) { onLink(target); return true }
                                return false
                            }
                        }
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun postEvent(type: String, data: String?) {
                                val payload = runCatching { JSONObject(data ?: "{}") }.getOrDefault(JSONObject())
                                main.post {
                                    when (type) {
                                        "web_app_close" -> onClose()
                                        "web_app_open_link" -> payload.optString("url").takeIf { it.isNotBlank() }?.let(onLink)
                                        "web_app_open_tg_link" -> payload.optString("path_full").takeIf { it.isNotBlank() }?.let { onLink("https://t.me$it") }
                                    }
                                }
                            }
                        }, "TelegramWebviewProxy")
                        loadUrl(url)
                    }
                },
                onRelease = { it.destroy() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
