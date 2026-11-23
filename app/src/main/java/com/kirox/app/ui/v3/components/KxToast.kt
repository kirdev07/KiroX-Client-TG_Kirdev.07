package com.kirox.app.ui.v3.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.KxType
import kotlinx.coroutines.delay

/** Shows a short toast (kxToast: centred, bottom 110). Provided by [KxToastHost]. Errors are recognised by their words. */
val LocalKxToast = staticCompositionLocalOf<(String) -> Unit> { {} }

/** critique-C: toast kinds — Ok (green check), Info (neutral i), Error (red !). */
enum class KxToastKind { Ok, Info, Error }

/** Full toast: kind + optional action («Отменить»…). Provided by [KxToastHost]. */
val LocalKxToastEx = staticCompositionLocalOf<(text: String, kind: KxToastKind, action: String?, onAction: (() -> Unit)?) -> Unit> { { _, _, _, _ -> } }

/** Kind of a plain toast text: failures read as errors, the rest as done. */
fun kxToastKindOf(text: String): KxToastKind {
    val t = text.lowercase()
    return when {
        listOf("не удалось", "ошибка", "не получилось", "нет доступа", "не скачал", "нет сети", "нет соединения", "недоступ", "не найден").any { it in t } -> KxToastKind.Error
        else -> KxToastKind.Ok
    }
}

private data class ToastMsg(val text: String, val kind: KxToastKind, val action: String?, val onAction: (() -> Unit)?, val id: Long)

@Composable
fun KxToastHost(content: @Composable () -> Unit) {
    var msg by remember { mutableStateOf<ToastMsg?>(null) }
    var shown by remember { mutableStateOf(false) }
    val showEx: (String, KxToastKind, String?, (() -> Unit)?) -> Unit = remember { { t, k, a, f -> msg = ToastMsg(t, k, a, f, System.nanoTime()); shown = true } }
    val show: (String) -> Unit = remember { { t -> showEx(t, kxToastKindOf(t), null, null) } }
    // Long texts and toasts with an action stay longer (1.8 s → up to 4.5 s).
    LaunchedEffect(msg?.id) { val m = msg ?: return@LaunchedEffect; delay(if (m.action != null) 4500L else (1800L + (m.text.length - 30).coerceAtLeast(0) * 40L).coerceAtMost(4000L)); shown = false }
    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalKxToast provides show, LocalKxToastEx provides showEx, content = content)
        AnimatedVisibility(shown, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 110.dp),
            enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 3 }, exit = fadeOut(tween(250))) {
            msg?.let { m ->
                val (icon, tint) = when (m.kind) {
                    KxToastKind.Ok -> Icons.Rounded.CheckCircle to Kx.accent
                    KxToastKind.Info -> Icons.Rounded.Info to Kx.text2
                    KxToastKind.Error -> Icons.Rounded.ErrorOutline to Kx.danger
                }
                Row(Modifier.semantics { liveRegion = LiveRegionMode.Polite }.clip(RoundedCornerShape(18.dp)).background(Color(0xF228322D))
                    .border(1.dp, if (m.kind == KxToastKind.Error) Kx.danger.copy(alpha = .35f) else Kx.w(.12f), RoundedCornerShape(18.dp))
                    .padding(start = 16.dp, end = if (m.action != null) 6.dp else 16.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(icon, null, Modifier.size(18.dp), tint = tint)
                    Text(m.text, style = KxType.style(14.sp, FontWeight.SemiBold), modifier = Modifier.weight(1f, fill = false))
                    if (m.action != null && m.onAction != null) Text(m.action, style = KxType.style(14.sp, FontWeight.ExtraBold, Kx.accent),
                        modifier = Modifier.kxHitClick(m.action, RoundedCornerShape(12.dp)) { m.onAction.invoke(); shown = false }.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
        }
    }
}
