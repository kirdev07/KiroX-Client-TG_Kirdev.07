package com.kirox.app.ui.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.kirox.app.BuildConfig
import com.kirox.app.i18n.Text
import com.kirox.app.ui.icons.KiroXIcon
import com.kirox.app.ui.theme.Fusion

/** One line of «Что нового»: an icon, a title and what it does. */
private data class NewThing(val icon: KiroXIcon, val title: String, val text: String)

/** What this version brings; written for each release. */
private val CHANGES = listOf(
    NewThing(KiroXIcon.Music, "Музыка", "Вся музыка из чатов, плейлисты и «Любимое», полноэкранный плеер с очередью, повтором и таймером сна"),
    NewThing(KiroXIcon.Video, "Онлайн-воспроизведение", "Видео и музыка играют сразу, пока скачиваются; двойное касание перематывает видео на 10 секунд"),
    NewThing(KiroXIcon.File, "Файлы Telegram", "Всё скачанное в одном месте: PDF, документы, архивы, APK открываются прямо в kiroX"),
    NewThing(KiroXIcon.Shield, "Проверка файлов", "Перед открытием kiroX ищет замаскированные программы, макросы и опасные архивы"),
    NewThing(KiroXIcon.Link, "Встроенный браузер", "Ссылки открываются внутри kiroX: приватный режим, экономия трафика, поиск на странице"),
    NewThing(KiroXIcon.Eye, "Фильтры сообщений", "Прячьте в группах сообщения с нужными словами или от выбранных людей"),
    NewThing(KiroXIcon.Lock, "Графический ключ", "kiroX можно закрыть не только PIN-кодом, но и графическим ключом"),
    NewThing(KiroXIcon.Refresh, "Энергосбережение", "Вместе с экономией заряда телефона kiroX отключает анимации и тяжёлые эффекты")
)

/**
 * «Что нового»: shown once after kiroX is updated (not on the first install) and from «О приложении».
 * The version that was last shown is kept on the phone.
 */
object WhatsNew {
    private const val PREFS = "whats_new"
    private const val KEY = "seen_version"

    /** True right after an update; remembers the current version so it is shown once. */
    fun shouldShow(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val seen = prefs.getInt(KEY, 0)
        val current = BuildConfig.VERSION_CODE
        if (seen == current) return false
        prefs.edit { putInt(KEY, current) }
        // First install: nothing is «new» yet.
        return seen != 0 && seen < current
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Fusion.colors.surface1) {
        LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 16.dp)) {
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                    Text("Что нового в kiroX", color = Fusion.colors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Версия ${BuildConfig.VERSION_NAME}", color = Fusion.colors.textTertiary, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
            items(CHANGES) { thing ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Fusion.colors.accent.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                        KiroXIcon(thing.icon, null, Modifier.size(20.dp), Fusion.colors.accent)
                    }
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(thing.title, color = Fusion.colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(thing.text, color = Fusion.colors.textSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
            }
            item {
                com.kirox.app.ui.components.FusionButton("Понятно", onClick = onDismiss, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).navigationBarsPadding())
            }
        }
    }
}
