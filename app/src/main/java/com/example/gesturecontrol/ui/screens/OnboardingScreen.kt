package com.example.gesturecontrol.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gesturecontrol.ui.theme.*

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    var page by remember { mutableStateOf(0) }
    
    Surface(modifier = Modifier.fillMaxSize(), color = Background) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedContent(targetState = page, label = "onboarding") { p ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when (p) {
                        0 -> {
                            Text("Control your phone with your hands", style = MaterialTheme.typography.displayLarge, color = PrimaryTeal, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Use hand gestures to navigate, control media, and answer calls without touching the screen.", style = MaterialTheme.typography.bodyLarge, color = TextPrimary, textAlign = TextAlign.Center)
                        }
                        1 -> {
                            Text("Required Permissions", style = MaterialTheme.typography.displayLarge, color = PrimaryTeal, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("We need Camera to see your hand, and Accessibility to tap and swipe for you.", style = MaterialTheme.typography.bodyLarge, color = TextPrimary, textAlign = TextAlign.Center)
                        }
                        2 -> {
                            Text("Ready to go", style = MaterialTheme.typography.displayLarge, color = PrimaryTeal, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("You can tweak settings and view the gesture guide anytime in the app.", style = MaterialTheme.typography.bodyLarge, color = TextPrimary, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = { if (page < 2) page++ else onFinish() },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = Background),
                modifier = Modifier.fillMaxWidth(0.8f).height(56.dp)
            ) {
                Text(if (page < 2) "Next" else "Get Started")
            }
            if (page < 2) {
                TextButton(onClick = onFinish, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Skip", color = TextSecondary)
                }
            }
        }
    }
}
