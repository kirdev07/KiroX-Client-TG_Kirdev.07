package com.kirox.app.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kirox.app.KiroXApplication
import com.kirox.app.i18n.Text
import com.kirox.app.telegram.TelegramFileRef
import com.kirox.app.ui.components.FusionFullscreen
import com.kirox.app.ui.components.FusionScreenBar
import com.kirox.app.ui.components.rememberTelegramBitmap
import com.kirox.app.ui.theme.Fusion
import kotlinx.coroutines.launch

/** All profile photos of a person, swiped like in Telegram: «2 из 5». */
@Composable
fun ProfilePhotosViewer(userId: Long, title: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val account = remember { (context.applicationContext as KiroXApplication).telegram.account }
    var photos by remember { mutableStateOf<List<TelegramFileRef>?>(null) }
    LaunchedEffect(userId) { photos = account.profilePhotos(userId) }
    FusionFullscreen(onDismiss = onClose) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            val list = photos
            when {
                list == null -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
                list.isEmpty() -> Text("Фото нет", color = Color.White, modifier = Modifier.align(Alignment.Center))
                else -> {
                    val pager = rememberPagerState { list.size }
                    val sizePx = with(LocalDensity.current) { 720.dp.roundToPx() }
                    HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                        val bitmap = rememberTelegramBitmap(list[page], sizePx, priority = 32)
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (bitmap != null) androidx.compose.foundation.Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                            else CircularProgressIndicator(color = Color.White)
                        }
                    }
                    Text("${pager.currentPage + 1} из ${list.size}", color = Color.White, fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp))
                }
            }
            Box(Modifier.statusBarsPadding()) { FusionScreenBar(title, onClose) }
        }
    }
}

/**
 * «Подключить устройство»: the camera reads the QR code that Telegram Desktop or Web shows at
 * sign-in, and after a confirmation that device is signed in to this account.
 */
@Composable
fun QrLoginScreen(onBack: () -> Unit, onNotice: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val account = remember { (context.applicationContext as KiroXApplication).telegram.account }
    var allowed by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = it }
    LaunchedEffect(Unit) { if (!allowed) ask.launch(Manifest.permission.CAMERA) }
    var found by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (allowed && found == null) QrCamera { code -> if (code.startsWith("tg://login?token=")) found = code }
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            FusionScreenBar("Подключить устройство", onBack)
            Spacer(Modifier.weight(1f))
            Box(Modifier.align(Alignment.CenterHorizontally).size(240.dp).border(3.dp, Color.White.copy(alpha = .8f), RoundedCornerShape(24.dp)))
            Spacer(Modifier.weight(1f))
            Text(
                if (allowed) "Откройте Telegram на компьютере или desktop.telegram.org и наведите камеру на QR-код входа"
                else "Нужен доступ к камере, чтобы прочитать QR-код",
                color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(24.dp).navigationBarsPadding()
            )
        }
    }
    found?.let { link ->
        AlertDialog(
            onDismissRequest = { if (!busy) found = null },
            containerColor = Fusion.colors.surface1,
            title = { Text("Войти на этом устройстве?", color = Fusion.colors.textPrimary) },
            text = { Text("Устройство, на экране которого этот QR-код, получит доступ к вашему аккаунту. Подтверждайте, только если это ваш компьютер.", color = Fusion.colors.textSecondary) },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    busy = true
                    scope.launch {
                        runCatching { account.confirmQrLogin(link) }
                            .onSuccess { name -> onNotice(if (name.isBlank()) "Устройство подключено" else "Подключено: $name"); onBack() }
                            .onFailure { onNotice("QR-код устарел или не подошёл — обновите его на компьютере"); found = null }
                        busy = false
                    }
                }) { Text("Войти", color = Fusion.colors.accent) }
            },
            dismissButton = { TextButton(onClick = { found = null }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
        )
    }
}

/** The back camera with a QR reader (zxing); [onCode] gets each code seen. */
@Composable
private fun QrCamera(onCode: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val reader = remember { com.google.zxing.qrcode.QRCodeReader() }
    AndroidView(
        factory = { ctx ->
            val view = PreviewView(ctx)
            val future = ProcessCameraProvider.getInstance(ctx)
            future.addListener({
                val provider = future.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
                val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { image ->
                    runCatching {
                        val plane = image.planes[0]
                        val bytes = ByteArray(plane.buffer.remaining()).also { plane.buffer.get(it) }
                        val source = com.google.zxing.PlanarYUVLuminanceSource(bytes, plane.rowStride, image.height, 0, 0, image.width, image.height, false)
                        reader.decode(com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source))).text
                    }.getOrNull()?.let(onCode)
                    reader.reset()
                    image.close()
                }
                runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                }
            }, ContextCompat.getMainExecutor(ctx))
            view
        },
        onRelease = { runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() } },
        modifier = Modifier.fillMaxSize()
    )
}

/** «Сменить номер»: the new number, then the code Telegram sends to it. */
@Composable
fun ChangeNumberDialog(onDismiss: () -> Unit, onNotice: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val account = remember { (context.applicationContext as KiroXApplication).telegram.account }
    var phone by remember { mutableStateOf("+") }
    var code by remember { mutableStateOf("") }
    var sentTo by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = Fusion.colors.surface1,
        title = { Text("Сменить номер", color = Fusion.colors.textPrimary) },
        text = {
            Column {
                Text(
                    if (sentTo == null) "Аккаунт, чаты и контакты перейдут на новый номер. Старый номер больше не будет входом в Telegram."
                    else "$sentTo на $phone", color = Fusion.colors.textSecondary, fontSize = 14.sp
                )
                Spacer(Modifier.height(12.dp))
                if (sentTo == null) OutlinedTextField(phone, { phone = "+" + it.filter(Char::isDigit).take(15) }, singleLine = true, label = { Text("Новый номер") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone))
                else OutlinedTextField(code, { code = it.filter(Char::isDigit).take(8) }, singleLine = true, label = { Text("Код") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                error?.let { Text(it, color = Fusion.colors.danger, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy && (if (sentTo == null) phone.length > 7 else code.length >= 5), onClick = {
                busy = true; error = null
                scope.launch {
                    if (sentTo == null) runCatching { account.sendChangeNumberCode(phone) }
                        .onSuccess { sentTo = it }.onFailure { error = "Не удалось отправить код: проверьте номер" }
                    else runCatching { account.confirmChangeNumber(code) }
                        .onSuccess { onNotice("Номер изменён"); onDismiss() }.onFailure { error = "Неверный код" }
                    busy = false
                }
            }) { Text(if (sentTo == null) "Получить код" else "Сменить", color = Fusion.colors.accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    )
}

/**
 * «Удалить аккаунт»: for good — messages, groups and contacts are gone. Asks for the cloud password
 * when it is on and for «УДАЛИТЬ» typed by hand, so it cannot happen by accident.
 */
@Composable
fun DeleteAccountDialog(onDismiss: () -> Unit, onDeleted: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val account = remember { (context.applicationContext as KiroXApplication).telegram.account }
    var reason by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var word by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val confirmWord = com.kirox.app.i18n.I18n.t("УДАЛИТЬ")
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = Fusion.colors.surface1,
        title = { Text("Удалить аккаунт навсегда?", color = Fusion.colors.danger) },
        text = {
            Column {
                Text("Все сообщения, группы, каналы и контакты будут удалены без возможности восстановления. Это не выход из аккаунта — это удаление.", color = Fusion.colors.textSecondary, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(reason, { reason = it.take(200) }, label = { Text("Причина (необязательно)") })
                OutlinedTextField(password, { password = it }, singleLine = true, label = { Text("Облачный пароль, если включён") }, visualTransformation = PasswordVisualTransformation())
                OutlinedTextField(word, { word = it }, singleLine = true, label = { Text("Напишите $confirmWord") })
                error?.let { Text(it, color = Fusion.colors.danger, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy && word.trim().equals(confirmWord, ignoreCase = true), onClick = {
                busy = true; error = null
                scope.launch {
                    runCatching { account.deleteAccount(reason.trim(), password) }
                        .onSuccess { onDeleted() }
                        .onFailure { error = if (password.isBlank()) "Telegram не удалил аккаунт: возможно, нужен облачный пароль" else "Неверный пароль или Telegram отказал" }
                    busy = false
                }
            }) { Text("Удалить навсегда", color = Fusion.colors.danger) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = Fusion.colors.accent) } }
    )
}
