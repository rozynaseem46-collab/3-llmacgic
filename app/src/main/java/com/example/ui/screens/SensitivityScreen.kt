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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SensitivityConfig
import com.example.ui.theme.*

@Composable
fun SensitivityScreen(
    sensitivity: SensitivityConfig,
    onSensitivityChange: (SensitivityConfig) -> Unit,
    onSaveProfile: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Test Pad state: simulate aiming camera pan with current sensitivity multipliers
    var testAimOffset by remember { mutableStateOf(Offset.Zero) }
    var testHitCount by remember { mutableStateOf(0) }
    var isFiring by remember { mutableStateOf(false) }

    val presets = listOf(
        "Low" to SensitivityConfig(general = 45, cameraLook = 40, scope2x = 35, scope4x = 25, sniperScope = 18, presetName = "Low"),
        "Medium" to SensitivityConfig(general = 68, cameraLook = 65, scope2x = 52, scope4x = 38, sniperScope = 28, presetName = "Medium"),
        "High" to SensitivityConfig(general = 88, cameraLook = 82, scope2x = 72, scope4x = 58, sniperScope = 44, presetName = "High"),
        "Pro-Gamer" to SensitivityConfig(general = 98, cameraLook = 92, scope2x = 85, scope4x = 74, sniperScope = 55, presetName = "Pro-Gamer")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MacricBlack)
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("sensitivity_screen_root")
    ) {
        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Sensitivity Tuner",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MacricTextPrimary
                )
                Text(
                    text = "Tune camera tracking, ADS scopes & feel responsive aiming",
                    fontSize = 11.sp,
                    color = MacricTextMuted
                )
            }

            Button(
                onClick = onSaveProfile,
                colors = ButtonDefaults.buttonColors(containerColor = MacricViolet),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.testTag("sensitivity_save_btn")
            ) {
                Text("Save", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Preset Selector Row
        Text(text = "Quick Presets", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MacricTextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { (name, cfg) ->
                val isSelected = sensitivity.presetName == name
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) MacricViolet else MacricDarkCard)
                        .border(1.dp, if (isSelected) MacricVioletLight else MacricBorder, RoundedCornerShape(10.dp))
                        .clickable { onSensitivityChange(cfg) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else MacricTextMuted
                    )
                }
            }

            // Reset Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MacricDarkCardElevated)
                    .border(1.dp, MacricBorder, RoundedCornerShape(10.dp))
                    .clickable {
                        onSensitivityChange(
                            SensitivityConfig(
                                general = 68,
                                cameraLook = 65,
                                scope2x = 52,
                                scope4x = 38,
                                sniperScope = 28,
                                presetName = "Medium"
                            )
                        )
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset default", tint = MacricTextMuted, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interactive "Test Pad" Area
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
                                .background(MacricGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE SENSITIVITY TEST PAD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MacricTextMuted,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Dummy Targets Hit: $testHitCount",
                            fontSize = 10.sp,
                            color = MacricGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                testAimOffset = Offset.Zero
                                testHitCount = 0
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = "Reset Test", tint = MacricTextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Drag finger/pointer inside the box below to test camera movement multiplier (${sensitivity.general}%). Aim at target dummies!",
                    fontSize = 10.sp,
                    color = MacricTextMuted
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF090614))
                        .border(1.dp, MacricBorder, RoundedCornerShape(12.dp))
                        .pointerInput(sensitivity.general) {
                            val sensMultiplier = (sensitivity.general / 50f).coerceIn(0.2f, 2.5f)
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val newX = (testAimOffset.x + dragAmount.x * sensMultiplier).coerceIn(-130f, 130f)
                                val newY = (testAimOffset.y + dragAmount.y * sensMultiplier).coerceIn(-70f, 70f)
                                testAimOffset = Offset(newX, newY)

                                // Check hit on target dummy (located at fixed offsets)
                                val dist1 = (Offset(newX, newY) - Offset(-70f, -20f)).getDistance()
                                val dist2 = (Offset(newX, newY) - Offset(70f, 20f)).getDistance()
                                if (dist1 < 25f || dist2 < 25f) {
                                    testHitCount++
                                }
                            }
                        }
                        .testTag("sensitivity_test_pad_canvas")
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val cx = w / 2f
                        val cy = h / 2f

                        // Target Dummy 1
                        val t1 = Offset(cx - 70f, cy - 20f)
                        drawCircle(color = MacricRed.copy(alpha = 0.4f), radius = 22f, center = t1)
                        drawCircle(color = Color.White, radius = 10f, center = t1)
                        drawCircle(color = MacricRed, radius = 4f, center = t1)

                        // Target Dummy 2
                        val t2 = Offset(cx + 70f, cy + 20f)
                        drawCircle(color = MacricCyan.copy(alpha = 0.4f), radius = 22f, center = t2)
                        drawCircle(color = Color.White, radius = 10f, center = t2)
                        drawCircle(color = MacricCyan, radius = 4f, center = t2)

                        // Reticle position reacting to dragging & sensitivity
                        val reticlePos = Offset(cx + testAimOffset.x, cy + testAimOffset.y)

                        // Crosshair on test pad
                        val chColor = MacricVioletLight
                        drawLine(chColor, Offset(reticlePos.x - 14f, reticlePos.y), Offset(reticlePos.x + 14f, reticlePos.y), strokeWidth = 2f)
                        drawLine(chColor, Offset(reticlePos.x, reticlePos.y - 14f), Offset(reticlePos.x, reticlePos.y + 14f), strokeWidth = 2f)
                        drawCircle(color = chColor, radius = 3f, center = reticlePos)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sensitivity Sliders (0..100) with direct number inputs
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MacricBorder, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = MacricDarkCard),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // General Look Sensitivity
                SensSliderItem(
                    label = "General Sensitivity",
                    description = "Main camera swipe speed across the screen",
                    value = sensitivity.general,
                    onValueChange = { onSensitivityChange(sensitivity.copy(general = it, presetName = "Custom")) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Camera Look Sensitivity
                SensSliderItem(
                    label = "Camera / Free Look",
                    description = "Eye button / 360 observation sensitivity",
                    value = sensitivity.cameraLook,
                    onValueChange = { onSensitivityChange(sensitivity.copy(cameraLook = it, presetName = "Custom")) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 2x Scope Sensitivity
                SensSliderItem(
                    label = "Red Dot / 2x Scope ADS",
                    description = "Aim-down-sights mid-range tracking multiplier",
                    value = sensitivity.scope2x,
                    onValueChange = { onSensitivityChange(sensitivity.copy(scope2x = it, presetName = "Custom")) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 4x Scope Sensitivity
                SensSliderItem(
                    label = "4x Scope ADS",
                    description = "Long-range rifles tracking precision",
                    value = sensitivity.scope4x,
                    onValueChange = { onSensitivityChange(sensitivity.copy(scope4x = it, presetName = "Custom")) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Sniper Scope Sensitivity
                SensSliderItem(
                    label = "Sniper Scope ADS",
                    description = "High magnification bolt-action micro adjustments",
                    value = sensitivity.sniperScope,
                    onValueChange = { onSensitivityChange(sensitivity.copy(sniperScope = it, presetName = "Custom")) }
                )
            }
        }
    }
}

@Composable
private fun SensSliderItem(
    label: String,
    description: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MacricTextPrimary)
                Text(text = description, fontSize = 9.sp, color = MacricTextMuted)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MacricViolet.copy(alpha = 0.2f))
                    .border(1.dp, MacricViolet.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "$value",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MacricVioletLight
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = MacricVioletLight,
                activeTrackColor = MacricViolet,
                inactiveTrackColor = MacricBorder
            )
        )
    }
}
