package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameModel
import com.example.model.GameProfile
import com.example.ui.theme.*
import com.example.viewmodel.MacricTab
import kotlin.random.Random

@Composable
fun HomeScreen(
    connectedGame: GameModel?,
    activeProfile: GameProfile,
    isOverlayActive: Boolean,
    onNavigateTab: (MacricTab) -> Unit,
    onToggleOverlay: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MacricBlack)
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp)
            .testTag("home_screen_root")
    ) {
        // Hero Stage with Cosmic Planet Rim Arch & Stars
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(MacricBlack)
        ) {
            // Procedural Canvas Stars + Violet Glowing Planet Rim
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Draw background stars
                val rng = Random(42)
                for (i in 0..120) {
                    val sx = rng.nextFloat() * w
                    val sy = rng.nextFloat() * h
                    val sr = rng.nextFloat() * 1.5f + 0.5f
                    val alpha = rng.nextFloat() * 0.7f + 0.3f
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = sr,
                        center = Offset(sx, sy)
                    )
                }

                // Arching giant violet planet rim across the top
                val rimCenter = Offset(w / 2f, -h * 0.45f)
                val rimRadius = w * 0.75f

                // Outer ambient glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MacricViolet.copy(alpha = 0.5f),
                            MacricMagenta.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        center = Offset(w / 2f, h * 0.1f),
                        radius = w * 0.6f
                    ),
                    radius = w * 0.6f,
                    center = Offset(w / 2f, h * 0.1f)
                )

                // Planet rim stroke
                drawArc(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            MacricMagenta,
                            MacricVioletLight,
                            Color.White,
                            MacricViolet,
                            Color.Transparent
                        )
                    ),
                    startAngle = 15f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(rimCenter.x - rimRadius, rimCenter.y - rimRadius),
                    size = Size(rimRadius * 2, rimRadius * 2),
                    style = Stroke(width = 6f)
                )

                // Neon highlight line
                drawArc(
                    color = Color.White.copy(alpha = 0.9f),
                    startAngle = 60f,
                    sweepAngle = 60f,
                    useCenter = false,
                    topLeft = Offset(rimCenter.x - rimRadius, rimCenter.y - rimRadius),
                    size = Size(rimRadius * 2, rimRadius * 2),
                    style = Stroke(width = 2.5f)
                )
            }

            // Hero Text Overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(MacricDarkCardElevated.copy(alpha = 0.85f))
                        .border(1.dp, MacricBorderGlow, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = MacricVioletLight,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "EMULATOR CONTROLS ENGINE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MacricTextPrimary,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "MACRIC",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 2.sp
                )

                Text(
                    text = "Play Smarter, Aim Better.",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MacricVioletLight,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Custom Crosshair · Fire Button · Sensitivity · Emulator HUD",
                    fontSize = 11.sp,
                    color = MacricTextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Hero action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onNavigateTab(MacricTab.GAMES) },
                        colors = ButtonDefaults.buttonColors(containerColor = MacricViolet),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("hero_connect_game_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (connectedGame != null) "Manage Games" else "Connect Game",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { onNavigateTab(MacricTab.DOWNLOAD) },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MacricTextPrimary),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(MacricViolet, MacricBorder))),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("hero_download_portal_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MacricVioletLight
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Download Hub",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Main Dashboard Glass Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Stats row (Connected games & Saved profiles)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Stat 1: Connected games
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, MacricBorder, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = MacricDarkCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Connected games",
                                fontSize = 11.sp,
                                color = MacricTextMuted
                            )
                            Text(
                                text = "+12.5%",
                                fontSize = 10.sp,
                                color = MacricGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (connectedGame != null) "1 Active" else "0 Linked",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MacricTextPrimary
                        )
                    }
                }

                // Stat 2: Saved profiles
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, MacricBorder, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = MacricDarkCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Saved profiles",
                                fontSize = 11.sp,
                                color = MacricTextMuted
                            )
                            Text(
                                text = "Synced",
                                fontSize = 10.sp,
                                color = MacricCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "3 Profiles",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MacricTextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Chart Card: Daily sessions (Violet Area Chart)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MacricBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = MacricDarkCard),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Daily Sessions & Tuning",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MacricTextPrimary
                            )
                            Text(
                                text = "Weekly emulator usage breakdown",
                                fontSize = 10.sp,
                                color = MacricTextMuted
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MacricViolet.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Avg 2.4h/day",
                                fontSize = 10.sp,
                                color = MacricVioletLight,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Area Chart Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val points = listOf(0.3f, 0.55f, 0.4f, 0.75f, 0.6f, 0.9f, 0.85f)
                        val stepX = w / (points.size - 1)

                        val path = Path()
                        val fillPath = Path()

                        points.forEachIndexed { index, fraction ->
                            val x = index * stepX
                            val y = h - (fraction * h * 0.85f)
                            if (index == 0) {
                                path.moveTo(x, y)
                                fillPath.moveTo(x, h)
                                fillPath.lineTo(x, y)
                            } else {
                                val prevX = (index - 1) * stepX
                                val prevY = h - (points[index - 1] * h * 0.85f)
                                val cX = (prevX + x) / 2
                                path.cubicTo(cX, prevY, cX, y, x, y)
                                fillPath.cubicTo(cX, prevY, cX, y, x, y)
                            }
                        }

                        fillPath.lineTo(w, h)
                        fillPath.close()

                        // Draw gradient fill
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MacricViolet.copy(alpha = 0.45f),
                                    Color.Transparent
                                )
                            )
                        )

                        // Draw stroke line
                        drawPath(
                            path = path,
                            color = MacricVioletLight,
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // Draw points
                        points.forEachIndexed { index, fraction ->
                            val x = index * stepX
                            val y = h - (fraction * h * 0.85f)
                            drawCircle(color = MacricBlack, radius = 5.dp.toPx(), center = Offset(x, y))
                            drawCircle(color = MacricVioletLight, radius = 3.dp.toPx(), center = Offset(x, y))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                            Text(text = day, fontSize = 9.sp, color = MacricTextMuted)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Active Profile Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MacricBorderGlow, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = MacricDarkCardElevated),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MacricViolet.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MacricVioletLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Active Profile",
                                    fontSize = 11.sp,
                                    color = MacricTextMuted
                                )
                                Text(
                                    text = activeProfile.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MacricTextPrimary
                                )
                            }
                        }

                        IconButton(
                            onClick = { onNavigateTab(MacricTab.PROFILES) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "View profiles",
                                tint = MacricTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MacricBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Row items: Crosshair, Fire Buttons, Sensitivity
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        ProfileAttributeItem(
                            label = "Crosshair",
                            value = activeProfile.crosshair.style.displayName,
                            color = MacricVioletLight
                        ) {
                            onNavigateTab(MacricTab.CROSSHAIR)
                        }
                        ProfileAttributeItem(
                            label = "Fire Buttons",
                            value = "${activeProfile.fireButtons.size} mapped",
                            color = MacricCyan
                        ) {
                            onNavigateTab(MacricTab.FIRE_BUTTON)
                        }
                        ProfileAttributeItem(
                            label = "General Sens",
                            value = "${activeProfile.sensitivity.general}%",
                            color = MacricGreen
                        ) {
                            onNavigateTab(MacricTab.SENSITIVITY)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Shortcut: Emulator Tip Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MacricBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = MacricDarkCard),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MacricGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MacricGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Emulator Keymap Sync",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MacricTextPrimary
                        )
                        Text(
                            text = "BlueStacks, LDPlayer & MSI have built-in keymapping. You can copy coordinates from MACRIC's Fire Button screen directly.",
                            fontSize = 10.sp,
                            color = MacricTextMuted,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileAttributeItem(
    label: String,
    value: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Text(text = label, fontSize = 10.sp, color = MacricTextMuted)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
