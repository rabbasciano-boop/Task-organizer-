package com.example.taskorganizer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.Locale
import javax.net.ssl.HttpsURLConnection

@Composable
fun HomeAgendaTaskCard(
    task: OrganizerTask,
    onToggle: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var dragOffset by remember(task.id) { mutableFloatStateOf(0f) }
    val actionThreshold = with(LocalDensity.current) { 88.dp.toPx() }
    val swipeAction = when {
        dragOffset > 12f -> "complete"
        dragOffset < -12f -> "delete"
        else -> null
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Text(task.time ?: "--:--", color = SoftText, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 13.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = when (swipeAction) {
                    "complete" -> Success
                    "delete" -> Danger
                    else -> CardBlue
                }
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .weight(1f)
                .graphicsLayer { translationX = dragOffset }
                .semantics {
                    customActions = buildList {
                        add(CustomAccessibilityAction(if (task.completed) "Segna come da fare" else "Completa") { onToggle(); true })
                        add(CustomAccessibilityAction("Elimina") { onDelete(); true })
                    }
                }
                .pointerInput(task.id, task.completed) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, amount ->
                            val nextOffset = dragOffset + amount
                            dragOffset = when {
                                task.completed && nextOffset > 0f -> 0f
                                else -> nextOffset.coerceIn(-actionThreshold * 1.35f, actionThreshold * 1.35f)
                            }
                        },
                        onDragEnd = {
                            when {
                                dragOffset >= actionThreshold && !task.completed -> onComplete()
                                dragOffset <= -actionThreshold -> onDelete()
                            }
                            dragOffset = 0f
                        },
                        onDragCancel = { dragOffset = 0f }
                    )
                }
                .clickable(onClick = onEdit)
        ) {
            if (swipeAction != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (swipeAction == "complete") {
                        Text("✓", color = NightBlue, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.size(8.dp))
                        Text("Completata", color = NightBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("🗑", fontSize = 19.sp)
                        Spacer(Modifier.size(8.dp))
                        Text("Elimina", color = MainText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.size(48.dp).background(if (task.completed) Success else CardBlueLight, RoundedCornerShape(50)).clickable(onClick = onToggle),
                        contentAlignment = Alignment.Center
                    ) { Text(if (task.completed) "✓" else "○", color = if (task.completed) NightBlue else Gold, fontWeight = FontWeight.Bold) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(task.title, color = MainText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("${categoryDisplay(task.category, task.categoryIcon)}${if (task.completed) " · completata" else ""}", color = SoftText, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AccountAvatar(photoUrl: String?, initial: String, onClick: () -> Unit) {
    var profileImage by remember(photoUrl) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(photoUrl) {
        profileImage = photoUrl?.let { address -> withContext(Dispatchers.IO) { loadProfileImage(address) } }
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = 0.14f)),
        shape = RoundedCornerShape(50.dp),
        modifier = Modifier.size(36.dp).clickable(onClick = onClick).semantics {
            contentDescription = "Apri menu account"
            role = Role.Button
        }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            profileImage?.let { image ->
                Image(
                    bitmap = image.asImageBitmap(),
                    contentDescription = "Account Google collegato",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } ?: Text(initial, color = Success, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun loadProfileImage(address: String): Bitmap? {
    val url = runCatching { URL(address) }.getOrNull() ?: return null
    if (url.protocol.lowercase(Locale.ROOT) != "https") return null
    val connection = runCatching { url.openConnection() as? HttpsURLConnection }.getOrNull() ?: return null
    return try {
        connection.connectTimeout = 4_000
        connection.readTimeout = 4_000
        connection.instanceFollowRedirects = false
        connection.requestMethod = "GET"
        connection.connect()
        if (connection.responseCode != HttpsURLConnection.HTTP_OK || connection.contentLength > MAX_PROFILE_IMAGE_BYTES) null else {
            val bytes = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > MAX_PROFILE_IMAGE_BYTES) return@use null
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            } ?: return null
            decodeProfileImage(bytes)
        }
    } catch (_: Exception) {
        null
    } finally {
        connection.disconnect()
    }
}

private fun decodeProfileImage(bytes: ByteArray): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sampleSize = 1
    while (bounds.outWidth / sampleSize > 512 || bounds.outHeight / sampleSize > 512) sampleSize *= 2
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sampleSize })
}

private const val MAX_PROFILE_IMAGE_BYTES = 2 * 1024 * 1024
