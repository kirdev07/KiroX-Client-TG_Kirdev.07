@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.kirox.app.ui.home

import com.kirox.app.ui.components.fusionPanel
import androidx.compose.material3.MaterialTheme
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.BuildConfig
import com.kirox.app.telegram.TelegramContact
import com.kirox.app.telegram.TelegramUserProfile
import com.kirox.app.ui.components.SeasonalLayer
import com.kirox.app.ui.components.TelegramAvatar
import com.kirox.app.ui.format.TelegramFormat
import com.kirox.app.ui.icons.KiroXIcon
import com.kirox.app.ui.icons.KiroXIcon as IconGlyph
import com.kirox.app.ui.theme.Fusion
import com.kirox.app.ui.theme.FusionPalette
import kotlin.time.Duration.Companion.seconds

// Accounts on this device: switch, add, sign out.

/**
 * Accounts on this device, after the approved auth board (8 «Быстрое переключение», 7 «Добавить аккаунт»):
 * KiroX mark, accounts with the current one ticked, «Добавить аккаунт» and «Выйти». Only the chosen account is online.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun AccountsSheet(onDismiss: () -> Unit) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as com.kirox.app.KiroXApplication
    val client = app.telegram.client
    val accounts by client.accounts.accounts.collectAsState()
    val active = client.activeSession
    var adding by remember { mutableStateOf(false) }
    var confirmLogOut by remember { mutableStateOf(false) }
    val accent = Fusion.colors.accent
    val cardBase = Modifier.padding(horizontal = 14.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).fusionPanel(Fusion.colors, RoundedCornerShape(20.dp))
    fun card(): Modifier = cardBase
    if (confirmLogOut) AlertDialog(
        onDismissRequest = { confirmLogOut = false },
        containerColor = Fusion.colors.surface1,
        title = { Text("Выйти из аккаунта?", color = Fusion.colors.textPrimary) },
        text = { Text(if (accounts.size > 1) "kiroX переключится на другой аккаунт на этом устройстве." else "Чтобы вернуться, нужно будет снова войти по номеру или QR-коду.", color = Fusion.colors.textSecondary) },
        confirmButton = { TextButton(onClick = { confirmLogOut = false; onDismiss(); app.telegram.auth.logOut() }) { Text("Выйти", color = Fusion.colors.danger) } },
        dismissButton = { TextButton(onClick = { confirmLogOut = false }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    )
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss, containerColor = androidx.compose.ui.graphics.Color(0xFF0B0812)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            com.kirox.app.ui.auth.AuthMark(if (adding) KiroXIcon.Add else KiroXIcon.Send, Modifier.size(56.dp))
            Text(if (adding) "Добавить аккаунт" else "kiroX", color = Fusion.colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        }
        if (adding) {
            // Ways in for a new account; both open the sign-in of a fresh slot.
            listOf(
                Triple(KiroXIcon.Phone, "Существующий номер", "Войти в другой аккаунт") to false,
                Triple(KiroXIcon.Qr, "QR-код", "Быстрый вход с другого устройства") to true
            ).forEach { (texts, qr) ->
                Row(
                    card().clickable {
                        onDismiss()
                        client.addAccount()
                        if (qr) kotlinx.coroutines.MainScope().launch {
                            // Ask for the QR once the new slot reaches its phone-number step.
                            kotlinx.coroutines.withTimeoutOrNull(20.seconds) { app.telegram.auth.state.first<com.kirox.app.telegram.TelegramAuthState> { it == com.kirox.app.telegram.TelegramAuthState.WaitPhoneNumber } }
                                ?.let { app.telegram.auth.requestQr() }
                        }
                    }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                        IconGlyph(texts.first, null, Modifier.size(22.dp), accent)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(texts.second, color = Fusion.colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(texts.third, color = Fusion.colors.textSecondary, fontSize = 13.sp)
                    }
                    IconGlyph(KiroXIcon.Chevron, null, Modifier.size(18.dp), Fusion.colors.textTertiary)
                }
            }
            TextButton(onClick = { adding = false }, modifier = Modifier.fillMaxWidth()) { Text("Назад", color = Fusion.colors.textSecondary) }
        } else {
            Column(card()) {
                accounts.forEach { account ->
                    val current = account.session == active
                    Row(
                        Modifier.fillMaxWidth().clickable { onDismiss(); if (!current) client.switchTo(account.session) }
                            .background(if (current) accent.copy(alpha = 0.10f) else androidx.compose.ui.graphics.Color.Transparent).padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TelegramAvatar(account.title.ifBlank { "?" }, null, 44.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(account.title.ifBlank { "Вход не завершён" }, color = Fusion.colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (account.phone.isNotBlank()) Text("+" + account.phone.trimStart('+'), color = Fusion.colors.textSecondary, fontSize = 13.sp)
                        }
                        if (current) IconGlyph(KiroXIcon.Check, "Выбран", Modifier.size(20.dp), accent)
                    }
                }
                if (accounts.size < MAX_ACCOUNTS) Row(
                    Modifier.fillMaxWidth().clickable { adding = true }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { IconGlyph(KiroXIcon.Add, null, Modifier.size(22.dp), Fusion.colors.textPrimary) }
                    Spacer(Modifier.width(12.dp))
                    Text("Добавить аккаунт", color = Fusion.colors.textPrimary, fontSize = 16.sp)
                }
            }
            Row(card().clickable { confirmLogOut = true }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { IconGlyph(KiroXIcon.Back, null, Modifier.size(22.dp), Fusion.colors.danger) }
                Spacer(Modifier.width(12.dp))
                Text("Выйти", color = Fusion.colors.danger, fontSize = 16.sp)
            }
            Text(
                "Уведомления приходят только от выбранного аккаунта.",
                color = Fusion.colors.textTertiary, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }
        Spacer(Modifier.navigationBarsPadding().height(8.dp))
    }
}

private const val MAX_ACCOUNTS = 3
