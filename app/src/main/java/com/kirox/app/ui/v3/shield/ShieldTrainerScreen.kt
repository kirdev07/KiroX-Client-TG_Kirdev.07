package com.kirox.app.ui.v3.shield

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.security.shield.ShieldContext
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.screens.tools.*
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.KxType

/** A made-up example for the trainer (never a real message). */
private data class TrainerCard(val from: String, val text: String, val scam: Boolean, val lesson: String)

private val CARDS = listOf(
    TrainerCard("Служба поддержки Telegram", "Ваш аккаунт будет удалён через 24 часа за нарушение правил. Чтобы подтвердить аккаунт, пришлите код, который придёт вам в SMS.", true,
        "Telegram никогда не пишет о блокировке в личку и не просит код. Код = ключ от аккаунта."),
    TrainerCard("Айгерим (одноклассница)", "Привет! Проголосуй, пожалуйста, за мою племянницу в конкурсе рисунков, там надо войти через телеграм: vote-kids-konkurs.top", true,
        "«Голосование» с входом через Telegram — способ украсть аккаунт. Адрес на странном сайте .top."),
    TrainerCard("Мама", "Купи по дороге хлеб и молоко, пожалуйста. И позвони бабушке, она спрашивала про выходные.", false,
        "Обычная бытовая просьба: нет денег, кодов, ссылок и спешки."),
    TrainerCard("Неизвестный номер", "Мам, это я, у меня телефон разбился, пишу с нового номера. Срочно нужно 25 000 ₸, скинь на эту карту 4400 4301 2345 6789, потом всё объясню", true,
        "«Сменил номер» + срочно + деньги на чужую карту. Позвоните на старый номер."),
    TrainerCard("Kaspi Gold", "Ваш перевод 5 000 ₸ от Ерлан А. зачислен. Баланс можно посмотреть в приложении.", false,
        "Уведомление без просьб: ничего не нужно делать, ссылок и кодов нет."),
    TrainerCard("Crypto Signals VIP", "Гарантированный доход 30% в неделю! Вложи от 100 USDT, наш бот удвоит депозит без риска. Пиши в лс, мест мало", true,
        "Гарантий дохода не бывает. «Удвоим», «без риска», «мест мало» — признаки пирамиды."),
    TrainerCard("Коллега Данияр", "Скинь, пожалуйста, презентацию к завтрашней встрече, хочу посмотреть слайды заранее.", false,
        "Рабочая просьба про файл — без денег и кодов."),
    TrainerCard("Служба безопасности банка", "Зафиксирована подозрительная операция по вашей карте. Для отмены назовите номер карты, срок действия и три цифры на обороте.", true,
        "Банк не просит CVV и срок действия. Положите трубку и позвоните по номеру на карте."),
    TrainerCard("Друг Тимур", "Ты где? Мы уже у кинотеатра, начало в 19:30, билеты я купил. Возьми попкорн по дороге 🙂", false,
        "Обычная переписка друзей."),
    TrainerCard("Менеджер возврата", "Чтобы вернуть деньги за заказ, установите AnyDesk и продиктуйте код с экрана, я всё сделаю за вас.", true,
        "AnyDesk даёт чужому человеку управлять телефоном. Возврат так не делают."),
)

/** «Найди мошенника»: 10 made-up messages, the user guesses, then sees the answer and kiroX's score. */
@Composable
fun ShieldTrainerScreen(ctx: KxScreenCtx) {
    val c = LocalContext.current
    LaunchedEffect(Unit) { KxShield.init(c) }
    val acc = ctx.store.shieldAccount()
    val cards = remember { CARDS.shuffled() }
    var index by remember { mutableIntStateOf(0) }
    var answer by remember { mutableStateOf<Boolean?>(null) }
    var right by remember { mutableIntStateOf(0) }
    TScreen("Найди мошенника", ctx.back, sub = if (index < cards.size) "Пример ${index + 1} из ${cards.size}" else "Готово") {
        if (index >= cards.size) {
            Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.EmojiEvents, null, Modifier.size(64.dp), tint = Kx.accent)
                Text("$right из ${cards.size}", style = KxType.style(40.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.padding(top = 12.dp))
                Text(when { right == cards.size -> "Отлично: вас не обмануть."; right >= 7 -> "Хорошо! Помните про коды, карты и «срочно»."; else -> "Мошенники хитрые. Главное правило: код, пароль и данные карты — никому." },
                    style = KxType.style(15.sp, color = Kx.text2, lineHeight = 21.sp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                TPill("Ещё раз", Modifier.padding(top = 24.dp).fillMaxWidth(), icon = Icons.Rounded.Replay) { index = 0; right = 0; answer = null }
            }
            return@TScreen
        }
        val card = cards[index]
        // critique-B: progress dots for the 10 examples.
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(cards.size) { i ->
                Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(if (i < index) Kx.accent else if (i == index) Kx.accent.copy(alpha = .55f) else Kx.w(.1f)))
            }
        }
        THint("Это вымышленные примеры для тренировки. Ваши ответы модель не учит.", top = 0.dp)
        // The example as a chat bubble.
        Column(Modifier.padding(top = 14.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(28.dp).clip(RoundedCornerShape(14.dp)).background(Kx.w(.10f)), contentAlignment = Alignment.Center) { Text(card.from.take(1), style = KxType.style(13.sp, FontWeight.Bold)) }
                Text(card.from, style = KxType.style(13.sp, FontWeight.Bold, Kx.text2))
                Text("пример", style = KxType.style(11.sp, FontWeight.Bold, Kx.warning), modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Kx.warning.copy(alpha = .12f)).padding(horizontal = 6.dp, vertical = 2.dp))
            }
            Text(card.text, style = KxType.style(15.sp, lineHeight = 21.sp), modifier = Modifier.padding(top = 8.dp, end = 30.dp)
                .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)).background(Color(0xFF1B211E)).border(1.dp, Kx.w(.06f), RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)).padding(12.dp))
        }
        val a = answer
        if (a == null) {
            Text("Как думаете?", style = KxType.style(15.sp, FontWeight.Bold), modifier = Modifier.padding(top = 22.dp))
            // critique-B: two equal neutral buttons (a red «Мошенник» primed the answer) and «Пропустить».
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TPill("Нормально", Modifier.weight(1f), h = 56.dp, bg = solid(Kx.w(.08f)), ink = Kx.text, weight = FontWeight.Bold) { answer = false; if (!card.scam) right++ }
                TPill("Мошенник", Modifier.weight(1f), h = 56.dp, bg = solid(Kx.w(.08f)), ink = Kx.text, weight = FontWeight.Bold) { answer = true; if (card.scam) right++ }
            }
            Text("Пропустить", style = KxType.style(14.sp, FontWeight.Bold, Kx.text3), modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp)
                .clip(RoundedCornerShape(14.dp)).kxClick("Пропустить пример") { index++ }.padding(horizontal = 16.dp, vertical = 10.dp))
        } else {
            val ok = a == card.scam
            val verdict = remember(card, KxShield.ready) { KxShield.score(acc, card.text, ShieldContext(stranger = true)) }
            Column(Modifier.padding(top = 18.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(TTile).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel, null, tint = if (ok) Kx.accent else Kx.danger)
                    Text((if (ok) "Верно! " else "Не совсем. ") + if (card.scam) "Это мошенник" else "Это нормальное сообщение", style = KxType.style(16.sp, FontWeight.ExtraBold))
                }
                Text(card.lesson, style = KxType.style(14.sp, color = Kx.text2, lineHeight = 20.sp), modifier = Modifier.padding(top = 6.dp))
                Text("Оценка kiroX", style = KxType.style(12.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(top = 14.dp))
                RiskBar(verdict, Modifier.padding(top = 4.dp))
                if (verdict.reasons.isNotEmpty()) Text("Признаки: " + verdict.reasons.take(3).joinToString(", "), style = KxType.style(13.sp, color = Kx.text3, lineHeight = 18.sp), modifier = Modifier.padding(top = 6.dp))
            }
            TPill(if (index == cards.lastIndex) "Результат" else "Дальше", Modifier.padding(top = 16.dp).fillMaxWidth(), icon = Icons.Rounded.ArrowForward) { index++; answer = null }
        }
    }
}
