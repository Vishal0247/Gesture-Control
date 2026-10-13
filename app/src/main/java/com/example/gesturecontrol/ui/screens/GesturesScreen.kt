package com.example.gesturecontrol.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gesturecontrol.ui.components.MappingRow
import com.example.gesturecontrol.ui.components.SectionHeader
import com.example.gesturecontrol.ui.theme.*

@Composable
fun GesturesScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Normal", "Media", "Calls")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Background,
            contentColor = PrimaryTeal
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, color = if (selectedTab == index) PrimaryTeal else TextSecondary) }
                )
            }
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    item { SectionHeader("Navigation & Actions") }
                    item { MappingRow("Palm", "Home", "1s") }
                    item { MappingRow("Fist", "Back", "1s", listOf("3s" to "Lock Screen")) }
                    item { MappingRow("Thumbs Up", "Tap", "0.4s") }
                    item { MappingRow("1 Finger", "Swipe Up") }
                    item { MappingRow("2 Fingers", "Swipe Down") }
                    item { MappingRow("3 Fingers", "WhatsApp", "1s", listOf("2.5s" to "Toggle ARMED")) }
                    item { MappingRow("Pinch", "Media Mode", "1.5s") }
                }
                1 -> {
                    item { SectionHeader("Media Controls (When Media Mode is ON)") }
                    item { MappingRow("Thumbs Up", "Play / Pause", "0.4s") }
                    item { MappingRow("1 Finger", "Volume Up", "Hold to repeat") }
                    item { MappingRow("2 Fingers", "Volume Down", "Hold to repeat") }
                    item { MappingRow("Fist", "Prev Track", "1s") }
                    item { MappingRow("3 Fingers", "Next Track", "1s") }
                }
                2 -> {
                    item { SectionHeader("Call Handling (Automatic during calls)") }
                    item { SectionHeader("When Ringing:") }
                    item { MappingRow("Thumbs Up", "Answer Call", "0.7s") }
                    item { MappingRow("Palm", "Reject Call", "1s") }
                    item { SectionHeader("When In Call:") }
                    item { MappingRow("Fist", "End Call", "2s") }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = ElevatedSurface),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Tips for Best Detection", style = MaterialTheme.typography.titleLarge, color = PrimaryTeal)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("• Ensure good lighting on your hand.\n• Keep your hand 30-60 cm from the camera.\n• Keep unused fingers fully curled into a fist.", color = TextSecondary)
                    }
                }
            }
        }
    }
}
