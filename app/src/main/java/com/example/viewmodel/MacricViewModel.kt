package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ProfileRepository
import com.example.model.*
import com.example.service.MacricOverlayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class MacricTab(val title: String) {
    HOME("Home"),
    GAMES("Games"),
    CROSSHAIR("Crosshair"),
    FIRE_BUTTON("Fire Button"),
    SENSITIVITY("Sensitivity"),
    PROFILES("Profiles"),
    DOWNLOAD("Download")
}

data class MacricUiState(
    val currentTab: MacricTab = MacricTab.HOME,
    val games: List<GameModel> = emptyList(),
    val connectedGame: GameModel? = null,
    val profiles: List<GameProfile> = emptyList(),
    val activeProfile: GameProfile = GameProfile(id = "init", name = "Default Setup", gameId = ""),
    val isOverlayActive: Boolean = false,
    val showFairPlayDialog: Boolean = false,
    val toastMessage: String? = null,
    val searchQuery: String = "",
    val showOnlyInstalled: Boolean = true
)

class MacricViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProfileRepository(application)
    private val _uiState = MutableStateFlow(MacricUiState())
    val uiState: StateFlow<MacricUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        val games = repository.getAllGames(onlyInstalled = true)
        val connectedId = repository.getConnectedGameId()
        val connectedGame = games.find { it.id == connectedId }
        val profiles = repository.getProfiles()
        val active = profiles.find { it.gameId == connectedId } ?: profiles.firstOrNull() ?: GameProfile(
            id = "default",
            name = "Default Profile",
            gameId = connectedId ?: "game_generic"
        )
        val fairPlayAccepted = repository.isFairPlayAccepted()

        _uiState.update {
            it.copy(
                games = games,
                connectedGame = connectedGame,
                profiles = profiles,
                activeProfile = active,
                showFairPlayDialog = !fairPlayAccepted,
                isOverlayActive = MacricOverlayService.isRunning,
                showOnlyInstalled = true
            )
        }
    }

    fun selectTab(tab: MacricTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun refreshGames() {
        val onlyInstalled = _uiState.value.showOnlyInstalled
        val games = repository.getAllGames(onlyInstalled = onlyInstalled)
        val connectedId = repository.getConnectedGameId()
        val connectedGame = games.find { it.id == connectedId }
        _uiState.update {
            it.copy(
                games = games,
                connectedGame = connectedGame,
                toastMessage = if (games.isEmpty()) "No games detected on device" else "Refreshed: ${games.size} games found"
            )
        }
    }

    fun toggleOnlyInstalled(onlyInstalled: Boolean) {
        val games = repository.getAllGames(onlyInstalled = onlyInstalled)
        val connectedId = repository.getConnectedGameId()
        val connectedGame = games.find { it.id == connectedId }
        _uiState.update {
            it.copy(
                showOnlyInstalled = onlyInstalled,
                games = games,
                connectedGame = connectedGame
            )
        }
    }

    fun connectGame(game: GameModel) {
        repository.setConnectedGameId(game.id)
        val updatedGames = repository.getAllGames(onlyInstalled = _uiState.value.showOnlyInstalled)
        val existingProfile = repository.getProfiles().find { it.gameId == game.id }
        val targetProfile = if (existingProfile != null) {
            existingProfile
        } else {
            val newProfile = GameProfile(
                id = "prof_" + UUID.randomUUID().toString().take(8),
                name = "${game.name} Profile",
                gameId = game.id
            )
            repository.saveProfile(newProfile)
            newProfile
        }

        _uiState.update {
            it.copy(
                games = updatedGames,
                connectedGame = game.copy(isConnected = true),
                profiles = repository.getProfiles(),
                activeProfile = targetProfile,
                currentTab = MacricTab.CROSSHAIR,
                toastMessage = "Connected to ${game.name}"
            )
        }
        notifyOverlayService()
    }

    fun disconnectGame() {
        val prevName = _uiState.value.connectedGame?.name ?: "Game"
        repository.setConnectedGameId(null)
        val updatedGames = repository.getAllGames(onlyInstalled = _uiState.value.showOnlyInstalled)

        _uiState.update {
            it.copy(
                games = updatedGames,
                connectedGame = null,
                toastMessage = "Disconnected from $prevName"
            )
        }
        notifyOverlayService()
    }

    fun addCustomGame(name: String, pkg: String, emoji: String) {
        if (name.isBlank() || pkg.isBlank()) return
        val newGame = repository.addCustomGame(name.trim(), pkg.trim(), emoji.trim())
        val updatedGames = repository.getAllGames(onlyInstalled = _uiState.value.showOnlyInstalled)
        _uiState.update {
            it.copy(
                games = updatedGames,
                toastMessage = "Added ${newGame.name}"
            )
        }
        connectGame(newGame)
    }

    fun updateCrosshair(crosshair: CrosshairConfig) {
        val updated = _uiState.value.activeProfile.copy(crosshair = crosshair)
        _uiState.update { it.copy(activeProfile = updated) }
        repository.saveProfile(updated)
        notifyOverlayService()
    }

    fun updateFireButtons(buttons: List<FireButtonConfig>) {
        val updated = _uiState.value.activeProfile.copy(fireButtons = buttons)
        _uiState.update { it.copy(activeProfile = updated) }
        repository.saveProfile(updated)
        notifyOverlayService()
    }

    fun addFireButton() {
        val current = _uiState.value.activeProfile.fireButtons.toMutableList()
        val count = current.size + 1
        val newBtn = FireButtonConfig(
            id = "fire_btn_" + UUID.randomUUID().toString().take(6),
            label = "FIRE $count",
            xPercent = 50f,
            yPercent = 50f,
            size = 64f,
            opacity = 0.85f,
            colorHex = "#9312FF",
            shape = FireButtonShape.CIRCLE,
            mode = FireMode.TAP,
            keyBinding = "Space"
        )
        current.add(newBtn)
        updateFireButtons(current)
        showToast("Fire button added")
    }

    fun removeFireButton(id: String) {
        val current = _uiState.value.activeProfile.fireButtons.filter { it.id != id }
        updateFireButtons(current)
        showToast("Fire button removed")
    }

    fun updateSensitivity(sensitivity: SensitivityConfig) {
        val updated = _uiState.value.activeProfile.copy(sensitivity = sensitivity)
        _uiState.update { it.copy(activeProfile = updated) }
        repository.saveProfile(updated)
    }

    fun saveActiveProfile() {
        val current = _uiState.value.activeProfile
        repository.saveProfile(current)
        val refreshed = repository.getProfiles()
        _uiState.update {
            it.copy(
                profiles = refreshed,
                toastMessage = "Profile saved: ${current.name}"
            )
        }
        notifyOverlayService()
    }

    fun selectProfile(profile: GameProfile) {
        _uiState.update {
            it.copy(
                activeProfile = profile,
                toastMessage = "Loaded profile: ${profile.name}"
            )
        }
        notifyOverlayService()
    }

    fun duplicateActiveProfile() {
        val dup = repository.duplicateProfile(_uiState.value.activeProfile.id)
        if (dup != null) {
            _uiState.update {
                it.copy(
                    profiles = repository.getProfiles(),
                    activeProfile = dup,
                    toastMessage = "Profile duplicated: ${dup.name}"
                )
            }
        }
    }

    fun deleteProfile(id: String) {
        repository.deleteProfile(id)
        val refreshed = repository.getProfiles()
        val nextActive = refreshed.firstOrNull() ?: GameProfile(id = "def", name = "Default", gameId = "")
        _uiState.update {
            it.copy(
                profiles = refreshed,
                activeProfile = nextActive,
                toastMessage = "Profile deleted"
            )
        }
    }

    fun exportProfileJson(): String {
        return repository.exportProfileJson(_uiState.value.activeProfile)
    }

    fun importProfileJson(json: String) {
        try {
            val imported = repository.importProfileFromJson(json)
            _uiState.update {
                it.copy(
                    profiles = repository.getProfiles(),
                    activeProfile = imported,
                    toastMessage = "Imported profile: ${imported.name}"
                )
            }
        } catch (e: Exception) {
            showToast("Failed to import profile: Invalid JSON format")
        }
    }

    fun acceptFairPlay() {
        repository.setFairPlayAccepted(true)
        _uiState.update { it.copy(showFairPlayDialog = false) }
    }

    fun dismissFairPlay() {
        _uiState.update { it.copy(showFairPlayDialog = false) }
    }

    fun toggleOverlay(canDrawOverlay: Boolean, onRequestPermission: () -> Unit) {
        val context = getApplication<Application>()
        if (!canDrawOverlay) {
            onRequestPermission()
            return
        }

        if (_uiState.value.isOverlayActive) {
            val intent = Intent(context, MacricOverlayService::class.java).apply {
                action = MacricOverlayService.ACTION_STOP_OVERLAY
            }
            context.startService(intent)
            _uiState.update { it.copy(isOverlayActive = false, toastMessage = "Overlay HUD stopped") }
        } else {
            val intent = Intent(context, MacricOverlayService::class.java).apply {
                action = MacricOverlayService.ACTION_START_OVERLAY
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            _uiState.update { it.copy(isOverlayActive = true, toastMessage = "Overlay HUD launched!") }
        }
    }

    private fun notifyOverlayService() {
        if (_uiState.value.isOverlayActive) {
            val context = getApplication<Application>()
            val intent = Intent(context, MacricOverlayService::class.java).apply {
                action = MacricOverlayService.ACTION_UPDATE_CONFIG
            }
            context.startService(intent)
        }
    }

    fun showToast(msg: String) {
        _uiState.update { it.copy(toastMessage = msg) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
