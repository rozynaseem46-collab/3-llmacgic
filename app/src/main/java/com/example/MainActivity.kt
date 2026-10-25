package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.FairPlayDialog
import com.example.ui.components.MacricHeader
import com.example.ui.screens.*
import com.example.ui.theme.MacricBlack
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MacricTab
import com.example.viewmodel.MacricViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MacricViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }
                val context = LocalContext.current

                // Show toast messages via Snackbar
                LaunchedEffect(uiState.toastMessage) {
                    uiState.toastMessage?.let { msg ->
                        snackbarHostState.showSnackbar(
                            message = msg,
                            duration = SnackbarDuration.Short
                        )
                        viewModel.clearToast()
                    }
                }

                // Handle system back navigation
                BackHandler(enabled = uiState.currentTab != MacricTab.HOME) {
                    viewModel.selectTab(MacricTab.HOME)
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MacricBlack),
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        MacricHeader(
                            currentTab = uiState.currentTab,
                            connectedGame = uiState.connectedGame,
                            isOverlayActive = uiState.isOverlayActive,
                            onTabSelected = { viewModel.selectTab(it) },
                            onToggleOverlay = {
                                val canDraw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    Settings.canDrawOverlays(this@MainActivity)
                                } else {
                                    true
                                }
                                val willBeActive = !uiState.isOverlayActive
                                viewModel.toggleOverlay(canDraw) {
                                    requestOverlayPermission()
                                }
                                if (canDraw && willBeActive && !com.example.service.MacricAccessibilityService.isAccessibilityServiceEnabled(this@MainActivity)) {
                                    viewModel.showToast("Enable 'MACRIC' in Accessibility Settings for real in-game auto-headshot drag!")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MacricBlack)
                    ) {
                        when (uiState.currentTab) {
                            MacricTab.HOME -> {
                                HomeScreen(
                                    connectedGame = uiState.connectedGame,
                                    activeProfile = uiState.activeProfile,
                                    isOverlayActive = uiState.isOverlayActive,
                                    onNavigateTab = { viewModel.selectTab(it) },
                                    onToggleOverlay = {
                                        val canDraw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            Settings.canDrawOverlays(this@MainActivity)
                                        } else {
                                            true
                                        }
                                        viewModel.toggleOverlay(canDraw) {
                                            requestOverlayPermission()
                                        }
                                    }
                                )
                            }
                            MacricTab.GAMES -> {
                                GamesConnectScreen(
                                    games = uiState.games,
                                    connectedGame = uiState.connectedGame,
                                    searchQuery = uiState.searchQuery,
                                    showOnlyInstalled = uiState.showOnlyInstalled,
                                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                    onToggleOnlyInstalled = { viewModel.toggleOnlyInstalled(it) },
                                    onRefreshGames = { viewModel.refreshGames() },
                                    onConnectGame = { viewModel.connectGame(it) },
                                    onDisconnectGame = { viewModel.disconnectGame() },
                                    onAddCustomGame = { name, pkg, emoji ->
                                        viewModel.addCustomGame(name, pkg, emoji)
                                    }
                                )
                            }
                            MacricTab.CROSSHAIR -> {
                                CrosshairScreen(
                                    crosshair = uiState.activeProfile.crosshair,
                                    onCrosshairChange = { viewModel.updateCrosshair(it) },
                                    onSaveProfile = { viewModel.saveActiveProfile() }
                                )
                            }
                            MacricTab.FIRE_BUTTON -> {
                                FireButtonScreen(
                                    fireButtons = uiState.activeProfile.fireButtons,
                                    onFireButtonsChange = { viewModel.updateFireButtons(it) },
                                    onAddFireButton = { viewModel.addFireButton() },
                                    onRemoveFireButton = { viewModel.removeFireButton(it) },
                                    onSaveProfile = { viewModel.saveActiveProfile() },
                                    onRequestAccessibilitySettings = { openAccessibilitySettings() }
                                )
                            }
                            MacricTab.SENSITIVITY -> {
                                SensitivityScreen(
                                    sensitivity = uiState.activeProfile.sensitivity,
                                    onSensitivityChange = { viewModel.updateSensitivity(it) },
                                    onSaveProfile = { viewModel.saveActiveProfile() }
                                )
                            }
                            MacricTab.PROFILES -> {
                                ProfilesScreen(
                                    profiles = uiState.profiles,
                                    activeProfile = uiState.activeProfile,
                                    games = uiState.games,
                                    onSelectProfile = { viewModel.selectProfile(it) },
                                    onSaveCurrentProfile = { viewModel.saveActiveProfile() },
                                    onDuplicateProfile = { viewModel.duplicateActiveProfile() },
                                    onDeleteProfile = { viewModel.deleteProfile(it) },
                                    onExportJson = { viewModel.exportProfileJson() },
                                    onImportJson = { viewModel.importProfileJson(it) }
                                )
                            }
                            MacricTab.DOWNLOAD -> {
                                DownloadPortalScreen(
                                    onTriggerToast = { viewModel.showToast(it) }
                                )
                            }
                        }

                        // Mandatory Fair Play Disclaimer dialog on first launch
                        if (uiState.showFairPlayDialog) {
                            FairPlayDialog(
                                onAccept = { viewModel.acceptFairPlay() },
                                onDismiss = { viewModel.dismissFairPlay() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intentWithPackage = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            try {
                startActivity(intentWithPackage)
            } catch (e: Exception) {
                try {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                } catch (e2: Exception) {
                    try {
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                    } catch (e3: Exception) {
                        viewModel.showToast("Could not open overlay settings. Please enable manually in Android Settings.")
                        return
                    }
                }
            }
            viewModel.showToast("Please grant 'Display over other apps' to enable HUD")
        }
    }

    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            viewModel.showToast("Find 'MACRIC' in Accessibility and turn it ON to enable in-game fire taps")
        } catch (e: Exception) {
            viewModel.showToast("Open Android Settings > Accessibility to enable MACRIC")
        }
    }
}
