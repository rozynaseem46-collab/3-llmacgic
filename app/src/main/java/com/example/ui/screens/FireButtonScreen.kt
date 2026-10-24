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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.FireButtonConfig
import com.example.model.FireButtonShape
import com.example.model.FireMode
import com.example.ui.theme.*

@Composable
fun FireButtonScreen(
    fireButtons: List<FireButtonConfig>,
    onFireButtonsChange: (List<FireButtonConfig>) -> Unit,
    onAddFireButton: () -> Unit,
    onRemoveFireButton: (String) -> Unit,
    onSaveProfile: () -> Unit
) {
    val scrollState = rememberScrollState()
    var selectedButtonId by remember(fireButtons) {
        mutableStateOf(fireButtons.firstOrNull()?.id ?: "")
    }

    var snapToGrid by remember { mutableStateOf(true) }
    var lockLayout by remember { mutableStateOf(false) }
    var showExportKeymapDialog by remember { mutableStateOf(false) }

    val activeSelected = fireButtons.find { it.id == selectedButtonId } ?: fireButtons.firstOrNull()

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
                    text = "Fire Button Studio",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MacricTextPrimary
                )
                Text(
                    text = "HUD positioning, emulator bindings & layout editor",
                    fontSize = 11.sp,
                    color = MacricTextMuted
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { showExportKeymapDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MacricCyan),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(MacricCyan.copy(alpha = 0.6f))),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("export_emulator_keymap_btn")
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Emulator Keymap", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onSaveProfile,
                    colors = ButtonDefaults.buttonColors(containerColor = MacricViolet),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("fire_button_save_btn")
                ) {
                    Text("Save", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Toolbar: Add button, Snap to Grid toggle, Lock Layout toggle
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
                // Snap to Grid
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

                // Lock Layout
                IconButton(
                    onClick = { lockLayout = !lockLayout },
                    modifier = Modifier
                        .size(32.dp)
                        .background(if (lockLayout) MacricRed.copy(alpha = 0.3f) else MacricDarkCard, CircleShape)
                ) {
                    Icon(
                        imageVector = if (lockLayout) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Lock layout",
                        tint = if (lockLayout) MacricRed else MacricTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
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
                        text = if (lockLayout) "LAYOUT LOCKED (Tap to select button)" else "DRAG BUTTONS TO EDIT HUD POSITION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (lockLayout) MacricRed else MacricTextMuted,
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
                                .pointerInput(lockLayout, snapToGrid) {
                                    if (!lockLayout) {
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
                text = "Customize Selected: ${activeSelected.label}",
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Keybinding Picker
                    Text(text = "Emulator Key / Mouse Binding", fontSize = 11.sp, color = MacricTextMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    val keyOptions = listOf("Left Click", "Right Click", "Space", "F", "E", "C", "Shift", "V")
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

                    // Shape: Circle vs Rounded Square
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

                    // Fire Mode: Tap vs Hold (Fair Play compliance explanation)
                    Text(text = "Trigger Mode (Strict Fair-Play)", fontSize = 11.sp, color = MacricTextMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FireMode.values().forEach { mode ->
                            val isModeSelected = activeSelected.mode == mode
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.dp,
                                        if (isModeSelected) MacricViolet else MacricBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        val updated = fireButtons.map {
                                            if (it.id == activeSelected.id) it.copy(mode = mode) else it
                                        }
                                        onFireButtonsChange(updated)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isModeSelected) MacricViolet.copy(alpha = 0.2f) else MacricDarkCardElevated
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = mode.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isModeSelected) MacricVioletLight else MacricTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = mode.description,
                                        fontSize = 9.sp,
                                        color = MacricTextMuted,
                                        lineHeight = 12.sp
                                    )
                                }
                            }
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
                            valueRange = 40f..100f,
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
