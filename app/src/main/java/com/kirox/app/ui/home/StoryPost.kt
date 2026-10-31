package com.kirox.app.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import com.kirox.app.i18n.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.kirox.app.KiroXApplication
import com.kirox.app.ui.chat.copyToCache
import com.kirox.app.ui.chat.videoMeta
import com.kirox.app.ui.theme.Fusion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Gallery picker plus a caption/audience dialog that posts a story; call the returned lambda to start. */
@Composable
internal fun rememberStoryPoster(onNotice: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val stories = (context.applicationContext as KiroXApplication).telegram.stories
    var picked by remember { mutableStateOf<Uri?>(null) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { picked = it }
    picked?.let { uri ->
        var caption by remember { mutableStateOf("") }
        var audience by remember { mutableStateOf(com.kirox.app.telegram.StoryAudience.Contacts) }
        var editingFriends by remember { mutableStateOf(false) }
        if (editingFriends) CloseFriendsSheet(onDismiss = { editingFriends = false }, onNotice = onNotice)
        AlertDialog(
            onDismissRequest = { picked = null },
            containerColor = Fusion.colors.surface2,
            title = { Text("Новая история", color = Fusion.colors.textPrimary) },
            text = {
                Column {
                    OutlinedTextField(caption, { caption = it.take(1024) }, placeholder = { Text("Подпись") })
                    com.kirox.app.telegram.StoryAudience.entries.forEach { value ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(audience == value, { audience = value })
                            Text(value.title, color = Fusion.colors.textPrimary, modifier = Modifier.weight(1f))
                            if (value == com.kirox.app.telegram.StoryAudience.CloseFriends)
                                Text("Список", color = Fusion.colors.accent, modifier = Modifier.clickable { editingFriends = true }.padding(8.dp))
                        }
                    }
                    Text("История будет видна 24 часа.", color = Fusion.colors.textSecondary)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    picked = null
                    // Not tied to the chat list: switching tabs while a video is copied must not drop the story.
                    com.kirox.app.AppScope.launch {
                        val path = withContext(Dispatchers.IO) { copyToCache(context, uri) }
                        if (path == null) { onNotice("Не удалось прочитать файл"); return@launch }
                        val isVideo = context.contentResolver.getType(uri)?.startsWith("video/") == true
                        val duration = if (isVideo) withContext(Dispatchers.IO) { videoMeta(path).first } else 0
                        runCatching { stories.postStory(path, isVideo, duration, caption.trim(), audience) }
                            .onSuccess { onNotice("История публикуется") }
                            .onFailure { onNotice("Не удалось опубликовать историю") }
                    }
                }) { Text("Опубликовать", color = Fusion.colors.textPrimary) }
            },
            dismissButton = { TextButton(onClick = { picked = null }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
        )
    }
    return { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }
}

/** «Близкие друзья»: contacts who see stories posted for close friends. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun CloseFriendsSheet(onDismiss: () -> Unit, onNotice: (String) -> Unit) {
    val telegram = (LocalContext.current.applicationContext as KiroXApplication).telegram
    val contacts by telegram.users.contacts.collectAsState()
    var chosen by remember { mutableStateOf<Set<Long>?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { chosen = telegram.extras.closeFriends() }
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Fusion.colors.surface1) {
        Text("Близкие друзья", color = Fusion.colors.textPrimary, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
        Text("Они увидят истории, опубликованные для близких друзей. Список виден только вам.", color = Fusion.colors.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        val set = chosen
        if (set == null) Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { androidx.compose.material3.CircularProgressIndicator(color = Fusion.colors.accent) }
        else {
            androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max = 460.dp)) {
                items(contacts.size) { i ->
                    val c = contacts[i]
                    Row(Modifier.fillMaxWidth().clickable { chosen = if (c.id in set) set - c.id else set + c.id }.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        com.kirox.app.ui.components.TelegramAvatar(c.name, c.photo, 40.dp)
                        Text(c.name, color = Fusion.colors.textPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(start = 12.dp))
                        androidx.compose.material3.Checkbox(c.id in set, { on -> chosen = if (on) set + c.id else set - c.id })
                    }
                }
            }
            com.kirox.app.ui.components.FusionButton("Сохранить · ${set.size}", onClick = {
                scope.launch {
                    runCatching { telegram.extras.setCloseFriends(set) }
                        .onSuccess { onNotice("Список близких друзей сохранён"); onDismiss() }
                        .onFailure { onNotice("Не удалось сохранить список") }
                }
            }, modifier = Modifier.padding(16.dp))
        }
        Spacer(Modifier.navigationBarsPadding().height(4.dp))
    }
}
