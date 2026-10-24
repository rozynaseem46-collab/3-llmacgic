package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.GameModel
import com.example.model.GameProfile
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfilesScreen(
    profiles: List<GameProfile>,
    activeProfile: GameProfile,
    games: List<GameModel>,
    onSelectProfile: (GameProfile) -> Unit,
    onSaveCurrentProfile: () -> Unit,
    onDuplicateProfile: () -> Unit,
    onDeleteProfile: (String) -> Unit,
    onExportJson: () -> String,
    onImportJson: (String) -> Unit
) {
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<GameProfile?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MacricBlack)
            .padding(16.dp)
            .testTag("profiles_screen_root")
    ) {
        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Control Profiles",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MacricTextPrimary
                )
                Text(
                    text = "Manage, duplicate, export & import per-game configs",
                    fontSize = 11.sp,
                    color = MacricTextMuted
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Export JSON
                IconButton(
                    onClick = { showExportDialog = true },
                    modifier = Modifier.size(34.dp).background(MacricDarkCard, CircleShape)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = "Export JSON", tint = MacricVioletLight, modifier = Modifier.size(18.dp))
                }

                // Import JSON
                IconButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier.size(34.dp).background(MacricDarkCard, CircleShape)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Import JSON", tint = MacricCyan, modifier = Modifier.size(18.dp))
                }

                // Save Current Profile
                Button(
                    onClick = onSaveCurrentProfile,
                    colors = ButtonDefaults.buttonColors(containerColor = MacricViolet),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("save_profile_button")
                ) {
                    Text("Save Active", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Active Profile Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MacricVioletLight, RoundedCornerShape(16.dp)),
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
                            text = "CURRENTLY LOADED PROFILE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MacricGreen,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Row {
                        IconButton(
                            onClick = onDuplicateProfile,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = MacricTextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = activeProfile.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MacricTextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                val gameName = games.find { it.id == activeProfile.gameId }?.name ?: "All Games"
                Text(
                    text = "Linked Game: $gameName · Reticle: ${activeProfile.crosshair.style.displayName} · Buttons: ${activeProfile.fireButtons.size}",
                    fontSize = 11.sp,
                    color = MacricTextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Saved Profiles (${profiles.size})",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MacricTextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(profiles, key = { it.id }) { profile ->
                val isActive = profile.id == activeProfile.id
                val dateStr = remember(profile.updatedAt) {
                    SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()).format(Date(profile.updatedAt))
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isActive) MacricViolet else MacricBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onSelectProfile(profile) }
                        .testTag("profile_item_${profile.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) MacricDarkCardElevated else MacricDarkCard
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = profile.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) MacricVioletLight else MacricTextPrimary
                                )
                                if (isActive) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MacricGreen.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MacricGreen)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "Crosshair: ${profile.crosshair.style.displayName} | Gen Sens: ${profile.sensitivity.general}%",
                                fontSize = 10.sp,
                                color = MacricTextMuted
                            )

                            Text(
                                text = "Updated: $dateStr",
                                fontSize = 9.sp,
                                color = MacricTextMuted.copy(alpha = 0.7f)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isActive) {
                                OutlinedButton(
                                    onClick = { onSelectProfile(profile) },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text("Load", fontSize = 10.sp)
                                }
                            }

                            if (profiles.size > 1) {
                                IconButton(
                                    onClick = { onDeleteProfile(profile.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MacricRed.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Export Dialog
    if (showExportDialog) {
        val exportedJson = remember { onExportJson() }
        val clipboard = LocalClipboardManager.current
        var copied by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showExportDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, MacricViolet, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MacricDarkCardElevated),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Export Profile JSON", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MacricTextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Save this configuration or share it with other players:", fontSize = 11.sp, color = MacricTextMuted)
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MacricBlack)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = exportedJson,
                            fontSize = 9.sp,
                            color = MacricCyan,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showExportDialog = false }) {
                            Text("Close", color = MacricTextMuted)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                clipboard.setText(AnnotatedString(exportedJson))
                                copied = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (copied) MacricGreen else MacricViolet)
                        ) {
                            Text(if (copied) "Copied!" else "Copy to Clipboard", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Import Dialog
    if (showImportDialog) {
        var importInput by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showImportDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, MacricCyan, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MacricDarkCardElevated),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Import Profile JSON", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MacricTextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Paste a MACRIC profile JSON string below:", fontSize = 11.sp, color = MacricTextMuted)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = importInput,
                        onValueChange = { importInput = it },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        placeholder = { Text("Paste JSON here...", fontSize = 11.sp, color = MacricTextMuted) },
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showImportDialog = false }) {
                            Text("Cancel", color = MacricTextMuted)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (importInput.isNotBlank()) {
                                    onImportJson(importInput)
                                    showImportDialog = false
                                }
                            },
                            enabled = importInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = MacricCyan)
                        ) {
                            Text("Import Profile", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
