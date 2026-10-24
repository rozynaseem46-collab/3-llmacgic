package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun FairPlayDialog(
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, MacricViolet, RoundedCornerShape(16.dp))
                .testTag("fair_play_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MacricDarkCardElevated)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MacricViolet.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                        .border(1.dp, MacricViolet, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Fair Play Icon",
                        tint = MacricVioletLight,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Fair Play & Usage Notice",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MacricTextPrimary,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "MACRIC is designed solely to customize your own on-screen visual crosshair, fire button position, and sensitivity profiles for Android emulators.\n\n" +
                            "• Fair Play Guarantee: MACRIC contains NO aimbot, NO auto-fire, NO recoil macros, and does NOT read or modify any game memory or files.\n" +
                            "• Compliance: Some competitive games may restrict third-party screen overlays. Please follow each game's specific Terms of Service.\n" +
                            "• Use responsibly at your own discretion.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MacricTextMuted,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onAccept,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("accept_fair_play_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MacricViolet),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = "I Understand & Accept",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
