package com.example.taskorganizer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.taskorganizer.ui.theme.OrganizerPalette

@Composable
fun FilterChoice(label: String, value: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = OrganizerPalette.cardLight),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = OrganizerPalette.muted, fontSize = 12.sp)
                Text(value, color = OrganizerPalette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("●", color = OrganizerPalette.accent, fontSize = 14.sp)
        }
    }
}

@Composable
fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = OrganizerPalette.accent, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(label, color = OrganizerPalette.muted, fontSize = 12.sp)
    }
}

@Composable
fun EmptyState(title: String, subtitle: String) {
    Card(colors = CardDefaults.cardColors(containerColor = OrganizerPalette.cardLight), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✦", color = OrganizerPalette.accent, fontSize = 28.sp)
            Spacer(Modifier.height(9.dp))
            Text(title, color = OrganizerPalette.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(7.dp))
            Text(subtitle, color = OrganizerPalette.muted, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun InfoCard(title: String, body: String, status: String) {
    Card(colors = CardDefaults.cardColors(containerColor = OrganizerPalette.card), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Text(title, color = OrganizerPalette.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp)); Text(body, color = OrganizerPalette.muted, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp)); Text(status, color = OrganizerPalette.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
