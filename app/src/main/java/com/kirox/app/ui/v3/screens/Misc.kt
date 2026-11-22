package com.kirox.app.ui.v3.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.KxStore
import com.kirox.app.ui.v3.model.KxTab
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.KxType

/** Tab not ported yet: says so honestly instead of showing a fake screen. */
@Composable
fun KxTabPlaceholder(tab: KxTab) = KxEmpty("${tab.label}: экран переносится")

@Composable
fun KxComingSoon(name: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        KxTopBar(name, Modifier.statusBarsPadding(), onBack = onBack)
        KxEmpty("Экран «$name» ещё переносится в новый дизайн")
    }
}

@Composable
fun KxEmpty(text: String) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.Construction, null, Modifier.size(40.dp), tint = Kx.accent)
        Text(text, style = KxType.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
    }
}
