package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

private val ClWho = mapOf("Я" to Kx.accentDeep, "М" to Color(0xFFB07A2E), "П" to Color(0xFF2F6E9E), "Л" to Color(0xFFC0443C))
private val ClColors = listOf(Kx.accent, Kx.warning, Color(0xFF6FA9D8), Color(0xFFF07AA0), Color(0xFFB39DDB))
private data class ClItem(val t: String, val who: String)

/** Board «Чек-лист в чате»: shared checklist message with progress, tick + who avatar, add item, confetti when all done. */
@Composable
fun ChecklistScreen(ctx: KxScreenCtx) {
    // demo: needs store (checklist messages: TDLib inputMessageChecklist / toggle task / add tasks)
    val items = remember { mutableStateListOf(ClItem("Мангал и уголь", "П"), ClItem("Мясо для шашлыка", ""), ClItem("Овощи и зелень", "М"), ClItem("Пледы", ""), ClItem("Настолки", "Л"), ClItem("Колонка", "")) }
    var draft by remember { mutableStateOf("") }
    val n = items.size; val done = items.count { it.who.isNotEmpty() }
    val party = done == n
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().drawBehind { drawLine(Kx.w(.06f), Offset(0f, size.height - .5f), Offset(size.width, size.height - .5f), 1f) }
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") { ctx.back() }
                ExLetter("С", 40.dp, Color(0xFF2F6E9E), Kx.accentDeep, 135f, Kx.text, 16.sp)
                Column(Modifier.weight(1f)) { Text("Семья", style = ts(16.sp, FontWeight.ExtraBold)); Text("5 участников", style = ts(12.sp, color = Kx.text3)) }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.fillMaxWidth(.8f).wrapContentWidth(Alignment.Start).clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)).background(Kx.w(.07f)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text("Мама", style = ts(12.sp, FontWeight.ExtraBold, Kx.warning)); Text("Кто что берёт на дачу в субботу?", style = ts(14.sp))
                }
                Box(Modifier.align(Alignment.End).width(300.dp)) {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp)).background(Brush.verticalGradient(listOf(Kx.accentDeep.copy(alpha = .28f), Kx.accentDeep.copy(alpha = .14f))))
                        .border(1.dp, Kx.accent.copy(alpha = .35f), RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp)).padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Checklist, null, Modifier.size(24.dp), tint = Kx.accent)
                            Text("На дачу в субботу", style = ts(15.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f))
                            Text("$done из $n", style = ts(12.sp, FontWeight.ExtraBold, Kx.accent))
                        }
                        val pct by animateFloatAsState(done / n.toFloat(), tween(400, easing = KxMotion.rise), label = "pct")
                        Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.Black.copy(alpha = .25f))) {
                            Box(Modifier.fillMaxWidth(pct).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(Kx.accent))
                        }
                        Column(Modifier.padding(top = 8.dp)) {
                            items.forEachIndexed { k, it ->
                                val on = it.who.isNotEmpty()
                                Row(Modifier.fillMaxWidth().kxClick(it.t) { items[k] = it.copy(who = if (it.who == "Я") "" else if (on) it.who else "Я") }.padding(horizontal = 2.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    val ring by animateColorAsState(if (on) Kx.accent else Kx.w(.45f), tween(200), label = "r")
                                    val fill by animateColorAsState(if (on) Kx.accent else Color.Transparent, tween(200), label = "f")
                                    Box(Modifier.size(24.dp).clip(RoundedCornerShape(8.dp)).background(fill).border(2.dp, ring, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                        if (on) key(it.who) {
                                            val p = enter(300)
                                            Icon(Icons.Outlined.Check, null, Modifier.size(18.dp).graphicsLayer {
                                                val s = if (p < .6f) 1.3f * (p / .6f) else 1.3f - .3f * ((p - .6f) / .4f); scaleX = s; scaleY = s
                                                rotationZ = if (p < .6f) -30f + 35f * (p / .6f) else 5f - 5f * ((p - .6f) / .4f)
                                            }, tint = Kx.onAccent)
                                        }
                                    }
                                    val ink by animateColorAsState(if (on) Kx.text3 else Kx.text, tween(200), label = "i")
                                    Text(it.t, style = ts(14.sp, color = ink).copy(textDecoration = if (on) TextDecoration.LineThrough else null), modifier = Modifier.weight(1f))
                                    if (on) key(it.who) {
                                        val p = enter(300)
                                        ExLetter(it.who, 24.dp, ClWho[it.who] ?: Color(0xFF5C6661), font = 11.sp, ink = Kx.text, modifier = Modifier.graphicsLayer {
                                            val s = if (p < .7f) 1.2f * (p / .7f) else 1.2f - .2f * ((p - .7f) / .3f); scaleX = s; scaleY = s
                                        })
                                    }
                                }
                            }
                        }
                        Row(Modifier.padding(top = 6.dp).fillMaxWidth().drawBehind { drawLine(Kx.w(.1f), Offset(0f, .5f), Offset(size.width, .5f), 1f) }.padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Add, null, Modifier.size(20.dp), tint = Kx.accent)
                            ExField(draft, { draft = it }, "Добавить пункт", Modifier.weight(1f), ts(14.sp))
                            if (draft.isNotBlank()) Box(Modifier.height(28.dp).clip(RoundedCornerShape(14.dp)).background(Kx.accent).kxClick("Добавить") { items.add(ClItem(draft.trim(), "")); draft = "" }
                                .padding(horizontal = 10.dp), contentAlignment = Alignment.Center) { Text("Добавить", style = ts(12.sp, FontWeight.ExtraBold, Kx.onAccent)) }
                        }
                        Text("18:20 · все могут отмечать", style = ts(11.sp, color = Kx.text3), textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                    }
                    if (party) key(done) {
                        repeat(14) { i ->
                            val p = enter(1000, (i % 4) * 40, EaseOutCss)
                            val a = i / 14.0 * Math.PI * 2; val r = 80 + (i % 3) * 25
                            if (p < 1f) Box(Modifier.offset(150.dp, 40.dp).size(8.dp).graphicsLayer {
                                translationX = (Math.cos(a) * r).toFloat() * density * p; translationY = (Math.sin(a) * r).toFloat() * density * p; rotationZ = 220f * p; alpha = 1f - p
                            }.clip(RoundedCornerShape(2.dp)).background(ClColors[i % 5]))
                        }
                    }
                }
                if (party) Text("Список выполнен 🎉", style = ts(12.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.align(Alignment.CenterHorizontally).exIn(300, dy = 8.dp)
                    .clip(RoundedCornerShape(12.dp)).background(Kx.accent.copy(alpha = .12f)).padding(horizontal = 12.dp, vertical = 4.dp))
            }
            Row(Modifier.fillMaxWidth().drawBehind { drawLine(Kx.w(.06f), Offset(0f, .5f), Offset(size.width, .5f), 1f) }.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(Kx.accent.copy(alpha = .14f)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Checklist, null, Modifier.size(24.dp), tint = Kx.accent) }
                Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.07f)).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                    Text("Сообщение", style = ts(15.sp, color = Kx.text4))
                }
            }
        }
    }
}
