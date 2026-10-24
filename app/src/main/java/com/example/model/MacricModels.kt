package com.example.model

enum class CrosshairStyle(val displayName: String) {
    DOT("Dot"),
    PLUS("Plus"),
    CIRCLE("Circle"),
    CROSS_GAP("Cross with Gap"),
    CIRCLE_DOT("Circle + Dot"),
    T_SHAPE("T-Shape"),
    CHEVRON("Chevron"),
    DIAMOND("Diamond")
}

data class CrosshairConfig(
    val enabled: Boolean = true,
    val style: CrosshairStyle = CrosshairStyle.CROSS_GAP,
    val size: Float = 36f,
    val thickness: Float = 3f,
    val gap: Float = 10f,
    val opacity: Float = 0.95f,
    val colorHex: String = "#9312FF",
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val hotkey: String = "H"
)

enum class FireButtonShape(val displayName: String) {
    CIRCLE("Circle"),
    ROUNDED_SQUARE("Rounded Square"),
    HEXAGON("Hexagon")
}

enum class FireMode(val displayName: String, val description: String) {
    TAP("Tap Mode", "1 tap = 1 single clean trigger input. Fully fair-play compliant."),
    HOLD("Hold Mode", "Held down continuously for automatic weapons. No macros or burst scripts.")
}

data class FireButtonConfig(
    val id: String,
    val label: String,
    val xPercent: Float, // 0..100 relative to landscape game viewport
    val yPercent: Float, // 0..100 relative to landscape game viewport
    val size: Float = 64f,
    val opacity: Float = 0.85f,
    val colorHex: String = "#9312FF",
    val shape: FireButtonShape = FireButtonShape.CIRCLE,
    val mode: FireMode = FireMode.TAP,
    val keyBinding: String = "Left Mouse"
)

data class SensitivityConfig(
    val general: Int = 68,
    val cameraLook: Int = 72,
    val scope2x: Int = 54,
    val scope4x: Int = 42,
    val sniperScope: Int = 30,
    val presetName: String = "Custom"
)

data class GameModel(
    val id: String,
    val name: String,
    val packageName: String,
    val category: String = "Battle Royale / Shooter",
    val isConnected: Boolean = false,
    val isCustom: Boolean = false,
    val isInstalled: Boolean = true,
    val iconEmoji: String = "🎮"
)

data class GameProfile(
    val id: String,
    val name: String,
    val gameId: String,
    val crosshair: CrosshairConfig = CrosshairConfig(),
    val fireButtons: List<FireButtonConfig> = listOf(
        FireButtonConfig(
            id = "left_fire",
            label = "L-FIRE",
            xPercent = 18f,
            yPercent = 68f,
            size = 64f,
            colorHex = "#9312FF",
            shape = FireButtonShape.CIRCLE,
            mode = FireMode.TAP,
            keyBinding = "F"
        ),
        FireButtonConfig(
            id = "right_fire",
            label = "FIRE",
            xPercent = 82f,
            yPercent = 68f,
            size = 76f,
            colorHex = "#A020FF",
            shape = FireButtonShape.CIRCLE,
            mode = FireMode.TAP,
            keyBinding = "Left Click"
        )
    ),
    val sensitivity: SensitivityConfig = SensitivityConfig(),
    val updatedAt: Long = System.currentTimeMillis()
)
