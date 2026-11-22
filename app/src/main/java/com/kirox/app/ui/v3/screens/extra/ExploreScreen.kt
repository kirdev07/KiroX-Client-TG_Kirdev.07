package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

private val ExpCats = listOf(
    Triple("all", Icons.Outlined.Apps, "Все"), Triple("tech", Icons.Outlined.Devices, "Технологии"), Triple("news", Icons.Outlined.Newspaper, "Новости"),
    Triple("edu", Icons.Outlined.School, "Учёба"), Triple("fun", Icons.Outlined.Mood, "Юмор"), Triple("food", Icons.Outlined.Restaurant, "Еда"),
    Triple("travel", Icons.Outlined.Flight, "Путешествия"),
)
private class ExpCh(val name: String, val cat: String, val icon: ImageVector, val a: Color, val b: Color, val desc: String, val subs: Int, val v: Boolean = false, val g: Boolean = false)
// demo: needs store (channel recommendations / search of public chats + join)
private val ExpList = listOf(
    ExpCh("Android изнутри", "tech", Icons.Rounded.Android, Kx.accentDeep, Color(0xFF1F4D36), "Разбираем новинки и баги системы", 120400, v = true),
    ExpCh("Kotlin-клуб", "tech", Icons.Rounded.Code, Color(0xFF6A4C9C), Color(0xFF2A2240), "Группа для вопросов и код-ревью", 18200, g = true),
    ExpCh("Главное за день", "news", Icons.Rounded.Newspaper, Color(0xFF2F6E9E), Color(0xFF14243A), "Пять новостей каждое утро", 540000, v = true),
    ExpCh("Английский за 5 минут", "edu", Icons.Rounded.Translate, Color(0xFFB07A2E), Color(0xFF3A2A14), "Одна фраза и пример в день", 76100),
    ExpCh("Мемы про код", "fun", Icons.Rounded.SentimentVerySatisfied, Color(0xFFA8456A), Color(0xFF2E1420), "Когда тесты прошли с первого раза", 230000),
    ExpCh("Быстрые рецепты", "food", Icons.Rounded.SoupKitchen, Color(0xFFC98A3A), Color(0xFF6E4214), "Ужин за 20 минут", 98300),
    ExpCh("Куда поехать", "travel", Icons.Rounded.TravelExplore, Color(0xFF2E8A86), Color(0xFF143A38), "Недорогие маршруты и лайфхаки", 64500),
    ExpCh("Физика для всех", "edu", Icons.Rounded.Science, Color(0xFF3A6E7A), Color(0xFF14303A), "Сложное — понятным языком", 41200),
)

/** Board «Каналы и группы» (discover): featured card, category chips, list with subscribe / join toggles, search. */
@Composable
fun ExploreScreen(ctx: KxScreenCtx) {
    var q by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("all") }
    val joined = remember { mutableStateListOf<Int>() }
    var feat by remember { mutableStateOf(false) }
    var popI by remember { mutableIntStateOf(-1) }
    var popN by remember { mutableIntStateOf(0) }
    val qq = q.trim().lowercase()
    val list = ExpList.withIndex().filter { (_, c) -> (cat == "all" || c.cat == cat) && (qq.isEmpty() || "${c.name} ${c.desc}".lowercase().contains(qq)) }
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") { ctx.back() }
                Row(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(22.dp)).background(Kx.w(.06f)).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), tint = Kx.text4)
                    ExField(q, { q = it }, "Каналы и группы", Modifier.weight(1f))
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 4.dp, bottom = 30.dp)) {
                if (qq.isEmpty()) Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp).fillMaxWidth().height(150.dp).clip(RoundedCornerShape(26.dp)).lin2(140f, Color(0xFF23523B), Color(0xFF0F2A1E))) {
                    val d = tri(kfRun(8000))
                    Box(Modifier.align(Alignment.TopEnd).offset(40.dp, (-40).dp).size(200.dp).graphicsLayer { translationX = -10.dp.toPx() * d; translationY = 6.dp.toPx() * d }
                        .circleGlow(Kx.accent.copy(alpha = .35f)))
                    Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp)) {
                        Row(Modifier.height(24.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = .25f)).padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(14.dp), tint = Kx.accent)
                            Text("Похожие на ваши каналы", style = ts(11.sp, FontWeight.ExtraBold, Kx.accent))
                        }
                        Text("Наука простыми словами", style = ts(20.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 10.dp))
                        Text("Каждый день одно удивительное открытие", style = ts(13.sp, color = Color(0xFFC9E6D4)), modifier = Modifier.padding(top = 4.dp))
                    }
                    val bg by animateColorAsState(if (feat) Color.Black.copy(alpha = .25f) else Kx.accent, tween(200), label = "fb")
                    val ink by animateColorAsState(if (feat) Kx.accent else Kx.onAccent, tween(200), label = "fi")
                    Row(Modifier.align(Alignment.BottomStart).padding(start = 18.dp, bottom = 16.dp).height(34.dp).clip(RoundedCornerShape(17.dp)).background(bg)
                        .kxClick(if (feat) "Вы подписаны" else "Подписаться") { feat = !feat }.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(if (feat) Icons.Outlined.Check else Icons.Outlined.Add, null, Modifier.size(17.dp), tint = ink)
                        Text(if (feat) "Вы подписаны" else "Подписаться", style = ts(13.sp, FontWeight.ExtraBold, ink))
                    }
                    Text("84 тыс. подписчиков", style = ts(12.sp, color = Color(0xFFC9E6D4)), modifier = Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 22.dp))
                }
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExpCats.forEach { (id, icon, label) ->
                        val on = cat == id
                        val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.05f), tween(200), label = "cb")
                        val bd by animateColorAsState(if (on) Kx.accent.copy(alpha = .45f) else Color.Transparent, tween(200), label = "cd")
                        val ink = if (on) Kx.accent else Kx.text
                        Row(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(bg).border(1.dp, bd, RoundedCornerShape(18.dp)).kxClick(label) { cat = id }.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(icon, null, Modifier.size(17.dp), tint = ink); Text(label, style = ts(13.sp, FontWeight.Bold, ink))
                        }
                    }
                }
                val title = if (qq.isNotEmpty()) "Результаты поиска" else if (cat == "all") "Популярное" else ExpCats.first { it.first == cat }.third
                Text(title, style = ts(13.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 6.dp))
                Column(Modifier.padding(horizontal = 10.dp)) {
                    key(cat, qq) {
                        list.forEachIndexed { k, (i, c) ->
                            val on = i in joined
                            val e = enter(300, k * 40)
                            Row(Modifier.graphicsLayer { alpha = e; translationY = (1f - e) * 10.dp.toPx() }.padding(horizontal = 8.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box {
                                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(17.dp)).lin2(140f, c.a, c.b), contentAlignment = Alignment.Center) {
                                        Icon(c.icon, null, Modifier.size(26.dp), tint = Color.White)
                                    }
                                    // 20 + 2px border (content-box → 24), right/bottom -3
                                    if (c.g) Box(Modifier.align(Alignment.BottomEnd).offset(5.dp, 5.dp).size(24.dp).clip(CircleShape).background(Kx.bg).padding(2.dp).clip(CircleShape)
                                        .background(Color(0xFF1E2622)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Group, null, Modifier.size(12.dp), tint = Kx.text2) }
                                }
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(c.name, style = ts(15.sp, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                        if (c.v) Icon(Icons.Rounded.Verified, null, Modifier.size(15.dp), tint = Kx.accent)
                                    }
                                    Text(c.desc, style = ts(12.sp, color = Kx.text4), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                                    Text(exFmtCount(c.subs + if (on) 1 else 0) + if (c.g) " участников" else " подписчиков", style = ts(11.sp, color = Kx.text4), modifier = Modifier.padding(top = 2.dp))
                                }
                                val btn = if (on) (if (c.g) "В группе" else "Подписан") else (if (c.g) "Вступить" else "Подписаться")
                                key(if (popI == i) popN else 0) {
                                    val p = if (popI == i && popN > 0) enter(350) else 1f
                                    Row(Modifier.graphicsLayer { val s = if (p < .7f) .7f + .4f * (p / .7f) else 1.1f - .1f * ((p - .7f) / .3f); scaleX = s; scaleY = s }
                                        .height(34.dp).clip(RoundedCornerShape(17.dp)).then(if (on) Modifier.background(Kx.accent.copy(alpha = .12f)) else Modifier.lin2(140f, Kx.accentMid, Kx.accentDeep))
                                        .kxClick(btn) { if (on) joined.remove(i) else joined.add(i); popI = i; popN++ }.padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (on) Icon(Icons.Outlined.Check, null, Modifier.size(15.dp), tint = Kx.accent)
                                        Text(btn, style = ts(12.sp, FontWeight.ExtraBold, if (on) Kx.accent else Kx.onAccent))
                                    }
                                }
                            }
                        }
                    }
                    if (list.isEmpty()) Text("Ничего не нашлось", style = ts(14.sp, color = Kx.text4), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp))
                }
            }
        }
    }
}
