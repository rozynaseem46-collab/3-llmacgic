package com.example.ui.screens

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ProfileRepository
import com.example.model.AimSmoothMode
import com.example.model.FireButtonConfig
import com.example.model.FireButtonShape
import com.example.model.FireMode
import com.example.service.MacricAccessibilityService
import com.example.service.MacricOverlayService
import com.example.ui.theme.*

@Composable
fun FireButtonScreen(
    fireButtons: List<FireButtonConfig>,
    onFireButtonsChange: (List<FireButtonConfig>) -> Unit,
    onAddFireButton: () -> Unit,
    onRemoveFireButton: (String) -> Unit,
    onSaveProfile: () -> Unit,
    onRequestAccessibilitySettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val repo = remember { ProfileRepository(context) }
    val scrollState = rememberScrollState()

    var selectedButtonId by remember(fireButtons) {
        mutableStateOf(fireButtons.firstOrNull()?.id ?: "")
    }

    var snapToGrid by remember { mutableStateOf(true) }
    var isHudLocked by remember { mutableStateOf(repo.isHudLocked()) }
    var showExportKeymapDialog by remember { mutableStateOf(false) }
    var showBlueStacksGuideDialog by remember { mutableStateOf(false) }
    var emulatorTriggerKey by remember { mutableStateOf(repo.getEmulatorTriggerKey()) }
    var mouseLockEnabled by remember { mutableStateOf(repo.isEmulatorMouseLockEnabled()) }
    var touchVisualizerEnabled by remember { mutableStateOf(repo.isTouchVisualizerEnabled()) }

    val activeSelected = fireButtons.find { it.id == selectedButtonId } ?: fireButtons.firstOrNull()
    val isAccessibilityActive = MacricAccessibilityService.isRunning

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MacricBlack)
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("fire_button_screen_root")
    ) {
        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Fire Button & Aim Engine",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MacricTextPrimary
                )
                Text(
                    text = "Ultra-smooth Pro Headshot Drag & BlueStacks Mouse-Lock",
                    fontSize = 11.sp,
                    color = MacricTextMuted
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { showBlueStacksGuideDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MacricCyan),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(MacricCyan.copy(alpha = 0.6f))),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("bluestacks_guide_btn")
                ) {
                    Icon(Icons.Default.SportsEsports, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("BlueStacks", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = onSaveProfile,
                    colors = ButtonDefaults.buttonColors(containerColor = MacricViolet),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("fire_button_save_btn")
                ) {
                    Text("Save", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // HUD Position Lock & Combat Anchor Status Card (Direct Solution to User Request)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    color = if (isHudLocked) MacricGreen.copy(alpha = 0.8f) else Color(0xFFFFD600).copy(alpha = 0.8f),
                    shape = RoundedCornerShape(14.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (isHudLocked) Color(0x1A45B558) else Color(0x22FFD600)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isHudLocked) MacricGreen.copy(alpha = 0.2f) else Color(0x33FFD600)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isHudLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (isHudLocked) MacricGreen else Color(0xFFFFD600),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isHudLocked) "HUD POSITION: LOCKED (COMBAT READY)" else "HUD POSITION: UNLOCKED (EDITING)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isHudLocked) MacricGreen else Color(0xFFFFD600)
                        )
                        Text(
                            text = if (isHudLocked)
                                "Button is firmly anchored. Tapping immediately fires headshot drag with ZERO screen movement!"
                            else
                                "Move button to align directly on top of Free Fire's in-game fire button, then Lock.",
                            fontSize = 9.5.sp,
                            color = MacricTextMuted,
                            lineHeight = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val newLocked = !isHudLocked
                        isHudLocked = newLocked
                        repo.setHudLocked(newLocked)
                        // Signal overlay service to sync lock state
                        try {
                            val intent = Intent(context, MacricOverlayService::class.java).apply {
                                action = MacricOverlayService.ACTION_TOGGLE_LOCK
                            }
                            context.startService(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isHudLocked) Color(0xFF2E7D32) else Color(0xFFFFA000)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("toggle_hud_lock_btn")
                ) {
                    Text(
                        text = if (isHudLocked) "Unlock" else "Lock Now",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Accessibility Status Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    if (isAccessibilityActive) MacricGreen.copy(alpha = 0.5f) else MacricOrange.copy(alpha = 0.5f),
                    RoundedCornerShape(14.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = MacricDarkCardElevated),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isAccessibilityActive) MacricGreen.copy(alpha = 0.15f) else MacricOrange.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAccessibilityActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isAccessibilityActive) MacricGreen else MacricOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isAccessibilityActive) "Synthetic Touch & Key Engine: ACTIVE" else "ACCESSIBILITY PERMISSION REQUIRED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAccessibilityActive) MacricGreen else MacricOrange
                        )
                        Text(
                            text = if (isAccessibilityActive)
                                "Gestures bypass emulator lag & directly sweep Free Fire's fire button."
                            else
                                "Turn on MACRIC in Accessibility Settings so taps can inject into games.",
                            fontSize = 9.sp,
                            color = MacricTextMuted,
                            lineHeight = 12.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isAccessibilityActive) {
                        Button(
                            onClick = {
                                MacricOverlayService.executeTestFireNow()
                                android.widget.Toast.makeText(context, "🔥 Testing Headshot Drag Tap! Check screen/overlay.", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MacricViolet),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("test_live_fire_tap_btn")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Test Tap", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = onRequestAccessibilitySettings,
                            colors = ButtonDefaults.buttonColors(containerColor = MacricOrange),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("enable_accessibility_btn")
                        ) {
                            Text("Enable", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // BlueStacks Mouse Lock & Keyboard Trigger Config Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MacricCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = MacricDarkCard),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🖱️", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Auto Mouse-Lock Mode (Left Click)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MacricCyan
                        )
                    }

                    Switch(
                        checked = mouseLockEnabled,
                        onCheckedChange = {
                            mouseLockEnabled = it
                            repo.setEmulatorMouseLockEnabled(it)
                            if (it) {
                                repo.setEmulatorTriggerKey("Left Click")
                                emulatorTriggerKey = "Left Click"
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MacricCyan,
                            checkedTrackColor = MacricCyan.copy(alpha = 0.3f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Operates directly from Left Mouse Click! When mouse is locked in BlueStacks (F1 / ~), pressing Left Click automatically executes MACRIC's smooth recoil & headshot drag.",
                    fontSize = 10.sp,
                    color = MacricTextMuted,
                    lineHeight = 13.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Auto Active Badge & Key Choice
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MacricCyan.copy(alpha = 0.12f))
                        .border(1.dp, MacricCyan.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MacricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Active Trigger: Left Click (AUTO)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "READY",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MacricCyan,
                        modifier = Modifier
                            .background(MacricCyan.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Touch Visualizer (Click Ripple Feedback) Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MacricCyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = MacricDarkCard),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(text = "✨", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Touch Visualizer (Click Ripple)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MacricCyan
                        )
                        Text(
                            text = "Displays a subtle ripple effect at the point of Left-Click, providing immediate feedback that input is registered.",
                            fontSize = 10.sp,
                            color = MacricTextMuted,
                            lineHeight = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = touchVisualizerEnabled,
                    onCheckedChange = {
                        touchVisualizerEnabled = it
                        repo.setTouchVisualizerEnabled(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MacricCyan,
                        checkedTrackColor = MacricCyan.copy(alpha = 0.3f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Toolbar: Add button, Snap Grid, Keymap Export
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAddFireButton,
                    colors = ButtonDefaults.buttonColors(containerColor = MacricVioletDark),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_fire_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Button", fontSize = 11.sp)
                }

                if (activeSelected != null && fireButtons.size > 1) {
                    OutlinedButton(
                        onClick = { onRemoveFireButton(activeSelected.id) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MacricRed),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("delete_fire_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontSize = 11.sp)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = snapToGrid,
                    onClick = { snapToGrid = !snapToGrid },
                    label = { Text("Snap Grid", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MacricViolet.copy(alpha = 0.3f),
                        selectedLabelColor = MacricVioletLight
                    )
                )

                Spacer(modifier = Modifier.width(6.dp))

                OutlinedButton(
                    onClick = { showExportKeymapDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Coords", fontSize = 10.sp, color = MacricCyan)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Landscape Viewport Drag & Drop Canvas
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
                    Text(
                        text = if (isHudLocked) "SCREEN POSITION: LOCKED (Combat Mode)" else "DRAG BUTTONS TO ALIGN OVER FREE FIRE HUD",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isHudLocked) MacricGreen else Color(0xFFFFD600),
                        letterSpacing = 0.8.sp
                    )

                    if (activeSelected != null) {
                        Text(
                            text = "${activeSelected.label} (${activeSelected.xPercent.toInt()}%, ${activeSelected.yPercent.toInt()}%)",
                            fontSize = 10.sp,
                            color = MacricCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0D0A17))
                        .border(1.dp, MacricBorder, RoundedCornerShape(12.dp))
                        .testTag("fire_button_canvas_area")
                ) {
                    val canvasWidth = constraints.maxWidth.toFloat()
                    val canvasHeight = constraints.maxHeight.toFloat()

                    // Background Grid & Center Guides
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (snapToGrid) {
                            val step = 20.dp.toPx()
                            for (x in 0..(canvasWidth / step).toInt()) {
                                drawLine(
                                    color = Color(0x11FFFFFF),
                                    start = Offset(x * step, 0f),
                                    end = Offset(x * step, canvasHeight),
                                    strokeWidth = 1f
                                )
                            }
                            for (y in 0..(canvasHeight / step).toInt()) {
                                drawLine(
                                    color = Color(0x11FFFFFF),
                                    start = Offset(0f, y * step),
                                    end = Offset(canvasWidth, y * step),
                                    strokeWidth = 1f
                                )
                            }
                        }

                        // Center reticle marker
                        drawCircle(
                            color = Color(0x22FFFFFF),
                            radius = 6.dp.toPx(),
                            center = Offset(canvasWidth / 2f, canvasHeight / 2f),
                            style = Stroke(width = 1f)
                        )
                    }

                    // Render Draggable Fire Buttons
                    fireButtons.forEach { btn ->
                        val isSelected = btn.id == activeSelected?.id
                        val btnX = (btn.xPercent / 100f * canvasWidth) - (btn.size / 2f)
                        val btnY = (btn.yPercent / 100f * canvasHeight) - (btn.size / 2f)

                        Box(
                            modifier = Modifier
                                .offset(
                                    x = (btnX / LocalDensityCurrent().density).dp,
                                    y = (btnY / LocalDensityCurrent().density).dp
                                )
                                .size(btn.size.dp)
                                .clip(if (btn.shape == FireButtonShape.CIRCLE) CircleShape else RoundedCornerShape(12.dp))
                                .background(
                                    Color(android.graphics.Color.parseColor(btn.colorHex))
                                        .copy(alpha = btn.opacity)
                                )
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else MacricBorderGlow,
                                    shape = if (btn.shape == FireButtonShape.CIRCLE) CircleShape else RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedButtonId = btn.id }
                                .pointerInput(isHudLocked, snapToGrid) {
                                    if (!isHudLocked) {
                                        detectDragGestures(
                                            onDragStart = { selectedButtonId = btn.id }
                                        ) { change, dragAmount ->
                                            change.consume()
                                            var newXPx = (btn.xPercent / 100f * canvasWidth) + dragAmount.x
                                            var newYPx = (btn.yPercent / 100f * canvasHeight) + dragAmount.y

                                            if (snapToGrid) {
                                                val step = 15f
                                                newXPx = Math.round(newXPx / step) * step
                                                newYPx = Math.round(newYPx / step) * step
                                            }

                                            val newXPercent = (newXPx / canvasWidth * 100f).coerceIn(5f, 95f)
                                            val newYPercent = (newYPx / canvasHeight * 100f).coerceIn(5f, 95f)

                                            val updated = fireButtons.map {
                                                if (it.id == btn.id) it.copy(xPercent = newXPercent, yPercent = newYPercent)
                                                else it
                                            }
                                            onFireButtonsChange(updated)
                                        }
                                    }
                                }
                                .testTag("draggable_button_${btn.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = btn.label,
                                    fontSize = (btn.size * 0.22f).coerceIn(9f, 14f).sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "[${btn.keyBinding}]",
                                    fontSize = (btn.size * 0.16f).coerceIn(8f, 11f).sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Button Customizer Panel
        if (activeSelected != null) {
            Text(
                text = "Headshot Physics & Config: ${activeSelected.label}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MacricTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MacricBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = MacricDarkCard),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Label input
                    Text(text = "Button Label", fontSize = 11.sp, color = MacricTextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = activeSelected.label,
                        onValueChange = { newLabel ->
                            val updated = fireButtons.map {
                                if (it.id == activeSelected.id) it.copy(label = newLabel) else it
                            }
                            onFireButtonsChange(updated)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_button_label_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Aim Smooth Mode Selection (All 5 elite modes)
                    Text(text = "Aim Drag & Headshot Assist Algorithm", fontSize = 11.sp, color = MacricTextMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AimSmoothMode.values().forEach { aimMode ->
                            val isAimSelected = activeSelected.aimSmoothMode == aimMode
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isAimSelected) MacricCyan else MacricBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        val updated = fireButtons.map {
                                            if (it.id == activeSelected.id) it.copy(aimSmoothMode = aimMode) else it
                                        }
                                        onFireButtonsChange(updated)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isAimSelected) MacricCyan.copy(alpha = 0.15f) else MacricDarkCardElevated
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isAimSelected,
                                        onClick = {
                                            val updated = fireButtons.map {
                                                if (it.id == activeSelected.id) it.copy(aimSmoothMode = aimMode) else it
                                            }
                                            onFireButtonsChange(updated)
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = MacricCyan)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = aimMode.displayName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAimSelected) MacricCyan else MacricTextPrimary
                                        )
                                        Text(
                                            text = aimMode.description,
                                            fontSize = 9.5.sp,
                                            color = MacricTextMuted,
                                            lineHeight = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (activeSelected.aimSmoothMode != AimSmoothMode.INSTANT_TAP) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // Upward Drag Intensity Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Upward Headshot Drag Distance", fontSize = 11.sp, color = MacricTextMuted)
                                Text("${activeSelected.dragDistance.toInt()} px", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MacricCyan)
                            }
                            Slider(
                                value = activeSelected.dragDistance,
                                onValueChange = { newDist ->
                                    val updated = fireButtons.map {
                                        if (it.id == activeSelected.id) it.copy(dragDistance = newDist) else it
                                    }
                                    onFireButtonsChange(updated)
                                },
                                valueRange = 35f..200f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MacricCyan,
                                    activeTrackColor = MacricCyan,
                                    inactiveTrackColor = MacricBorder
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Drag Duration (Speed) Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Drag Duration (Speed)", fontSize = 11.sp, color = MacricTextMuted)
                                Text("${activeSelected.dragDurationMs} ms (Smoothness)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MacricCyan)
                            }
                            Slider(
                                value = activeSelected.dragDurationMs.toFloat(),
                                onValueChange = { newDuration ->
                                    val updated = fireButtons.map {
                                        if (it.id == activeSelected.id) it.copy(dragDurationMs = newDuration.toLong()) else it
                                    }
                                    onFireButtonsChange(updated)
                                },
                                valueRange = 70f..260f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MacricVioletLight,
                                    activeTrackColor = MacricViolet,
                                    inactiveTrackColor = MacricBorder
                                )
                            )
                        }

                        // J-Curve Direction
                        if (activeSelected.aimSmoothMode == AimSmoothMode.HEADSHOT_DRAG) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "J-Curve Drag Arc Direction", fontSize = 11.sp, color = MacricTextMuted)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("RIGHT" to "Right J-Drag (Pro)", "LEFT" to "Left J-Drag", "STRAIGHT" to "Straight Up").forEach { (dir, label) ->
                                    val isDirSelected = activeSelected.jCurveDirection.equals(dir, ignoreCase = true)
                                    FilterChip(
                                        selected = isDirSelected,
                                        onClick = {
                                            val updated = fireButtons.map {
                                                if (it.id == activeSelected.id) it.copy(jCurveDirection = dir) else it
                                            }
                                            onFireButtonsChange(updated)
                                        },
                                        label = { Text(label, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MacricCyan,
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Keybinding Picker
                    Text(text = "Trigger Key / Mouse Binding", fontSize = 11.sp, color = MacricTextMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    val keyOptions = listOf("Left Click", "Space", "F", "E", "V", "C", "Shift", "Ctrl")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        keyOptions.take(4).forEach { key ->
                            val isBound = activeSelected.keyBinding == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isBound) MacricViolet else MacricDarkCardElevated)
                                    .clickable {
                                        val updated = fireButtons.map {
                                            if (it.id == activeSelected.id) it.copy(keyBinding = key) else it
                                        }
                                        onFireButtonsChange(updated)
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 10.sp,
                                    color = if (isBound) Color.White else MacricTextMuted,
                                    fontWeight = if (isBound) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        keyOptions.drop(4).forEach { key ->
                            val isBound = activeSelected.keyBinding == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isBound) MacricViolet else MacricDarkCardElevated)
                                    .clickable {
                                        val updated = fireButtons.map {
                                            if (it.id == activeSelected.id) it.copy(keyBinding = key) else it
                                        }
                                        onFireButtonsChange(updated)
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 10.sp,
                                    color = if (isBound) Color.White else MacricTextMuted,
                                    fontWeight = if (isBound) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Shape: Circle vs Rounded Square vs Hexagon
                    Text(text = "Button Shape", fontSize = 11.sp, color = MacricTextMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FireButtonShape.values().forEach { shape ->
                            val isShapeSelected = activeSelected.shape == shape
                            FilterChip(
                                selected = isShapeSelected,
                                onClick = {
                                    val updated = fireButtons.map {
                                        if (it.id == activeSelected.id) it.copy(shape = shape) else it
                                    }
                                    onFireButtonsChange(updated)
                                },
                                label = { Text(shape.displayName, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MacricViolet,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Size Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Button Size", fontSize = 11.sp, color = MacricTextMuted)
                            Text("${activeSelected.size.toInt()}dp", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MacricCyan)
                        }
                        Slider(
                            value = activeSelected.size,
                            onValueChange = { newSize ->
                                val updated = fireButtons.map {
                                    if (it.id == activeSelected.id) it.copy(size = newSize) else it
                                }
                                onFireButtonsChange(updated)
                            },
                            valueRange = 40f..110f,
                            colors = SliderDefaults.colors(
                                thumbColor = MacricVioletLight,
                                activeTrackColor = MacricViolet,
                                inactiveTrackColor = MacricBorder
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Opacity Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Button Opacity", fontSize = 11.sp, color = MacricTextMuted)
                            Text("${(activeSelected.opacity * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MacricCyan)
                        }
                        Slider(
                            value = activeSelected.opacity,
                            onValueChange = { newOp ->
                                val updated = fireButtons.map {
                                    if (it.id == activeSelected.id) it.copy(opacity = newOp) else it
                                }
                                onFireButtonsChange(updated)
                            },
                            valueRange = 0.2f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = MacricVioletLight,
                                activeTrackColor = MacricViolet,
                                inactiveTrackColor = MacricBorder
                            )
                        )
                    }
                }
            }
        }
    }

    if (showBlueStacksGuideDialog) {
        BlueStacksMouseLockGuideDialog(
            activeKey = emulatorTriggerKey,
            onDismiss = { showBlueStacksGuideDialog = false }
        )
    }

    if (showExportKeymapDialog) {
        EmulatorKeymapExportDialog(
            fireButtons = fireButtons,
            onDismiss = { showExportKeymapDialog = false }
        )
    }
}

@Composable
private fun LocalDensityCurrent() = androidx.compose.ui.platform.LocalDensity.current

@Composable
private fun BlueStacksMouseLockGuideDialog(
    activeKey: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .border(1.5.dp, MacricCyan, RoundedCornerShape(16.dp))
                .testTag("bluestacks_mouse_lock_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MacricDarkCardElevated)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🎮", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BlueStacks Mouse-Lock Setup",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MacricCyan
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Follow these 3 easy steps for 100% smooth firing in BlueStacks without needing the emulator's stiff fire button:",
                    fontSize = 11.sp,
                    color = MacricTextMuted,
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Step 1
                Text(
                    text = "1. Align & Lock Button Position",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Unlock HUD, drag the MACRIC Fire Button directly over Free Fire's in-game fire button, then click 'Lock Now'.",
                    fontSize = 10.sp,
                    color = MacricTextMuted
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Step 2
                Text(
                    text = "2. Press F1 (or ~) to Lock Mouse",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "In BlueStacks, locking mouse captures cursor and lets you aim with mouse smoothly.",
                    fontSize = 10.sp,
                    color = MacricTextMuted
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Step 3
                Text(
                    text = "3. Fire with [$activeKey] or Left Click",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MacricGreen
                )
                Text(
                    text = "In BlueStacks Controls Editor, either map Left Click as a Tap Spot on MACRIC's button, OR simply press [$activeKey]. MACRIC intercepts the key and injects the Pro Headshot Bezier drag instantly!",
                    fontSize = 10.sp,
                    color = MacricTextMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MacricCyan),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Understood", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmulatorKeymapExportDialog(
    fireButtons: List<FireButtonConfig>,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    val keymapText = buildString {
        appendLine("// MACRIC Emulator Keymap Export")
        appendLine("// Copy & paste coordinates into BlueStacks / LDPlayer / MSI Keymapper:")
        fireButtons.forEach { b ->
            appendLine("Button: ${b.label} | Key: [${b.keyBinding}] | Screen Pos: X=${b.xPercent.toInt()}% Y=${b.yPercent.toInt()}% | Mode=${b.mode.name}")
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, MacricCyan, RoundedCornerShape(16.dp))
                .testTag("keymap_export_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MacricDarkCardElevated)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Emulator Keymap Coordinates",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MacricTextPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Most emulators (BlueStacks, LDPlayer, Gameloop) allow setting on-screen touch points corresponding to keyboard/mouse shortcuts.",
                    fontSize = 11.sp,
                    color = MacricTextMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MacricBlack)
                        .border(1.dp, MacricBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = keymapText,
                        fontSize = 10.sp,
                        color = MacricCyan,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = MacricTextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(keymapText))
                            copied = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (copied) MacricGreen else MacricViolet),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (copied) "Copied!" else "Copy Keymap", color = Color.White)
                    }
                }
            }
        }
    }
}
