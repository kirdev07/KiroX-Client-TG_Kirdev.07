package com.kirox.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.icons.KiroXIcon
import com.kirox.app.ui.theme.Fusion
import kotlinx.coroutines.launch

/** A page shows [icon], or the kiroX logo when it is null. */
private data class WelcomePage(val icon: KiroXIcon?, val title: String, val text: String)

private val pages = listOf(
    WelcomePage(null, "Добро пожаловать\nв kiroX", "Быстрый, удобный и красивый\nнеофициальный клиент Telegram"),
    WelcomePage(KiroXIcon.Lock, "Ваши данные\nв безопасности", "Вход через Telegram, база на телефоне\nзашифрована"),
    WelcomePage(KiroXIcon.Palette, "Свой стиль", "Темы Expressive и Glass, обои\nи праздничные сцены"),
    WelcomePage(KiroXIcon.Contacts, "Несколько\nаккаунтов", "До трёх аккаунтов и быстрое\nпереключение между ними")
)

/** «Приветствие» from the approved auth board, shown once before the first sign-in. */
@Composable
internal fun WelcomeScreen(onContinue: () -> Unit) {
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().authBackdrop().statusBarsPadding().navigationBarsPadding()) {
        HorizontalPager(pager, Modifier.weight(1f)) { index ->
            val page = pages[index]
            Column(Modifier.fillMaxSize().padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                if (page.icon != null) AuthMark(page.icon, Modifier.size(112.dp))
                else AuthLogo(Modifier.size(112.dp))
                Spacer(Modifier.height(28.dp))
                Text(page.title, color = Fusion.colors.textPrimary, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(page.text, color = Fusion.colors.textSecondary, fontSize = 16.sp, lineHeight = 22.sp, textAlign = TextAlign.Center)
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.Center) {
            repeat(pages.size) { i ->
                val selected = i == pager.currentPage
                Box(Modifier.padding(horizontal = 4.dp).size(if (selected) 10.dp else 8.dp).clip(CircleShape).background(if (selected) Fusion.colors.accent else Color.White.copy(alpha = 0.25f)))
            }
        }
        Box(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            val last = pager.currentPage == pages.lastIndex
            AuthGradientButton(if (last) "Начать" else "Продолжить", enabled = true) {
                if (last) onContinue() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            }
        }
        AuthLink("Пропустить", onClick = onContinue)
        Text(
            "Неофициальное приложение, не связано с Telegram",
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            color = Fusion.colors.textTertiary, fontSize = 12.sp, textAlign = TextAlign.Center
        )
    }
}
