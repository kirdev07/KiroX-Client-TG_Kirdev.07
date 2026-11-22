package com.kirox.app.ui.v3.screens.extra

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.theme.Kx

/** Shared pieces of the second batch of «extra» boards (Seen … Nearby). */

/** Plain input like the mockups' bare `<input>`: [placeholder] in #8A958F while empty. */
@Composable
internal fun ExField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, style: TextStyle = ts(15.sp),
                     phColor: Color = Kx.text4, singleLine: Boolean = true, keyboard: KeyboardOptions = KeyboardOptions.Default) {
    BasicTextField(value, onChange, modifier, singleLine = singleLine, textStyle = style, cursorBrush = SolidColor(Kx.accent), keyboardOptions = keyboard,
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, style = style.copy(color = phColor), maxLines = if (singleLine) 1 else Int.MAX_VALUE)
                inner()
            }
        })
}

/** 120400 → «120 тыс.», 540000 → «540 тыс.», 1200000 → «1.2 млн» (the boards' fmt()). */
internal fun exFmtCount(n: Int): String = when {
    n >= 1_000_000 -> (Math.round(n / 100_000.0) / 10.0).toString().removeSuffix(".0") + " млн"
    n >= 1000 -> "${Math.round(n / 1000.0)} тыс."
    else -> "$n"
}

/** Round letter avatar with a CSS gradient (deg, from, to). */
@Composable
internal fun ExLetter(letter: String, size: Dp, from: Color, to: Color = from, deg: Float = 140f, ink: Color = Color.White, font: TextUnit = 16.sp,
                      weight: FontWeight = FontWeight.ExtraBold, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).then(if (from == to) Modifier.background(from) else Modifier.lin2(deg, from, to)), contentAlignment = Alignment.Center) {
        Text(letter, style = ts(font, weight, ink))
    }
}

internal val ExDp0: Dp = 0.dp
