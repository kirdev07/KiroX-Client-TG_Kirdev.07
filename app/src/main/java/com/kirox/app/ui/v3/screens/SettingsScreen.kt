package com.kirox.app.ui.v3.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.KxStore
import com.kirox.app.ui.v3.model.pref
import com.kirox.app.ui.v3.theme.*

/** Settings tab (board "Настройки"): profile card, Premium + gifts, grouped rows with captions, ⋮ menu. */
@Composable
fun SettingsScreen(store: KxStore, onOpen: (String) -> Unit, bottomPadding: Dp = kxBottomContentPadding()) {
    var menu by remember { mutableStateOf(false) }
    val me = store.me
    Box(Modifier.fillMaxSize()) {
        KxGlow(diameter = 380.dp, x = (-90).dp, y = (-110).dp, alpha = .20f, alignEnd = true)
        // qa-real: the title row stays put under the status bar (it scrolled under the clock and could not be tapped).
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp).kxRise(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Настройки", style = KxType.style(30.sp, FontWeight.ExtraBold).copy(letterSpacing = (-.4).sp), modifier = Modifier.weight(1f))
                GlassRound(Icons.Rounded.Search, "Поиск настроек") { onOpen("SettingsSearch") }
                GlassRound(Icons.Rounded.MoreVert, "Ещё") { menu = !menu }
            }
        Column(Modifier.weight(1f).fillMaxWidth().clipToBounds().verticalScroll(rememberScrollState()).padding(bottom = bottomPadding)) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileCard(store, onOpen)
                // design-critique B11–B14/A1/A3: one list of grouped cards with captions; tiles by group colour
                // (security = green, look = purple, data = blue, kiroX = green, help = neutral); no 2-column tile grid.
                Column(Modifier.kxRise(180)) {
                    KxGroupCaption("Аккаунт и безопасность")
                    KxGroup {
                        SetRow(Icons.Rounded.Notifications, "Уведомления и звуки", KxTint.security) { onOpen("Notify") }
                        SetRow(Icons.Rounded.Lock, "Конфиденциальность", KxTint.security) { onOpen("Privacy") }
                        SetRow(Icons.Rounded.Shield, "Защита kiroX", KxTint.security) { onOpen("Shield") } // shield: hub entry
                    }
                    KxGroupCaption("Приложение", Modifier.padding(top = 8.dp))
                    KxGroup {
                        SetRow(Icons.Rounded.Storage, "Данные и память", KxTint.data, store.storage?.totalBytes?.let { com.kirox.app.ui.v3.screens.settings.sBytes(it) }) { onOpen("Storage") }
                        SetRow(Icons.Rounded.Palette, "Оформление", KxTint.look, com.kirox.app.ui.v3.screens.settings.CustomThemes[store.pref(com.kirox.app.ui.v3.model.KxPrefs.THEME).coerceIn(0, com.kirox.app.ui.v3.screens.settings.CustomThemes.lastIndex)].name) { onOpen("Custom") }
                        // qa-real: «Язык» hidden in 1.0 (Russian only); «Специальные возможности» gets its own row.
                        SetRow(Icons.Rounded.Accessibility, "Специальные возможности", KxTint.look) { onOpen("Access") }
                    }
                }
                // critique-A: Premium + Подарки come after the main settings (not above them).
                Row(Modifier.kxRise(120), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PremiumButton(Modifier.weight(1.4f)) { onOpen("Premium") }
                    Row(Modifier.weight(1f).height(64.dp).clip(RoundedCornerShape(22.dp)).background(Kx.w(.05f)).border(1.dp, Kx.fillHi, RoundedCornerShape(22.dp))
                        .kxClick { onOpen("Gifts") }.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.Redeem, null, tint = Color(0xFFF2A6C0))
                        Column { Text("Подарки", style = KxType.style(15.sp, FontWeight.Bold)); Text("За звёзды", style = KxType.caption) }
                    }
                }
                com.kirox.app.ui.v3.features.KxFeatureSettingsSection(store, onOpen) // features: «Функции kiroX» group (+ Музыка, Режим призрака)
                Column(Modifier.kxRise(300)) {
                    KxGroupCaption("kiroX")
                    KxGroup {
                        SetRow(Icons.Rounded.HelpOutline, "Помощь", KxTint.neutral) { onOpen("Help") }
                        SetRow(androidx.compose.material.icons.Icons.Outlined.Info, "О приложении", KxTint.neutral, com.kirox.app.BuildConfig.VERSION_NAME.substringBefore('-')) { onOpen("WhatsNew") }
                    }
                }
            }
        }
        }
        if (menu) {
            Box(Modifier.fillMaxSize().kxClick("Закрыть меню") { menu = false })
            Column(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 58.dp, end = 16.dp).width(210.dp)
                .clip(RoundedCornerShape(20.dp)).background(Brush.verticalGradient(listOf(Color(0xF22E3A34), Color(0xF5181E1B)))).border(1.dp, Kx.stroke, RoundedCornerShape(20.dp)).topHighlight(Kx.w(.22f)).padding(6.dp)) {
                listOf(androidx.compose.material.icons.Icons.Outlined.AccountCircle to "Мой профиль", androidx.compose.material.icons.Icons.Outlined.SwitchAccount to "Аккаунты", androidx.compose.material.icons.Icons.Outlined.Contacts to "Контакты",
                    androidx.compose.material.icons.Icons.Outlined.Devices to "Устройства", androidx.compose.material.icons.Icons.Outlined.Folder to "Чаты и папки", androidx.compose.material.icons.Icons.Outlined.VisibilityOff to "Скрытые чаты", androidx.compose.material.icons.Icons.Outlined.AutoStories to "Архив историй", androidx.compose.material.icons.Icons.AutoMirrored.Outlined.Logout to "Выйти").forEach { (ic, t) ->
                    Row(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(14.dp)).kxClick { menu = false; onOpen(mapOf("Мой профиль" to "EditProfile", "Аккаунты" to "Accounts", "Контакты" to "Contacts", "Устройства" to "Devices", "Чаты и папки" to "Folders", "Скрытые чаты" to "Hidden", "Архив историй" to "StoryArchive", "Выйти" to "Logout")[t] ?: t) }.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(ic, null, Modifier.size(20.dp), tint = Kx.accent); Text(t, style = KxType.style(15.sp))
                    }
                }
            }
        }
    }
}

/** One Settings row (inside a KxGroup): tinted icon tile, title, value that drops under the title when short, chevron. */
@Composable
private fun SetRow(icon: ImageVector, title: String, tint: Color, value: String? = null, onClick: () -> Unit) =
    com.kirox.app.ui.v3.screens.settings.SNavRow(icon, title, null, RoundedCornerShape(0.dp), tint = tint, value = value, onClick = onClick)

@Composable
private fun GlassRound(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(Modifier.kxHitClick(label, onClick = onClick).size(42.dp).clip(CircleShape).background(Kx.fill).border(1.dp, Kx.w(.09f), CircleShape).topHighlight(Kx.w(.14f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(22.dp), tint = Kx.text2)
    }
}

@Composable
private fun ProfileCard(store: KxStore, onOpen: (String) -> Unit) {
    val me = store.me
    val shape = RoundedCornerShape(28.dp)
    Column(Modifier.kxRise(60).fillMaxWidth().clip(shape)
        .background(Brush.linearGradient(0f to Color(0x473F9A6C), .6f to Color(0xE6121815), start = Offset.Zero, end = Offset(900f, 700f)))
        .border(1.dp, Kx.accent.copy(alpha = .18f), shape).topHighlight(Kx.w(.12f)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box {
                KxAvatar(me.name, me.id, size = 70.dp, swatch = KxAvatarPalette.Swatch(Color(0xFF3E7A5A), Color(0xFF173A28), Color(0xFFDDF5E5)), letterWeight = FontWeight.ExtraBold, letterSize = 26.sp, photo = me.avatar)
                Box(Modifier.align(Alignment.BottomEnd).offset(5.dp, 5.dp).size(32.dp).clip(CircleShape).background(Color(0xFF14201A)).padding(3.dp).clip(CircleShape).background(Kx.accent)
                    .kxClick("Сменить фото") { onOpen("Avatars") }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PhotoCamera, null, Modifier.size(14.dp), tint = Kx.onAccent)
                }
            }
            Column(Modifier.weight(1f)) {
                KxLine(me.name, KxType.style(20.sp, FontWeight.Bold))
                // B13: username and phone each on their own line (no «@demon6…» cut).
                if (me.username.isNotEmpty()) KxLine("@${me.username}", KxType.style(14.sp, FontWeight.SemiBold, Kx.accent))
                if (me.phone.isNotEmpty()) KxLine(me.phone, KxType.style(13.sp, color = Color(0xFFA6B5AC)))
            }
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = .25f)).kxClick("Мой QR-код") { onOpen("MyQr") }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.QrCode2, null, tint = Color(0xFFC8F0D8))
            }
        }
        Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Large system font: the «Изменить профиль» chip gives way (ellipsis) so the account chips stay whole.
            Box(Modifier.weight(1f)) { Row(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.10f)).border(1.dp, Kx.w(.12f), RoundedCornerShape(18.dp))
                .kxClick { onOpen("EditProfile") }.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.Edit, null, Modifier.size(18.dp), tint = Kx.text)
                Text("Изменить профиль", style = KxType.style(13.sp, FontWeight.SemiBold), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            } }
            // critique-A: no «Аккаунты» caption here — it squeezed «Изменить профиль» to «Изменить проф…» (the avatars keep the label for TalkBack).
            Row(Modifier.padding(start = 2.dp).kxClick("Аккаунты") { onOpen("Accounts") }) {
                store.accounts.forEachIndexed { i, a ->
                    val sw = if (i == 0) KxAvatarPalette.Swatch(Color(0xFF3E7A5A), Color(0xFF173A28), Color(0xFFDDF5E5)) else KxAvatarPalette.Swatch(Color(0xFF3C7BD0), Color(0xFF142A4A), Color(0xFFD6E6FF))
                    KxAvatar(a.name, a.id, Modifier.offset(x = (-8 * i).dp).border(2.dp, Color(0xFF14201A), CircleShape), size = 30.dp, swatch = sw, letterWeight = FontWeight.ExtraBold, letterSize = 12.sp, photo = a.avatar)
                }
                Box(Modifier.offset(x = (-8 * store.accounts.size).dp).size(30.dp).clip(CircleShape).background(Color(0xFF26302B)).border(2.dp, Color(0xFF14201A), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Add, "Добавить аккаунт", Modifier.size(16.dp), tint = Kx.text2)
                }
            }
        }
    }
}

@Composable
private fun PremiumButton(modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val t = rememberInfiniteTransition(label = "sheen")
    val reduced = LocalKxReducedMotion.current
    // kxSheen runs twice then rests (decorative animations are not endless).
    val sheen = remember { Animatable(-1.4f) }
    LaunchedEffect(reduced) { if (!reduced) repeat(2) { sheen.snapTo(-1.4f); sheen.animateTo(2.6f, tween(1600, easing = LinearEasing)); kotlinx.coroutines.delay(1300) } }
    @Suppress("UNUSED_VARIABLE") val unused = t
    Box(modifier.height(64.dp).clip(shape).background(Brush.linearGradient(0f to Color(0xFF6E58C9), .6f to Color(0xFF3E2F7A), 1f to Color(0xFF2A6B4F))).kxClick(onClick = onClick)) {
        Box(Modifier.width(40.dp).fillMaxHeight().graphicsLayer { translationX = sheen.value * 40.dp.toPx() }
            .background(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = .28f), Color.Transparent))))
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Star, null, tint = Color(0xFFFFE38A))
            Column { Text("Premium", style = KxType.style(15.sp, FontWeight.Bold)); Text("Статусы, подарки", style = KxType.style(12.sp, color = Color(0xFFE2DAFF))) }
        }
    }
}
