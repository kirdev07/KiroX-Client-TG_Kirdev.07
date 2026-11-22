package com.kirox.app.ui.v3.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContactPhone
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.model.KxPeer
import com.kirox.app.ui.v3.theme.*

private val Alts = listOf("👋", "🤝", "😊", "🎉")

/** Board «Пустой чат»: greeting sticker card (pick one of four, tap sends), info about the peer. */
@Composable
fun EmptyChatCard(peer: KxPeer?, onSend: (String) -> Unit, modifier: Modifier = Modifier) {
    var pick by remember { mutableIntStateOf(0) }
    val e = Alts[pick]
    val wave = rememberInfiniteTransition(label = "wave")
    val reduced = LocalKxReducedMotion.current
    // kxWave 1.8s: the hand waves (process-like, loops only for 👋).
    val rot by wave.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing)), label = "rot")
    val angle = if (e == "👋" && !reduced) kotlin.math.sin(rot * 2 * Math.PI.toFloat() * 2) * 14f * (1f - rot) else 0f
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.width(300.dp).clip(RoundedCornerShape(28.dp)).background(Brush.verticalGradient(listOf(Kx.w(.1f), Kx.w(.04f))))
            .border(1.dp, Kx.w(.12f), RoundedCornerShape(28.dp)).padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Здесь пока пусто", style = KxType.style(17.sp, FontWeight.ExtraBold))
            Text("Отправьте сообщение или нажмите на приветствие", style = KxType.style(13.sp, color = Kx.text3), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
            Box(Modifier.padding(top = 14.dp).size(140.dp).clip(RoundedCornerShape(28.dp)).kxClick("Отправить стикер-приветствие") { onSend(e) }, contentAlignment = Alignment.Center) {
                Text(e, style = KxType.style(96.sp), modifier = Modifier.graphicsLayer { rotationZ = angle; transformOrigin = TransformOrigin(.7f, .8f) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Alts.forEachIndexed { i, a ->
                    val on = i == pick
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(if (on) Kx.accent.copy(alpha = .16f) else Color.Black.copy(alpha = .2f))
                        .border(1.5.dp, if (on) Kx.accent else Color.Transparent, RoundedCornerShape(16.dp)).kxClick("Выбрать $a") { pick = i }, contentAlignment = Alignment.Center) {
                        Text(a, style = KxType.style(26.sp))
                    }
                }
            }
        }
        if (peer != null) Column(Modifier.padding(top = 14.dp).width(300.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (peer.commonGroups > 0) InfoRow(Icons.Outlined.Groups, Kx.accent, "${peer.commonGroups} ${plural(peer.commonGroups, "общая группа", "общие группы", "общих групп")}")
            if (!peer.isContact) InfoRow(Icons.Outlined.ContactPhone, Kx.text3, "Нет в ваших контактах" + if (peer.phone.isEmpty()) " · номер скрыт" else "")
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, text: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.w(.04f)).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, Modifier.size(20.dp), tint = tint)
        Text(text, style = KxType.style(13.sp, color = Kx.text2), modifier = Modifier.weight(1f))
    }
}
