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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.GameModel
import com.example.ui.theme.*

@Composable
fun GamesConnectScreen(
    games: List<GameModel>,
    connectedGame: GameModel?,
    searchQuery: String,
    showOnlyInstalled: Boolean = true,
    onSearchQueryChange: (String) -> Unit,
    onToggleOnlyInstalled: (Boolean) -> Unit = {},
    onRefreshGames: () -> Unit = {},
    onConnectGame: (GameModel) -> Unit,
    onDisconnectGame: () -> Unit,
    onAddCustomGame: (String, String, String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredGames = remember(games, searchQuery) {
        if (searchQuery.isBlank()) games
        else games.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MacricBlack)
            .padding(16.dp)
            .testTag("games_connect_screen")
    ) {
        // Screen Title & Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Connected Games",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MacricTextPrimary
                )
                Text(
                    text = if (showOnlyInstalled) "Showing games detected on this emulator" else "Showing all preset games",
                    fontSize = 11.sp,
                    color = MacricTextMuted
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Refresh / Scan Button
                IconButton(
                    onClick = onRefreshGames,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MacricDarkCardElevated)
                        .border(1.dp, MacricBorder, CircleShape)
                        .testTag("refresh_games_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Scan for installed games",
                        tint = MacricVioletLight,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MacricViolet),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_custom_app_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Add Custom", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_game_input"),
            placeholder = { Text("Search installed game or package...", fontSize = 12.sp, color = MacricTextMuted) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MacricTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MacricTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MacricDarkCard,
                unfocusedContainerColor = MacricDarkCard,
                focusedBorderColor = MacricViolet,
                unfocusedBorderColor = MacricBorder,
                focusedTextColor = MacricTextPrimary,
                unfocusedTextColor = MacricTextPrimary
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips: Installed on Device vs All Presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = showOnlyInstalled,
                onClick = { onToggleOnlyInstalled(true) },
                label = {
                    Text(
                        text = "Installed Only (${games.count { it.isInstalled }})",
                        fontSize = 11.sp,
                        fontWeight = if (showOnlyInstalled) FontWeight.Bold else FontWeight.Normal
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (showOnlyInstalled) MacricGreen else MacricTextMuted
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MacricViolet.copy(alpha = 0.25f),
                    selectedLabelColor = Color.White,
                    containerColor = MacricDarkCard,
                    labelColor = MacricTextMuted
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = showOnlyInstalled,
                    borderColor = MacricBorder,
                    selectedBorderColor = MacricViolet
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("filter_installed_chip")
            )

            FilterChip(
                selected = !showOnlyInstalled,
                onClick = { onToggleOnlyInstalled(false) },
                label = {
                    Text(
                        text = "All Presets (${games.size})",
                        fontSize = 11.sp,
                        fontWeight = if (!showOnlyInstalled) FontWeight.Bold else FontWeight.Normal
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (!showOnlyInstalled) MacricVioletLight else MacricTextMuted
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MacricViolet.copy(alpha = 0.25f),
                    selectedLabelColor = Color.White,
                    containerColor = MacricDarkCard,
                    labelColor = MacricTextMuted
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = !showOnlyInstalled,
                    borderColor = MacricBorder,
                    selectedBorderColor = MacricViolet
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("filter_all_presets_chip")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Currently Connected Banner (if any)
        if (connectedGame != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MacricGreen.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = MacricDarkCardElevated),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = connectedGame.iconEmoji,
                            fontSize = 26.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MacricGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CURRENTLY CONNECTED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MacricGreen,
                                    letterSpacing = 0.8.sp
                                )
                            }
                            Text(
                                text = connectedGame.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MacricTextPrimary
                            )
                            Text(
                                text = connectedGame.packageName,
                                fontSize = 10.sp,
                                color = MacricTextMuted
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onDisconnectGame,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MacricRed),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(MacricRed.copy(alpha = 0.6f))),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("disconnect_active_game_btn")
                    ) {
                        Text("Disconnect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // List of Games
        if (filteredGames.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = MacricVioletLight.copy(alpha = 0.7f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (showOnlyInstalled) "No Installed Games Found" else "No Games Matched",
                        color = MacricTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (showOnlyInstalled)
                            "No supported game (Free Fire, PUBG, etc.) is currently detected on this emulator. You can install a game or tap 'Add Custom' to add any app package name."
                        else
                            "No games matched your search query. Try another keyword or add a custom app package.",
                        color = MacricTextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onRefreshGames,
                            colors = ButtonDefaults.buttonColors(containerColor = MacricViolet),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan Again", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        if (showOnlyInstalled) {
                            OutlinedButton(
                                onClick = { onToggleOnlyInstalled(false) },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MacricVioletLight),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(MacricBorder)),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("View Presets", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredGames, key = { it.id }) { game ->
                    GameRowItem(
                        game = game,
                        isConnected = game.id == connectedGame?.id,
                        onConnect = { onConnectGame(game) },
                        onDisconnect = onDisconnectGame
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddCustomGameDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, pkg, emoji ->
                onAddCustomGame(name, pkg, emoji)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun GameRowItem(
    game: GameModel,
    isConnected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isConnected) MacricGreen.copy(alpha = 0.5f) else MacricBorder,
                RoundedCornerShape(14.dp)
            )
            .testTag("game_row_${game.id}"),
        colors = CardDefaults.cardColors(containerColor = MacricDarkCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = game.iconEmoji,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = game.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MacricTextPrimary
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        if (game.isInstalled) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MacricGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "INSTALLED",
                                    fontSize = 9.sp,
                                    color = MacricGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MacricTextMuted.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "PRESET",
                                    fontSize = 9.sp,
                                    color = MacricTextMuted,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (game.isCustom) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MacricViolet.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Custom",
                                    fontSize = 9.sp,
                                    color = MacricVioletLight,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Text(
                        text = game.packageName,
                        fontSize = 10.sp,
                        color = MacricTextMuted
                    )

                    Text(
                        text = game.category,
                        fontSize = 10.sp,
                        color = MacricTextMuted.copy(alpha = 0.7f)
                    )
                }
            }

            if (isConnected) {
                OutlinedButton(
                    onClick = onDisconnect,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MacricRed),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(MacricRed.copy(alpha = 0.5f))),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_disconnect_${game.id}")
                ) {
                    Text("Disconnect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onConnect,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MacricViolet),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_connect_${game.id}")
                ) {
                    Text("Connect", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun AddCustomGameDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, pkg: String, emoji: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🕹️") }
    val emojiOptions = listOf("🕹️", "🎯", "⚡", "🔥", "💥", "🚀", "👑", "🛡️", "🤖", "⚔️")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, MacricViolet, RoundedCornerShape(16.dp))
                .testTag("add_custom_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MacricDarkCardElevated)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Add Custom Game or App",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MacricTextPrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "App Name", fontSize = 11.sp, color = MacricTextMuted)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. Shooter Demo", fontSize = 12.sp, color = MacricTextMuted) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_app_name_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(text = "Android Package Name", fontSize = 11.sp, color = MacricTextMuted)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    placeholder = { Text("e.g. com.example.mygame", fontSize = 12.sp, color = MacricTextMuted) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_app_pkg_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(text = "Choose Icon", fontSize = 11.sp, color = MacricTextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    emojiOptions.take(5).forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedEmoji == emoji) MacricViolet else MacricDarkCard)
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 18.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MacricTextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && packageName.isNotBlank()) {
                                onAdd(name, packageName, selectedEmoji)
                            }
                        },
                        enabled = name.isNotBlank() && packageName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MacricViolet),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("save_custom_app_btn")
                    ) {
                        Text("Add & Connect", color = Color.White)
                    }
                }
            }
        }
    }
}
