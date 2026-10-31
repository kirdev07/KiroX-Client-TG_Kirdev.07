package com.kirox.app.ui.home

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import com.kirox.app.i18n.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import com.kirox.app.notify.KeepConnectedService
import com.kirox.app.ui.icons.KiroXIcon
import com.kirox.app.ui.theme.Fusion

private const val PREFS = "kirox_ui"
private const val KEY_SEEN = "background_intro_seen"

private fun needsNotifications(context: Context) =
    Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

private fun needsBattery(context: Context) =
    context.getSystemService(android.os.PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == false

/** Shown once after sign-in while notifications or unrestricted background work are missing. */
internal fun shouldShowBackgroundIntro(context: Context) =
    !context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_SEEN, false) && (needsNotifications(context) || needsBattery(context))

/**
 * Says why KiroX asks for notifications and for work in the background before the system dialogs
 * appear: Telegram sends no pushes to KiroX unless a push app (ntfy) is installed, so KiroX keeps its
 * own connection while closed, and the system must not put it to sleep.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackgroundIntroSheet(onDone: () -> Unit) {
    val context = LocalContext.current
    fun finish() {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean(KEY_SEEN, true) }
        // Background mode starts right away (Auto turns it on when there is no push app).
        KeepConnectedService.start(context)
        onDone()
    }
    fun askBattery() {
        if (needsBattery(context)) runCatching {
            @android.annotation.SuppressLint("BatteryLife")
            val request = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:${context.packageName}".toUri())
            context.startActivity(request.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        finish()
    }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { askBattery() }
    ModalBottomSheet(onDismissRequest = { finish() }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Fusion.colors.surface1) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).background(Fusion.colors.accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                KiroXIcon(KiroXIcon.Bell, null, Modifier.size(30.dp), Fusion.colors.accent)
            }
            Spacer(Modifier.height(16.dp))
            Text("Чтобы сообщения приходили сразу", color = Fusion.colors.textPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Reason(KiroXIcon.Bell, "Уведомления", "Показывать новые сообщения и входящие звонки, когда kiroX свёрнут.")
            Reason(
                KiroXIcon.Refresh, "Работа в фоне",
                "Telegram присылает пуши только своему приложению. Поэтому kiroX сам держит связь, пока закрыт, — система не должна его усыплять. Батарея тратится немного, в шторке будет тихое «kiroX на связи»."
            )
            Text(
                "Сообщения и звонки не уходят никуда, кроме Telegram. Режим можно поменять в «Уведомления → Фоновое соединение».",
                color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 17.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "Разрешить", color = Fusion.colors.onAccent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Fusion.colors.accent).clickable {
                    if (needsNotifications(context)) notifications.launch(android.Manifest.permission.POST_NOTIFICATIONS) else askBattery()
                }.padding(vertical = 14.dp)
            )
            Text(
                "Позже", color = Fusion.colors.textSecondary, fontSize = 15.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).clickable { finish() }.padding(vertical = 14.dp)
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Reason(icon: KiroXIcon, title: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(Fusion.colors.surface2), contentAlignment = Alignment.Center) {
            KiroXIcon(icon, null, Modifier.size(20.dp), Fusion.colors.textPrimary)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Fusion.colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(text, color = Fusion.colors.textSecondary, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}
