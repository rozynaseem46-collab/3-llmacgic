package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CrosshairConfig
import com.example.model.CrosshairStyle
import com.example.ui.theme.*

@Composable
fun CrosshairScreen(
    crosshair: CrosshairConfig,
    onCrosshairChange: (CrosshairConfig) -> Unit,
    onSaveProfile: () -> Unit
) {
    val scrollState = rememberScrollState()

    val presetColors = listOf(
        "#9312FF" to "Violet",
        "#45B558" to "Green",
        "#F03B2A" to "Red",
        "#F7F7F8" to "White",
        "#00E5FF" to "Cyan",
        "#FFD600" to "Yellow"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MacricBlack)
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("crosshair_screen_root")
    ) {
        // Top Header with On/Off Switch & Save Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Crosshair Studio",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MacricTextPrimary
                )
                Text(
                    text = "Custom visual target reticle with pixel precision",
                    fontSize = 11.sp,
                    color = MacricTextMuted
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = crosshair.enabled,
                    onCheckedChange = { onCrosshairChange(crosshair.copy(enabled = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MacricViolet,
                        uncheckedTrackColor = MacricDarkCard
                    ),
                    modifier = Modifier.testTag("crosshair_toggle_switch")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onSaveProfile,
                    colors = ButtonDefaults.buttonColors(containerColor = MacricViolet),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("crosshair_save_btn")
                ) {
                    Text("Save", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live Landscape Game-Screen Viewport Preview (Drag to reposition)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MacricBorderGlow, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MacricDarkCardElevated),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (crosshair.enabled) MacricGreen else MacricTextMuted)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE GAME VIEWPORT (DRAG TO POSITION)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MacricTextMuted,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Text(
                        text = "Offset: (${crosshair.offsetX.toInt()}, ${crosshair.offsetY.toInt()})",
                        fontSize = 10.sp,
                        color = MacricCyan
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Interactive Canvas Viewport (Landscape Mock)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0D0A17))
                        .border(1.dp, MacricBorder, RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onCrosshairChange(
                                    crosshair.copy(
                                        offsetX = (crosshair.offsetX + dragAmount.x).coerceIn(-120f, 120f),
                                        offsetY = (crosshair.offsetY + dragAmount.y).coerceIn(-80f, 80f)
                                    )
                                )
                            }
                        }
                        .testTag("crosshair_preview_canvas")
                ) {
                    // Draw landscape environment grid lines & target
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val cx = w / 2f + crosshair.offsetX
                        val cy = h / 2f + crosshair.offsetY

                        // Subtle center guide lines
                        drawLine(
                            color = Color(0x22FFFFFF),
                            start = Offset(w / 2f, 0f),
                            end = Offset(w / 2f, h),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = Color(0x22FFFFFF),
                            start = Offset(0f, h / 2f),
                            end = Offset(w, h / 2f),
                            strokeWidth = 1f
                        )

                        if (!crosshair.enabled) {
                            return@Canvas
                        }

                        // Render Crosshair based on Style
                        val chColor = try {
                            Color(android.graphics.Color.parseColor(crosshair.colorHex))
                        } catch (e: Exception) {
                            MacricViolet
                        }.copy(alpha = crosshair.opacity)

                        val s = crosshair.size * 0.8f
                        val g = crosshair.gap * 0.8f
                        val t = crosshair.thickness

                        when (crosshair.style) {
                            CrosshairStyle.DOT -> {
                                drawCircle(
                                    color = chColor,
                                    radius = (t * 2f).coerceAtLeast(4f),
                                    center = Offset(cx, cy)
                                )
                            }
                            CrosshairStyle.PLUS -> {
                                drawLine(chColor, Offset(cx - s, cy), Offset(cx + s, cy), strokeWidth = t, cap = StrokeCap.Round)
                                drawLine(chColor, Offset(cx, cy - s), Offset(cx, cy + s), strokeWidth = t, cap = StrokeCap.Round)
                            }
                            CrosshairStyle.CIRCLE -> {
                                drawCircle(
                                    color = chColor,
                                    radius = s * 0.75f,
                                    center = Offset(cx, cy),
                                    style = Stroke(width = t)
                                )
                            }
                            CrosshairStyle.CROSS_GAP -> {
                                drawLine(chColor, Offset(cx - s, cy), Offset(cx - g, cy), strokeWidth = t, cap = StrokeCap.Round)
                                drawLine(chColor, Offset(cx + g, cy), Offset(cx + s, cy), strokeWidth = t, cap = StrokeCap.Round)
                                drawLine(chColor, Offset(cx, cy - s), Offset(cx, cy - g), strokeWidth = t, cap = StrokeCap.Round)
                                drawLine(chColor, Offset(cx, cy + g), Offset(cx, cy + s), strokeWidth = t, cap = StrokeCap.Round)
                            }
                            CrosshairStyle.CIRCLE_DOT -> {
                                drawCircle(
                                    color = chColor,
                                    radius = s * 0.75f,
                                    center = Offset(cx, cy),
                                    style = Stroke(width = t)
                                )
                                drawCircle(
                                    color = chColor,
                                    radius = (t * 1.5f).coerceAtLeast(3f),
                                    center = Offset(cx, cy)
                                )
                            }
                            CrosshairStyle.T_SHAPE -> {
                                drawLine(chColor, Offset(cx - s, cy), Offset(cx - g, cy), strokeWidth = t, cap = StrokeCap.Round)
                                drawLine(chColor, Offset(cx + g, cy), Offset(cx + s, cy), strokeWidth = t, cap = StrokeCap.Round)
                                drawLine(chColor, Offset(cx, cy + g), Offset(cx, cy + s), strokeWidth = t, cap = StrokeCap.Round)
                            }
                            CrosshairStyle.CHEVRON -> {
                                drawLine(chColor, Offset(cx - s * 0.7f, cy + s * 0.4f), Offset(cx, cy - s * 0.4f), strokeWidth = t, cap = StrokeCap.Round)
                                drawLine(chColor, Offset(cx, cy - s * 0.4f), Offset(cx + s * 0.7f, cy + s * 0.4f), strokeWidth = t, cap = StrokeCap.Round)
                            }
                            CrosshairStyle.DIAMOND -> {
                                drawLine(chColor, Offset(cx, cy - s * 0.7f), Offset(cx + s * 0.7f, cy), strokeWidth = t, cap = StrokeCap.Round)
                                drawLine(chColor, Offset(cx + s * 0.7f, cy), Offset(cx, cy + s * 0.7f), strokeWidth = t, cap = StrokeCap.Round)
                                drawLine(chColor, Offset(cx, cy + s * 0.7f), Offset(cx - s * 0.7f, cy), strokeWidth = t, cap = StrokeCap.Round)
                                drawLine(chColor, Offset(cx - s * 0.7f, cy), Offset(cx, cy - s * 0.7f), strokeWidth = t, cap = StrokeCap.Round)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Fine Nudge & Reset Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { onCrosshairChange(crosshair.copy(offsetX = crosshair.offsetX - 1f)) },
                            modifier = Modifier.size(32.dp).background(MacricDarkCard, CircleShape)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Nudge Left", tint = MacricTextPrimary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { onCrosshairChange(crosshair.copy(offsetX = crosshair.offsetX + 1f)) },
                            modifier = Modifier.size(32.dp).background(MacricDarkCard, CircleShape)
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Nudge Right", tint = MacricTextPrimary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { onCrosshairChange(crosshair.copy(offsetY = crosshair.offsetY - 1f)) },
                            modifier = Modifier.size(32.dp).background(MacricDarkCard, CircleShape)
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Nudge Up", tint = MacricTextPrimary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { onCrosshairChange(crosshair.copy(offsetY = crosshair.offsetY + 1f)) },
                            modifier = Modifier.size(32.dp).background(MacricDarkCard, CircleShape)
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Nudge Down", tint = MacricTextPrimary, modifier = Modifier.size(16.dp))
                        }
                    }

                    OutlinedButton(
                        onClick = { onCrosshairChange(crosshair.copy(offsetX = 0f, offsetY = 0f)) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MacricTextMuted),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("reset_crosshair_center_btn")
                    ) {
                        Icon(Icons.Default.FilterCenterFocus, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset to Centre", fontSize = 10.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Style Picker (8 styles)
        Text(text = "Crosshair Style", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MacricTextPrimary)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CrosshairStyle.values().take(4).forEach { style ->
                val isSelected = crosshair.style == style
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) MacricViolet else MacricDarkCard)
                        .border(1.dp, if (isSelected) MacricVioletLight else MacricBorder, RoundedCornerShape(10.dp))
                        .clickable { onCrosshairChange(crosshair.copy(style = style)) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = style.displayName,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else MacricTextMuted,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CrosshairStyle.values().drop(4).forEach { style ->
                val isSelected = crosshair.style == style
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) MacricViolet else MacricDarkCard)
                        .border(1.dp, if (isSelected) MacricVioletLight else MacricBorder, RoundedCornerShape(10.dp))
                        .clickable { onCrosshairChange(crosshair.copy(style = style)) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = style.displayName,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else MacricTextMuted,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sliders: Size, Thickness, Center Gap, Opacity
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MacricBorder, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = MacricDarkCard),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Size Slider
                SliderRow(
                    label = "Size",
                    value = crosshair.size,
                    valueRange = 10f..60f,
                    displayValue = "${crosshair.size.toInt()}px",
                    onValueChange = { onCrosshairChange(crosshair.copy(size = it)) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Thickness Slider
                SliderRow(
                    label = "Thickness",
                    value = crosshair.thickness,
                    valueRange = 1f..8f,
                    displayValue = "${crosshair.thickness.toInt()}px",
                    onValueChange = { onCrosshairChange(crosshair.copy(thickness = it)) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Center Gap Slider
                SliderRow(
                    label = "Center Gap",
                    value = crosshair.gap,
                    valueRange = 0f..25f,
                    displayValue = "${crosshair.gap.toInt()}px",
                    onValueChange = { onCrosshairChange(crosshair.copy(gap = it)) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Opacity Slider
                SliderRow(
                    label = "Opacity",
                    value = crosshair.opacity,
                    valueRange = 0.2f..1f,
                    displayValue = "${(crosshair.opacity * 100).toInt()}%",
                    onValueChange = { onCrosshairChange(crosshair.copy(opacity = it)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Color Picker
        Text(text = "Crosshair Color", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MacricTextPrimary)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            presetColors.forEach { (hex, name) ->
                val isSelected = crosshair.colorHex.equals(hex, ignoreCase = true)
                val c = Color(android.graphics.Color.parseColor(hex))
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(c)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color.White else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onCrosshairChange(crosshair.copy(colorHex = hex)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = if (hex == "#F7F7F8") Color.Black else Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Emulator Hotkey Bind
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MacricDarkCard)
                .border(1.dp, MacricBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Show/Hide Toggle Hotkey", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MacricTextPrimary)
                Text("Emulator keyboard binding shortcut", fontSize = 10.sp, color = MacricTextMuted)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MacricViolet.copy(alpha = 0.25f))
                    .border(1.dp, MacricViolet, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(crosshair.hotkey, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MacricVioletLight)
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 11.sp, color = MacricTextMuted)
            Text(text = displayValue, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MacricCyan)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MacricVioletLight,
                activeTrackColor = MacricViolet,
                inactiveTrackColor = MacricBorder
            )
        )
    }
}
