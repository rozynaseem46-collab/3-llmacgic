package com.example.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ProfileRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("macric_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PROFILES = "saved_profiles"
        private const val KEY_CONNECTED_GAME_ID = "connected_game_id"
        private const val KEY_FAIR_PLAY_ACCEPTED = "fair_play_accepted"
        private const val KEY_CUSTOM_GAMES = "custom_games"
        private const val KEY_HUD_LOCKED = "hud_locked_combat"
        private const val KEY_EMULATOR_MOUSE_LOCK_ENABLED = "emulator_mouse_lock_enabled"
        private const val KEY_EMULATOR_TRIGGER_KEY = "emulator_trigger_key"
    }

    fun isHudLocked(): Boolean = prefs.getBoolean(KEY_HUD_LOCKED, true)

    fun setHudLocked(locked: Boolean) {
        prefs.edit().putBoolean(KEY_HUD_LOCKED, locked).apply()
    }

    fun isEmulatorMouseLockEnabled(): Boolean = prefs.getBoolean(KEY_EMULATOR_MOUSE_LOCK_ENABLED, true)

    fun setEmulatorMouseLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_EMULATOR_MOUSE_LOCK_ENABLED, enabled).apply()
    }

    fun getEmulatorTriggerKey(): String = prefs.getString(KEY_EMULATOR_TRIGGER_KEY, "Space") ?: "Space"

    fun setEmulatorTriggerKey(key: String) {
        prefs.edit().putString(KEY_EMULATOR_TRIGGER_KEY, key).apply()
    }

    data class KnownPreset(
        val id: String,
        val name: String,
        val packageCandidates: List<String>,
        val category: String,
        val iconEmoji: String
    )

    private val presetGames = listOf(
        KnownPreset("game_ff_max", "Free Fire MAX", listOf("com.dts.freefiremax"), "Battle Royale", "🔥"),
        KnownPreset("game_ff", "Free Fire", listOf("com.dts.freefireth", "com.dts.freefire"), "Battle Royale", "🔥"),
        KnownPreset("game_bgmi", "BGMI (Battlegrounds Mobile India)", listOf("com.pubg.imobile"), "Tactical Shooter", "🎯"),
        KnownPreset("game_pubg", "PUBG Mobile", listOf("com.tencent.ig", "com.pubg.krmobile", "com.vng.pubgmobile"), "Tactical Shooter", "🎯"),
        KnownPreset("game_codm", "Call of Duty: Mobile", listOf("com.activision.callofduty.shooter", "com.garena.game.codm"), "FPS / Action", "⚡"),
        KnownPreset("game_standoff", "Standoff 2", listOf("com.axlebolt.standoff2"), "Competitive FPS", "💥"),
        KnownPreset("game_bloodstrike", "Blood Strike", listOf("com.netease.bloodstrike"), "Battle Royale FPS", "🩸"),
        KnownPreset("game_farlight", "Farlight 84", listOf("com.miraclegames.farlight84"), "Hero Shooter", "🚀"),
        KnownPreset("game_arena", "Arena Breakout", listOf("com.proximabeta.mf.uamo"), "Tactical Extraction", "🛡️")
    )

    fun isFairPlayAccepted(): Boolean = prefs.getBoolean(KEY_FAIR_PLAY_ACCEPTED, false)

    fun setFairPlayAccepted(accepted: Boolean) {
        prefs.edit().putBoolean(KEY_FAIR_PLAY_ACCEPTED, accepted).apply()
    }

    fun getConnectedGameId(): String? = prefs.getString(KEY_CONNECTED_GAME_ID, null)

    fun setConnectedGameId(gameId: String?) {
        prefs.edit().putString(KEY_CONNECTED_GAME_ID, gameId).apply()
    }

    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, 0)
            }
            true
        } catch (e: Exception) {
            context.packageManager.getLaunchIntentForPackage(packageName) != null
        }
    }

    fun getAllGames(onlyInstalled: Boolean = true): List<GameModel> {
        val pm = context.packageManager
        val installedList = mutableListOf<GameModel>()
        val foundPackages = mutableSetOf<String>()

        // 1. Check known game presets on the emulator/device
        for (preset in presetGames) {
            val installedPkg = preset.packageCandidates.firstOrNull { isPackageInstalled(it) }
            if (installedPkg != null) {
                foundPackages.add(installedPkg)
                installedList.add(
                    GameModel(
                        id = preset.id,
                        name = preset.name,
                        packageName = installedPkg,
                        category = preset.category,
                        isInstalled = true,
                        iconEmoji = preset.iconEmoji
                    )
                )
            }
        }

        // 2. Discover any other user-installed games or launcher apps
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            val ignorePrefixes = listOf(
                "com.android.", "com.google.android.", "android",
                "org.chromium.", "com.example", "com.macric.app",
                "com.cyanogenmod.", "com.qualcomm.", "com.bluestacks.",
                "com.microvirt.", "com.bignox."
            )

            for (info in resolveInfos) {
                val pkg = info.activityInfo.packageName
                if (pkg in foundPackages || pkg == context.packageName) continue
                if (ignorePrefixes.any { pkg.startsWith(it) }) continue

                val isSystem = (info.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isGame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    info.activityInfo.applicationInfo.category == ApplicationInfo.CATEGORY_GAME
                } else false

                if (isGame || !isSystem) {
                    foundPackages.add(pkg)
                    val label = try {
                        info.loadLabel(pm).toString()
                    } catch (e: Exception) {
                        pkg
                    }
                    installedList.add(
                        GameModel(
                            id = "app_" + pkg.replace(".", "_"),
                            name = label,
                            packageName = pkg,
                            category = if (isGame) "Installed Game" else "Installed App",
                            isInstalled = true,
                            iconEmoji = if (isGame) "🎮" else "📱"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. User custom added games
        val customGames = getCustomGamesList()
        for (cg in customGames) {
            val isInst = isPackageInstalled(cg.packageName)
            if (isInst && cg.packageName !in foundPackages) {
                installedList.add(cg.copy(isInstalled = true))
                foundPackages.add(cg.packageName)
            } else if (!onlyInstalled && cg.packageName !in foundPackages) {
                installedList.add(cg.copy(isInstalled = false))
                foundPackages.add(cg.packageName)
            }
        }

        // 4. If onlyInstalled is false, also append uninstalled preset games
        val finalList = if (!onlyInstalled) {
            val uninstalledPresets = presetGames.filter { preset ->
                preset.packageCandidates.none { it in foundPackages }
            }.map { preset ->
                GameModel(
                    id = preset.id,
                    name = preset.name,
                    packageName = preset.packageCandidates.first(),
                    category = preset.category,
                    isInstalled = false,
                    iconEmoji = preset.iconEmoji
                )
            }
            installedList + uninstalledPresets
        } else {
            installedList
        }

        val connectedId = getConnectedGameId()
        return finalList.map { game ->
            game.copy(isConnected = game.id == connectedId)
        }
    }

    fun getCustomGamesList(): List<GameModel> {
        val customGamesJson = prefs.getString(KEY_CUSTOM_GAMES, null) ?: return emptyList()
        val list = mutableListOf<GameModel>()
        try {
            val array = JSONArray(customGamesJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val pkg = obj.getString("packageName")
                list.add(
                    GameModel(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        packageName = pkg,
                        category = obj.optString("category", "Custom App"),
                        isCustom = true,
                        isInstalled = isPackageInstalled(pkg),
                        iconEmoji = obj.optString("iconEmoji", "🕹️")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun addCustomGame(name: String, packageName: String, emoji: String): GameModel {
        val isInst = isPackageInstalled(packageName.trim())
        val newGame = GameModel(
            id = "custom_" + UUID.randomUUID().toString().take(8),
            name = name.trim(),
            packageName = packageName.trim(),
            category = "Custom App",
            isCustom = true,
            isInstalled = isInst,
            iconEmoji = emoji.ifBlank { "🕹️" }
        )
        val current = getCustomGamesList().toMutableList()
        current.add(newGame)

        val array = JSONArray()
        current.forEach { game ->
            val obj = JSONObject().apply {
                put("id", game.id)
                put("name", game.name)
                put("packageName", game.packageName)
                put("category", game.category)
                put("iconEmoji", game.iconEmoji)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_CUSTOM_GAMES, array.toString()).apply()
        return newGame
    }

    fun getProfiles(): List<GameProfile> {
        val json = prefs.getString(KEY_PROFILES, null)
        if (json.isNullOrEmpty()) {
            val defaults = createDefaultProfiles()
            saveProfiles(defaults)
            return defaults
        }
        return try {
            parseProfilesJson(json)
        } catch (e: Exception) {
            val defaults = createDefaultProfiles()
            saveProfiles(defaults)
            defaults
        }
    }

    fun saveProfiles(profiles: List<GameProfile>) {
        val json = serializeProfilesToJson(profiles)
        prefs.edit().putString(KEY_PROFILES, json).apply()
    }

    fun saveProfile(profile: GameProfile) {
        val current = getProfiles().toMutableList()
        val index = current.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            current[index] = profile.copy(updatedAt = System.currentTimeMillis())
        } else {
            current.add(profile.copy(updatedAt = System.currentTimeMillis()))
        }
        saveProfiles(current)
    }

    fun updateFireButtonPosition(profileId: String, buttonId: String, xPercent: Float, yPercent: Float) {
        val profiles = getProfiles().toMutableList()
        val profIndex = profiles.indexOfFirst { it.id == profileId }
        if (profIndex >= 0) {
            val prof = profiles[profIndex]
            val updatedButtons = prof.fireButtons.map { btn ->
                if (btn.id == buttonId) btn.copy(xPercent = xPercent, yPercent = yPercent) else btn
            }
            val updatedProf = prof.copy(fireButtons = updatedButtons, updatedAt = System.currentTimeMillis())
            profiles[profIndex] = updatedProf
            saveProfiles(profiles)
        }
    }

    fun deleteProfile(profileId: String) {
        val current = getProfiles().filter { it.id != profileId }
        saveProfiles(current)
    }

    fun duplicateProfile(profileId: String): GameProfile? {
        val target = getProfiles().find { it.id == profileId } ?: return null
        val copy = target.copy(
            id = "prof_" + UUID.randomUUID().toString().take(8),
            name = "${target.name} (Copy)",
            updatedAt = System.currentTimeMillis()
        )
        saveProfile(copy)
        return copy
    }

    fun exportProfileJson(profile: GameProfile): String {
        return serializeProfile(profile).toString(2)
    }

    fun importProfileFromJson(jsonStr: String): GameProfile {
        val obj = JSONObject(jsonStr)
        val imported = parseSingleProfile(obj).copy(
            id = "imported_" + UUID.randomUUID().toString().take(8),
            name = obj.optString("name", "Imported Profile") + " [Imported]",
            updatedAt = System.currentTimeMillis()
        )
        saveProfile(imported)
        return imported
    }

    private fun createDefaultProfiles(): List<GameProfile> {
        return listOf(
            GameProfile(
                id = "prof_default_ff",
                name = "Free Fire Pro Setup",
                gameId = "game_ff",
                crosshair = CrosshairConfig(
                    style = CrosshairStyle.CROSS_GAP,
                    size = 32f,
                    thickness = 2.5f,
                    gap = 8f,
                    colorHex = "#9312FF"
                ),
                sensitivity = SensitivityConfig(
                    general = 95,
                    cameraLook = 90,
                    scope2x = 85,
                    scope4x = 75,
                    sniperScope = 60,
                    presetName = "Pro-Gamer"
                )
            ),
            GameProfile(
                id = "prof_default_pubg",
                name = "PUBG Precision ADS",
                gameId = "game_pubg",
                crosshair = CrosshairConfig(
                    style = CrosshairStyle.DOT,
                    size = 18f,
                    thickness = 3f,
                    gap = 0f,
                    colorHex = "#45B558"
                ),
                sensitivity = SensitivityConfig(
                    general = 60,
                    cameraLook = 55,
                    scope2x = 48,
                    scope4x = 35,
                    sniperScope = 25,
                    presetName = "Medium"
                )
            ),
            GameProfile(
                id = "prof_default_codm",
                name = "CODM Ranked Aggressive",
                gameId = "game_codm",
                crosshair = CrosshairConfig(
                    style = CrosshairStyle.CIRCLE_DOT,
                    size = 36f,
                    thickness = 2f,
                    gap = 12f,
                    colorHex = "#00E5FF"
                ),
                sensitivity = SensitivityConfig(
                    general = 80,
                    cameraLook = 75,
                    scope2x = 65,
                    scope4x = 55,
                    sniperScope = 45,
                    presetName = "High"
                )
            )
        )
    }

    private fun serializeProfilesToJson(profiles: List<GameProfile>): String {
        val array = JSONArray()
        profiles.forEach { array.put(serializeProfile(it)) }
        return array.toString()
    }

    private fun serializeProfile(p: GameProfile): JSONObject {
        val root = JSONObject()
        root.put("id", p.id)
        root.put("name", p.name)
        root.put("gameId", p.gameId)
        root.put("updatedAt", p.updatedAt)

        val ch = JSONObject().apply {
            put("enabled", p.crosshair.enabled)
            put("style", p.crosshair.style.name)
            put("size", p.crosshair.size.toDouble())
            put("thickness", p.crosshair.thickness.toDouble())
            put("gap", p.crosshair.gap.toDouble())
            put("opacity", p.crosshair.opacity.toDouble())
            put("colorHex", p.crosshair.colorHex)
            put("offsetX", p.crosshair.offsetX.toDouble())
            put("offsetY", p.crosshair.offsetY.toDouble())
            put("hotkey", p.crosshair.hotkey)
        }
        root.put("crosshair", ch)

        val buttons = JSONArray()
        p.fireButtons.forEach { fb ->
            val b = JSONObject().apply {
                put("id", fb.id)
                put("label", fb.label)
                put("xPercent", fb.xPercent.toDouble())
                put("yPercent", fb.yPercent.toDouble())
                put("size", fb.size.toDouble())
                put("opacity", fb.opacity.toDouble())
                put("colorHex", fb.colorHex)
                put("shape", fb.shape.name)
                put("mode", fb.mode.name)
                put("keyBinding", fb.keyBinding)
                put("aimSmoothMode", fb.aimSmoothMode.name)
                put("dragDistance", fb.dragDistance.toDouble())
                put("dragDurationMs", fb.dragDurationMs)
                put("jCurveDirection", fb.jCurveDirection)
            }
            buttons.put(b)
        }
        root.put("fireButtons", buttons)

        val sens = JSONObject().apply {
            put("general", p.sensitivity.general)
            put("cameraLook", p.sensitivity.cameraLook)
            put("scope2x", p.sensitivity.scope2x)
            put("scope4x", p.sensitivity.scope4x)
            put("sniperScope", p.sensitivity.sniperScope)
            put("presetName", p.sensitivity.presetName)
        }
        root.put("sensitivity", sens)

        return root
    }

    private fun parseProfilesJson(jsonStr: String): List<GameProfile> {
        val array = JSONArray(jsonStr)
        val list = mutableListOf<GameProfile>()
        for (i in 0 until array.length()) {
            list.add(parseSingleProfile(array.getJSONObject(i)))
        }
        return list
    }

    private fun parseSingleProfile(obj: JSONObject): GameProfile {
        val id = obj.getString("id")
        val name = obj.getString("name")
        val gameId = obj.optString("gameId", "game_generic")
        val updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())

        val chObj = obj.optJSONObject("crosshair")
        val crosshair = if (chObj != null) {
            val styleStr = chObj.optString("style", "CROSS_GAP")
            val style = try { CrosshairStyle.valueOf(styleStr) } catch (e: Exception) { CrosshairStyle.CROSS_GAP }
            CrosshairConfig(
                enabled = chObj.optBoolean("enabled", true),
                style = style,
                size = chObj.optDouble("size", 32.0).toFloat(),
                thickness = chObj.optDouble("thickness", 3.0).toFloat(),
                gap = chObj.optDouble("gap", 8.0).toFloat(),
                opacity = chObj.optDouble("opacity", 0.95).toFloat(),
                colorHex = chObj.optString("colorHex", "#9312FF"),
                offsetX = chObj.optDouble("offsetX", 0.0).toFloat(),
                offsetY = chObj.optDouble("offsetY", 0.0).toFloat(),
                hotkey = chObj.optString("hotkey", "H")
            )
        } else {
            CrosshairConfig()
        }

        val fbArray = obj.optJSONArray("fireButtons")
        val fireButtons = mutableListOf<FireButtonConfig>()
        if (fbArray != null) {
            for (i in 0 until fbArray.length()) {
                val b = fbArray.getJSONObject(i)
                val shapeStr = b.optString("shape", "CIRCLE")
                val shape = try { FireButtonShape.valueOf(shapeStr) } catch (e: Exception) { FireButtonShape.CIRCLE }
                val modeStr = b.optString("mode", "TAP")
                val mode = try { FireMode.valueOf(modeStr) } catch (e: Exception) { FireMode.TAP }
                val aimSmoothStr = b.optString("aimSmoothMode", "HEADSHOT_DRAG")
                val aimSmooth = try { AimSmoothMode.valueOf(aimSmoothStr) } catch (e: Exception) { AimSmoothMode.HEADSHOT_DRAG }
                val dragDist = b.optDouble("dragDistance", 85.0).toFloat()
                val dragDuration = b.optLong("dragDurationMs", 135L)
                val jDirection = b.optString("jCurveDirection", "RIGHT")
                fireButtons.add(
                    FireButtonConfig(
                        id = b.optString("id", UUID.randomUUID().toString()),
                        label = b.optString("label", "FIRE"),
                        xPercent = b.optDouble("xPercent", 50.0).toFloat(),
                        yPercent = b.optDouble("yPercent", 50.0).toFloat(),
                        size = b.optDouble("size", 64.0).toFloat(),
                        opacity = b.optDouble("opacity", 0.85).toFloat(),
                        colorHex = b.optString("colorHex", "#9312FF"),
                        shape = shape,
                        mode = mode,
                        keyBinding = b.optString("keyBinding", "Left Click"),
                        aimSmoothMode = aimSmooth,
                        dragDistance = dragDist,
                        dragDurationMs = dragDuration,
                        jCurveDirection = jDirection
                    )
                )
            }
        }

        val sensObj = obj.optJSONObject("sensitivity")
        val sensitivity = if (sensObj != null) {
            SensitivityConfig(
                general = sensObj.optInt("general", 68),
                cameraLook = sensObj.optInt("cameraLook", 72),
                scope2x = sensObj.optInt("scope2x", 54),
                scope4x = sensObj.optInt("scope4x", 42),
                sniperScope = sensObj.optInt("sniperScope", 30),
                presetName = sensObj.optString("presetName", "Custom")
            )
        } else {
            SensitivityConfig()
        }

        return GameProfile(
            id = id,
            name = name,
            gameId = gameId,
            crosshair = crosshair,
            fireButtons = if (fireButtons.isEmpty()) GameProfile(id = "", name = "", gameId = "").fireButtons else fireButtons,
            sensitivity = sensitivity,
            updatedAt = updatedAt
        )
    }
}
