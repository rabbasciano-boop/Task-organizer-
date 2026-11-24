package com.example.taskorganizer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskorganizer.ui.theme.OrganizerPalette
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.Locale
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeAgendaTaskCard(task: OrganizerTask, onToggle: () -> Unit, onComplete: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit) {
    var dragOffset by remember(task.id) { mutableFloatStateOf(0f) }
    val threshold = with(LocalDensity.current) { 88.dp.toPx() }
    val action = when { dragOffset > 12f -> "complete"; dragOffset < -12f -> "delete"; else -> null }
    val titleColor = taskCardTextColor(task)
    val mutedColor = taskCardMutedColor(task)
    val cardShape = RoundedCornerShape(24.dp)
    val useSunsetGradient = action == null && OrganizerPalette.gradientTaskCards
    Card(
        colors = CardDefaults.cardColors(containerColor = when (action) { "complete" -> Success; "delete" -> Danger; else -> if (useSunsetGradient) Color.Transparent else taskCardBackground(task.cardColor) }),
        shape = cardShape,
        modifier = Modifier.fillMaxWidth().then(if (useSunsetGradient) Modifier.sunsetTaskSurface(task.cardColor, cardShape) else Modifier).sunsetGlow(cardShape, strong = useSunsetGradient).graphicsLayer { translationX = dragOffset }.semantics {
            customActions = buildList {
                add(CustomAccessibilityAction(if (task.completed) "Segna come da fare" else "Completa") { onToggle(); true })
                add(CustomAccessibilityAction("Elimina") { onDelete(); true })
            }
            contentDescription = "${task.title}, ${formatTimeRange(task) ?: "senza orario"}, ${task.category}, ${if (task.completed) "completata" else "da fare"}"
            role = Role.Button
        }.pointerInput(task.id, task.completed) {
            detectHorizontalDragGestures(
                onHorizontalDrag = { _, amount ->
                    val next = dragOffset + amount
                    dragOffset = if (task.completed && next > 0f) 0f else next.coerceIn(-threshold * 1.35f, threshold * 1.35f)
                },
                onDragEnd = { when { dragOffset >= threshold && !task.completed -> onComplete(); dragOffset <= -threshold -> onDelete() }; dragOffset = 0f },
                onDragCancel = { dragOffset = 0f }
            )
        }.clickable(onClick = onEdit)
    ) {
        if (action != null) {
            Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (action == "complete") {
                    Text("✓", color = NightBlue, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                } else {
                    androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_delete), contentDescription = null, tint = MainText, modifier = Modifier.size(20.dp))
                }
                Text(if (action == "complete") "Completata" else "Elimina", color = if (action == "complete") NightBlue else MainText, modifier = Modifier.padding(start = 8.dp), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(categoryIcon(task.category, task.categoryIcon), fontSize = 24.sp)
                Column(Modifier.weight(1f)) {
                    Text(task.title, color = if (task.completed) mutedColor else titleColor, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None)
                    Text(listOfNotNull(formatTimeRange(task), task.category).joinToString(" • ").ifBlank { "Senza orario" }, color = mutedColor, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
                Box(Modifier.size(48.dp).clickable(onClick = onToggle), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(28.dp).background(if (task.completed) Success else CardBlueLight.copy(alpha = .82f), RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                        Text(if (task.completed) "✓" else "", color = NightBlue, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
fun AccountAvatar(photoUrl: String?, initial: String, onClick: () -> Unit) {
    var profileImage by remember(photoUrl) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(photoUrl) { profileImage = photoUrl?.let { address -> withContext(Dispatchers.IO) { loadProfileImage(address) } } }
    Card(colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = 0.14f)), shape = RoundedCornerShape(50.dp), modifier = Modifier.size(42.dp).clickable(onClick = onClick).semantics { contentDescription = "Apri menu account"; role = Role.Button }) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            profileImage?.let { image -> Image(image.asImageBitmap(), "Account Google collegato", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                ?: Text(initial, color = Success, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun loadProfileImage(address: String): Bitmap? {
    val url = runCatching { URL(address) }.getOrNull() ?: return null
    if (url.protocol.lowercase(Locale.ROOT) != "https") return null
    val connection = runCatching { url.openConnection() as? HttpsURLConnection }.getOrNull() ?: return null
    return try {
        connection.connectTimeout = 4_000; connection.readTimeout = 4_000; connection.instanceFollowRedirects = false; connection.requestMethod = "GET"; connection.connect()
        if (connection.responseCode != HttpsURLConnection.HTTP_OK || connection.contentLength > MAX_PROFILE_IMAGE_BYTES) null else {
            val bytes = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream(); val buffer = ByteArray(8 * 1024); var total = 0
                while (true) { val read = input.read(buffer); if (read < 0) break; total += read; if (total > MAX_PROFILE_IMAGE_BYTES) return@use null; output.write(buffer, 0, read) }
                output.toByteArray()
            } ?: return null
            decodeProfileImage(bytes)
        }
    } catch (_: Exception) { null } finally { connection.disconnect() }
}

private fun decodeProfileImage(bytes: ByteArray): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / sample > 512 || bounds.outHeight / sample > 512) sample *= 2
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
}

private const val MAX_PROFILE_IMAGE_BYTES = 2 * 1024 * 1024
