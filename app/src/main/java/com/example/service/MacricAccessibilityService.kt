package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.example.data.ProfileRepository
import com.example.model.AimSmoothMode

class MacricAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "MacricAccessibility"
        private var activeInstance: MacricAccessibilityService? = null

        val isRunning: Boolean
            get() = activeInstance != null

        fun getInstance(): MacricAccessibilityService? = activeInstance

        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            if (isRunning) return true
            try {
                val expectedComponentName = "${context.packageName}/${MacricAccessibilityService::class.java.canonicalName}"
                val enabledServicesSetting = android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: return false
                val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
                colonSplitter.setString(enabledServicesSetting)
                while (colonSplitter.hasNext()) {
                    val componentNameString = colonSplitter.next()
                    if (componentNameString.equals(expectedComponentName, ignoreCase = true) ||
                        componentNameString.contains(MacricAccessibilityService::class.java.simpleName)
                    ) {
                        return true
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error checking accessibility setting", e)
            }
            return isRunning
        }

        /**
         * Dispatches a synthetic instant tap gesture at (x, y) on the screen.
         * Note: Path MUST contain at least one line segment to be valid in GestureDescription.
         */
        fun dispatchTap(
            x: Float,
            y: Float,
            durationMs: Long = 40L,
            onFinished: ((Boolean) -> Unit)? = null
        ): Boolean {
            val service = activeInstance ?: run {
                Log.w(TAG, "Cannot dispatch tap: AccessibilityService is not running.")
                onFinished?.invoke(false)
                return false
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                onFinished?.invoke(false)
                return false
            }

            val path = Path().apply {
                moveTo(x, y)
                // Android StrokeDescription requires non-empty path with at least 1 segment
                lineTo(x + 1f, y + 1f)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs.coerceIn(20L, 200L))
            val gesture = GestureDescription.Builder().addStroke(stroke).build()

            return try {
                service.dispatchGesture(gesture, object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        super.onCompleted(gestureDescription)
                        Log.d(TAG, "Direct tap completed at ($x, $y)")
                        onFinished?.invoke(true)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        super.onCancelled(gestureDescription)
                        Log.w(TAG, "Direct tap cancelled at ($x, $y)")
                        onFinished?.invoke(false)
                    }
                }, Handler(Looper.getMainLooper()))
            } catch (e: Exception) {
                Log.e(TAG, "Error dispatching tap gesture", e)
                onFinished?.invoke(false)
                false
            }
        }

        /**
         * Dispatches the Ultra-Smooth Pro Headshot Drag Gesture.
         *
         * Emulates the elite Brazilian/Indonesian Free Fire "J-Drag" and "Auto Headshot S-Curve" physics:
         * 1. High-velocity initial snap: breaks the chest auto-aim magnet lock.
         * 2. Smooth cubic Bezier arc: curves up towards the head hitbox without straight-line recoil jitter.
         * 3. Apex micro-deceleration & stabilization: keeps the crosshair locked at the head level without overshooting.
         */
        fun dispatchUltraSmoothHeadshotDrag(
            startX: Float,
            startY: Float,
            dragUpPx: Float,
            durationMs: Long = 135L,
            aimMode: AimSmoothMode = AimSmoothMode.HEADSHOT_DRAG,
            jCurveDirection: String = "RIGHT",
            onFinished: ((Boolean) -> Unit)? = null
        ): Boolean {
            val service = activeInstance ?: run {
                Log.w(TAG, "Cannot dispatch headshot drag: AccessibilityService is not running.")
                onFinished?.invoke(false)
                return false
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                onFinished?.invoke(false)
                return false
            }

            val path = Path().apply {
                moveTo(startX, startY)

                when (aimMode) {
                    AimSmoothMode.HEADSHOT_DRAG -> {
                        // Pro J-Curve Headshot Drag
                        // Free Fire drag: slight initial lateral release dip, explosive snap, then lock to head
                        val lateralSign = if (jCurveDirection.equals("LEFT", ignoreCase = true)) -1f else 1f
                        val lateralOffset = (dragUpPx * 0.18f) * lateralSign
                        val endY = (startY - dragUpPx).coerceAtLeast(10f)

                        // Control point 1: initial snap outwards and upwards (30% up)
                        val cp1X = startX + lateralOffset
                        val cp1Y = startY - (dragUpPx * 0.32f)

                        // Control point 2: curve back inward toward center head line (80% up)
                        val cp2X = startX + (lateralOffset * 0.35f)
                        val cp2Y = startY - (dragUpPx * 0.82f)

                        // Apex target with subtle 0.5px stabilizer
                        cubicTo(cp1X, cp1Y, cp2X, cp2Y, startX, endY)
                    }

                    AimSmoothMode.SMOOTH_S_CURVE -> {
                        // Smooth Symmetrical S-Curve upward drag
                        val endY = (startY - dragUpPx).coerceAtLeast(10f)
                        val midY = (startY + endY) / 2f
                        val waveOffset = (dragUpPx * 0.12f)

                        cubicTo(
                            startX + waveOffset, startY - (dragUpPx * 0.35f),
                            startX - waveOffset, startY - (dragUpPx * 0.70f),
                            startX, endY
                        )
                    }

                    AimSmoothMode.SNIPER_SNAP -> {
                        // Sniper Micro-Snap: Fast linear flick with micro head lock
                        val microSnapPx = (dragUpPx * 0.55f).coerceIn(25f, 90f)
                        val endY = (startY - microSnapPx).coerceAtLeast(10f)
                        quadTo(startX, (startY + endY) / 2f, startX, endY)
                    }

                    AimSmoothMode.SMOOTH_RECOIL -> {
                        // Anti-Recoil Stabilizer: Smooth downward compensation to fight weapon recoil
                        val recoilDistance = (dragUpPx * 0.45f).coerceIn(20f, 80f)
                        val endY = startY + recoilDistance
                        quadTo(startX + 1f, (startY + endY) / 2f, startX, endY)
                    }

                    AimSmoothMode.INSTANT_TAP -> {
                        // Instant 1ms tap
                        lineTo(startX + 1f, startY + 1f)
                    }
                }
            }

            val finalDuration = when (aimMode) {
                AimSmoothMode.SNIPER_SNAP -> 70L
                AimSmoothMode.INSTANT_TAP -> 35L
                else -> durationMs.coerceIn(80L, 400L)
            }

            val stroke = GestureDescription.StrokeDescription(path, 0L, finalDuration)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()

            return try {
                service.dispatchGesture(gesture, object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        super.onCompleted(gestureDescription)
                        Log.d(TAG, "Headshot gesture completed: $aimMode (dragUp: ${dragUpPx}px in ${finalDuration}ms)")
                        onFinished?.invoke(true)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        super.onCancelled(gestureDescription)
                        Log.w(TAG, "Headshot gesture cancelled by system")
                        onFinished?.invoke(false)
                    }
                }, Handler(Looper.getMainLooper()))
            } catch (e: Exception) {
                Log.e(TAG, "Error executing headshot gesture", e)
                onFinished?.invoke(false)
                false
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeInstance = this
        try {
            val info = serviceInfo ?: android.accessibilityservice.AccessibilityServiceInfo()
            info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            info.feedbackType = android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC
            info.flags = info.flags or
                    android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                    android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            info.notificationTimeout = 50
            serviceInfo = info
        } catch (e: Exception) {
            Log.w(TAG, "Could not set dynamic serviceInfo flags", e)
        }
        Log.i(TAG, "MacricAccessibilityService connected and ready for touch/drag gestures & key filtering.")
    }

    /**
     * Filters and intercepts keyboard and mapped mouse keys from PC Emulators (BlueStacks, LDPlayer)
     * When mouse lock is enabled in BlueStacks, the user presses their mapped key (Space, F, Left Click, etc.)
     * and this triggers the MACRIC ultra-smooth headshot drag immediately!
     */
    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!MacricOverlayService.isRunning) return super.onKeyEvent(event)

        val repo = ProfileRepository(this)
        if (!repo.isEmulatorMouseLockEnabled()) return super.onKeyEvent(event)

        val triggerKey = repo.getEmulatorTriggerKey()
        val isTargetKey = isEventMatchingKey(event.keyCode, triggerKey) || isGeneralFireKey(event.keyCode)

        if (isTargetKey) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (event.repeatCount == 0) {
                        Log.d(TAG, "Fire key [${event.keyCode} / ${triggerKey}] pressed! Executing MACRIC auto-headshot fire.")
                        MacricOverlayService.triggerActiveFire()
                    }
                    return true
                }
                KeyEvent.ACTION_UP -> {
                    MacricOverlayService.stopHoldingFire()
                    return true
                }
            }
        }

        return super.onKeyEvent(event)
    }

    private fun isGeneralFireKey(keyCode: Int): Boolean {
        return keyCode == KeyEvent.KEYCODE_BUTTON_L1 ||
                keyCode == KeyEvent.KEYCODE_BUTTON_R1 ||
                keyCode == KeyEvent.KEYCODE_BUTTON_A ||
                keyCode == KeyEvent.KEYCODE_BUTTON_1 ||
                keyCode == KeyEvent.KEYCODE_BUTTON_THUMBL ||
                keyCode == KeyEvent.KEYCODE_SPACE ||
                keyCode == KeyEvent.KEYCODE_ENTER ||
                keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
    }

    private fun isEventMatchingKey(keyCode: Int, keyName: String): Boolean {
        val normalized = keyName.trim().lowercase()
        return when (normalized) {
            "space", "spacebar" -> keyCode == KeyEvent.KEYCODE_SPACE
            "f" -> keyCode == KeyEvent.KEYCODE_F
            "e" -> keyCode == KeyEvent.KEYCODE_E
            "v" -> keyCode == KeyEvent.KEYCODE_V
            "c" -> keyCode == KeyEvent.KEYCODE_C
            "z" -> keyCode == KeyEvent.KEYCODE_Z
            "r" -> keyCode == KeyEvent.KEYCODE_R
            "q" -> keyCode == KeyEvent.KEYCODE_Q
            "t" -> keyCode == KeyEvent.KEYCODE_T
            "g" -> keyCode == KeyEvent.KEYCODE_G
            "x" -> keyCode == KeyEvent.KEYCODE_X
            "b" -> keyCode == KeyEvent.KEYCODE_B
            "enter", "return" -> keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
            "shift", "left shift" -> keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT
            "ctrl", "control" -> keyCode == KeyEvent.KEYCODE_CTRL_LEFT || keyCode == KeyEvent.KEYCODE_CTRL_RIGHT
            "alt" -> keyCode == KeyEvent.KEYCODE_ALT_LEFT || keyCode == KeyEvent.KEYCODE_ALT_RIGHT
            "volume up", "vol up" -> keyCode == KeyEvent.KEYCODE_VOLUME_UP
            "volume down", "vol down" -> keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
            "left click", "left mouse", "mouse click", "mouse 1", "lmb" -> {
                // KeyCodes passed by BlueStacks / Gamepad for fire / primary button
                keyCode == KeyEvent.KEYCODE_BUTTON_L1 ||
                        keyCode == KeyEvent.KEYCODE_BUTTON_R1 ||
                        keyCode == KeyEvent.KEYCODE_BUTTON_A ||
                        keyCode == KeyEvent.KEYCODE_BUTTON_1 ||
                        keyCode == KeyEvent.KEYCODE_BUTTON_2 ||
                        keyCode == KeyEvent.KEYCODE_SPACE ||
                        keyCode == KeyEvent.KEYCODE_ENTER ||
                        keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
            }
            "right click", "mouse 2", "rmb" -> {
                keyCode == KeyEvent.KEYCODE_BUTTON_B ||
                        keyCode == KeyEvent.KEYCODE_BUTTON_2 ||
                        keyCode == KeyEvent.KEYCODE_BACK
            }
            else -> false
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Events not needed for touch injection
    }

    override fun onInterrupt() {
        Log.w(TAG, "MacricAccessibilityService interrupted")
    }

    override fun onDestroy() {
        if (activeInstance == this) {
            activeInstance = null
        }
        super.onDestroy()
    }
}
