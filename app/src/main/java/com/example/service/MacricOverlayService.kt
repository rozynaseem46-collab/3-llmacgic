package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.ProfileRepository
import com.example.model.*

class MacricOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var crosshairView: View? = null
    private var isCrosshairAttached = false
    private val fireButtonViews: MutableList<FireButtonCanvasView> = mutableListOf()
    private var hudControlBarView: View? = null
    private var isControlBarAttached = false

    // HUD State
    private var isHudLocked: Boolean = true
    private var isGhostMode: Boolean = false
    private var activeAimSmoothMode: AimSmoothMode = AimSmoothMode.HEADSHOT_DRAG

    private val mainHandler = Handler(Looper.getMainLooper())
    private var vibrator: Vibrator? = null

    companion object {
        private const val TAG = "MacricOverlayService"
        const val ACTION_START_OVERLAY = "com.example.macric.ACTION_START_OVERLAY"
        const val ACTION_STOP_OVERLAY = "com.example.macric.ACTION_STOP_OVERLAY"
        const val ACTION_UPDATE_CONFIG = "com.example.macric.ACTION_UPDATE_CONFIG"
        const val ACTION_TOGGLE_LOCK = "com.example.macric.ACTION_TOGGLE_LOCK"

        private const val CHANNEL_ID = "macric_overlay_channel"
        private const val NOTIF_ID = 1001

        var isRunning: Boolean = false
            private set

        private var activeInstance: MacricOverlayService? = null

        /**
         * Triggered by key filtering when mouse is locked in BlueStacks / PC emulators,
         * or via hardware shortcuts. Injects the ultra-smooth headshot drag immediately.
         */
        fun triggerActiveFire() {
            activeInstance?.fireActiveWeapon()
        }

        fun stopHoldingFire() {
            activeInstance?.releaseActiveFire()
        }

        fun executeTestFireNow() {
            activeInstance?.executeTestFire()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        isHudLocked = ProfileRepository(this).isHudLocked()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_OVERLAY -> {
                stopForegroundOverlay()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE_CONFIG -> {
                updateOverlayContent()
            }
            ACTION_TOGGLE_LOCK -> {
                toggleHudLock()
            }
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIF_ID,
                        buildNotification(),
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(NOTIF_ID, buildNotification())
                }
                isRunning = true
                showFloatingOverlay()
            }
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "Orientation / configuration changed. Re-adjusting HUD overlays.")
        mainHandler.postDelayed({
            if (isRunning) {
                updateOverlayContent()
            }
        }, 300L)
    }

    private fun getRealScreenDimensions(): Pair<Int, Int> {
        val wm = windowManager ?: return Pair(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            Pair(bounds.width(), bounds.height())
        } else {
            val dm = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(dm)
            Pair(dm.widthPixels, dm.heightPixels)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MACRIC Crosshair & Controls Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows status of the active MACRIC HUD crosshair and fire buttons"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MacricOverlayService::class.java).apply {
            action = ACTION_STOP_OVERLAY
        }
        val stopPending = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleLockIntent = Intent(this, MacricOverlayService::class.java).apply {
            action = ACTION_TOGGLE_LOCK
        }
        val toggleLockPending = PendingIntent.getService(
            this,
            2,
            toggleLockIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val lockTitle = if (isHudLocked) "Unlock HUD" else "Lock Position"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MACRIC Gaming HUD Active")
            .setContentText(if (isHudLocked) "HUD LOCKED (Combat Mode) · Tap to Shoot" else "HUD UNLOCKED (Drag to Adjust)")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_lock_lock, lockTitle, toggleLockPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close Overlay", stopPending)
            .setOngoing(true)
            .build()
    }

    private fun showFloatingOverlay() {
        val repo = ProfileRepository(this)
        val connectedGameId = repo.getConnectedGameId()
        val profile = repo.getProfiles().find { it.gameId == connectedGameId }
            ?: repo.getProfiles().firstOrNull()

        // 1. Show Crosshair Overlay
        attachCrosshairOverlay(profile)

        // 2. Attach Fire Buttons Overlays
        attachFireButtonOverlays(profile)

        // 3. Attach Floating Mini HUD Control Bar (Lock, Aim mode, Acc status)
        attachFloatingControlBar()
    }

    private fun attachCrosshairOverlay(profile: GameProfile?) {
        if (isCrosshairAttached && crosshairView != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

        if (isGhostMode) {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = 0
            y = 0
        }

        val crosshair = profile?.crosshair ?: CrosshairConfig()
        val customCanvas = CrosshairCanvasView(this, crosshair)

        val container = FrameLayout(this).apply {
            addView(customCanvas)
        }

        container.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                if (isHudLocked) return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        try {
                            windowManager?.updateViewLayout(container, params)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        return true
                    }
                }
                return false
            }
        })

        crosshairView = container
        try {
            windowManager?.addView(container, params)
            isCrosshairAttached = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun attachFireButtonOverlays(profile: GameProfile?) {
        val wm = windowManager ?: return
        val currentProfile = profile ?: return
        val buttons = currentProfile.fireButtons

        val (screenWidth, screenHeight) = getRealScreenDimensions()
        val metrics = resources.displayMetrics

        for (btn in buttons) {
            val sizePx = (btn.size * metrics.density).toInt().coerceIn(52, 280)
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

            if (isGhostMode) {
                flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            }

            val params = WindowManager.LayoutParams(
                sizePx,
                sizePx,
                layoutType,
                flags,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = ((btn.xPercent / 100f) * screenWidth - sizePx / 2f).toInt().coerceIn(0, (screenWidth - sizePx).coerceAtLeast(0))
                y = ((btn.yPercent / 100f) * screenHeight - sizePx / 2f).toInt().coerceIn(0, (screenHeight - sizePx).coerceAtLeast(0))
            }

            lateinit var fireButtonView: FireButtonCanvasView

            fireButtonView = FireButtonCanvasView(
                context = this,
                config = btn,
                isLockedProvider = { isHudLocked },
                aimSmoothProvider = { activeAimSmoothMode },
                onDragMove = { v, dx, dy ->
                    if (!isHudLocked) {
                        params.x = (params.x + dx).coerceIn(0, (screenWidth - sizePx).coerceAtLeast(0))
                        params.y = (params.y + dy).coerceIn(0, (screenHeight - sizePx).coerceAtLeast(0))
                        fireButtonView.currentCenterX = params.x + sizePx / 2f
                        fireButtonView.currentCenterY = params.y + sizePx / 2f
                        try {
                            wm.updateViewLayout(v, params)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
                onDragFinished = {
                    val finalCenterX = params.x + sizePx / 2f
                    val finalCenterY = params.y + sizePx / 2f
                    val newXPercent = (finalCenterX / screenWidth * 100f).coerceIn(0f, 100f)
                    val newYPercent = (finalCenterY / screenHeight * 100f).coerceIn(0f, 100f)
                    val repo = ProfileRepository(this)
                    repo.updateFireButtonPosition(currentProfile.id, btn.id, newXPercent, newYPercent)
                },
                onFireAction = { isDown, view ->
                    handleFireAction(btn, params.x + sizePx / 2f, params.y + sizePx / 2f, isDown, view)
                }
            ).apply {
                currentCenterX = params.x + sizePx / 2f
                currentCenterY = params.y + sizePx / 2f
                currentLayoutParams = params
            }

            try {
                wm.addView(fireButtonView, params)
                fireButtonViews.add(fireButtonView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private var isHoldingFire = false
    private var holdFireRunnable: Runnable? = null

    /**
     * Executes fire action when button is tapped on screen OR triggered via key in emulator.
     */
    private fun handleFireAction(
        btn: FireButtonConfig,
        centerX: Float,
        centerY: Float,
        isDown: Boolean,
        targetView: FireButtonCanvasView? = null
    ) {
        if (!isHudLocked) {
            // In unlocked edit mode, pressing does not fire; it allows repositioning
            return
        }

        val isAccRunning = MacricAccessibilityService.isRunning

        if (isDown) {
            vibratePulse()
            if (!isAccRunning) {
                mainHandler.post {
                    Toast.makeText(
                        this,
                        "⚠️ Accessibility is OFF! Turn ON 'MACRIC' in Accessibility Settings or switch to Direct Pass.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else if (btn.mode == FireMode.HOLD) {
                // HOLD mode: Trigger initial stroke and repeat
                executeShotGesture(btn, centerX, centerY, targetView)
                isHoldingFire = true
                holdFireRunnable = object : Runnable {
                    override fun run() {
                        if (isHoldingFire) {
                            executeShotGesture(btn, centerX, centerY, targetView)
                            mainHandler.postDelayed(this, 110L)
                        }
                    }
                }
                mainHandler.postDelayed(holdFireRunnable!!, 110L)
            }
        } else {
            // ACTION_UP:
            if (isHoldingFire) {
                isHoldingFire = false
                holdFireRunnable?.let { mainHandler.removeCallbacks(it) }
            } else if (btn.mode == FireMode.TAP && isAccRunning) {
                // TAP mode: Execute on finger release!
                // Critical Android fix: The physical finger has cleared the screen,
                // so dispatchGesture executes with ZERO pointer collision!
                executeShotGesture(btn, centerX, centerY, targetView)
            }
        }
    }

    /**
     * Dispatches synthetic headshot drag directly into Free Fire / games.
     * Note: If targetView is provided, we use real on-screen display coordinates and
     * set temporary pass-through so the synthetic gesture sweeps clean!
     */
    private fun executeShotGesture(
        btn: FireButtonConfig,
        centerX: Float,
        centerY: Float,
        targetView: FireButtonCanvasView? = null
    ) {
        val mode = activeAimSmoothMode
        val dragDistancePx = (btn.dragDistance * resources.displayMetrics.density).coerceIn(30f, 300f)
        val durationMs = btn.dragDurationMs
        val jDirection = btn.jCurveDirection

        val loc = IntArray(2)
        targetView?.getLocationOnScreen(loc)
        val (screenWidth, screenHeight) = getRealScreenDimensions()

        val rawX = if (loc[0] > 0 || loc[1] > 0) {
            loc[0] + (targetView?.width ?: 0) / 2f
        } else {
            centerX
        }

        val rawY = if (loc[0] > 0 || loc[1] > 0) {
            loc[1] + (targetView?.height ?: 0) / 2f
        } else {
            centerY
        }

        val finalX = rawX.coerceIn(15f, (screenWidth - 15).toFloat())
        val finalY = rawY.coerceIn(15f, (screenHeight - 15).toFloat())

        // Temporary Pass-Through: make overlay non-touchable during gesture injection so it doesn't collide
        targetView?.let { v ->
            val lp = v.currentLayoutParams ?: (v.layoutParams as? WindowManager.LayoutParams)
            if (lp != null && !isGhostMode) {
                lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                try {
                    windowManager?.updateViewLayout(v, lp)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not set temporary pass-through on fire view", e)
                }
            }
        }

        MacricAccessibilityService.dispatchUltraSmoothHeadshotDrag(
            startX = finalX,
            startY = finalY,
            dragUpPx = dragDistancePx,
            durationMs = durationMs,
            aimMode = mode,
            jCurveDirection = jDirection,
            onFinished = { success ->
                // Restore touchability after gesture stroke has been swept
                targetView?.let { v ->
                    mainHandler.post {
                        val lp = v.currentLayoutParams ?: (v.layoutParams as? WindowManager.LayoutParams)
                        if (lp != null && !isGhostMode) {
                            lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                            try {
                                windowManager?.updateViewLayout(v, lp)
                            } catch (e: Exception) {
                                Log.w(TAG, "Could not restore touchable on fire view", e)
                            }
                        }
                    }
                }
            }
        )
    }

    /**
     * Triggers active primary fire weapon when mouse lock or emulator key is triggered
     */
    private fun fireActiveWeapon() {
        val target = fireButtonViews.find { it.config.id == "right_fire" }
            ?: fireButtonViews.firstOrNull() ?: return
        vibratePulse()
        if (!MacricAccessibilityService.isRunning) {
            mainHandler.post {
                Toast.makeText(
                    this,
                    "⚠️ MACRIC Accessibility is OFF! Turn it ON in Settings to enable mouse-lock firing.",
                    Toast.LENGTH_LONG
                ).show()
            }
            return
        }
        executeShotGesture(target.config, target.currentCenterX, target.currentCenterY, target)
    }

    private fun releaseActiveFire() {
        val target = fireButtonViews.find { it.config.id == "right_fire" }
            ?: fireButtonViews.firstOrNull() ?: return
        handleFireAction(
            target.config,
            target.currentCenterX,
            target.currentCenterY,
            isDown = false,
            targetView = target
        )
    }

    fun executeTestFire() {
        val target = fireButtonViews.find { it.config.id == "right_fire" }
            ?: fireButtonViews.firstOrNull()
        if (target == null) {
            Toast.makeText(this, "No fire button found to test!", Toast.LENGTH_SHORT).show()
            return
        }
        if (!MacricAccessibilityService.isRunning) {
            Toast.makeText(this, "⚠️ Accessibility is OFF! Turn ON MACRIC in Settings to test tap.", Toast.LENGTH_LONG).show()
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }
        vibratePulse()
        executeShotGesture(target.config, target.currentCenterX, target.currentCenterY, target)
        Toast.makeText(this, "🔥 TEST SHOT: Tap & ${activeAimSmoothMode.displayName} fired!", Toast.LENGTH_SHORT).show()
    }

    private fun vibratePulse() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(18)
            }
        } catch (e: Exception) {
            // Ignore if vibration not permitted
        }
    }

    /**
     * Floating Tactical HUD Control Bar:
     * Compact pill that allows users to:
     * - Toggle Lock / Unlock HUD (firmly anchors button so it NEVER slides when tapping)
     * - Toggle Aim Smooth Drag Mode (Pro J-Drag, S-Curve, Sniper Snap, Anti-Recoil, Direct Tap)
     * - Show Accessibility Status (1-click link to Settings if needed)
     * - Toggle Click-Through Ghost Mode
     * - Minimize / Expand
     */
    private fun attachFloatingControlBar() {
        if (isControlBarAttached && hudControlBarView != null) return

        val wm = windowManager ?: return
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (20 * resources.displayMetrics.density).toInt()
        }

        val density = resources.displayMetrics.density
        var isMinimized = false

        // Outer Root Layout
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E60D0A17"))
                setStroke((1.5f * density).toInt(), Color.parseColor("#9312FF"))
                cornerRadius = 18f * density
            }
        }

        // Minimized Brand Icon
        val brandIcon = TextView(this).apply {
            text = "⚡"
            textSize = 14f
            setPadding((4 * density).toInt(), 0, (6 * density).toInt(), 0)
        }
        rootLayout.addView(brandIcon)

        // Expandable Content Container
        val contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Lock / Unlock Button
        val lockButton = TextView(this).apply {
            text = if (isHudLocked) "🔒 LOCKED" else "🔓 MOVE HUD"
            textSize = 11f
            setTextColor(if (isHudLocked) Color.parseColor("#45B558") else Color.parseColor("#FFD600"))
            setTypeface(null, Typeface.BOLD)
            setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
            background = GradientDrawable().apply {
                setColor(if (isHudLocked) Color.parseColor("#2245B558") else Color.parseColor("#33FFD600"))
                cornerRadius = 12f * density
                setStroke((1 * density).toInt(), if (isHudLocked) Color.parseColor("#45B558") else Color.parseColor("#FFD600"))
            }
        }

        lockButton.setOnClickListener {
            toggleHudLock()
            lockButton.text = if (isHudLocked) "🔒 LOCKED" else "🔓 MOVE HUD"
            lockButton.setTextColor(if (isHudLocked) Color.parseColor("#45B558") else Color.parseColor("#FFD600"))
            (lockButton.background as? GradientDrawable)?.apply {
                setColor(if (isHudLocked) Color.parseColor("#2245B558") else Color.parseColor("#33FFD600"))
                setStroke((1 * density).toInt(), if (isHudLocked) Color.parseColor("#45B558") else Color.parseColor("#FFD600"))
            }
        }
        contentContainer.addView(lockButton)

        // Spacer
        contentContainer.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams((6 * density).toInt(), 1)
        })

        // Aim Smooth Mode Chip
        val aimModeChip = TextView(this).apply {
            text = getAimModeChipLabel(activeAimSmoothMode)
            textSize = 10f
            setTextColor(Color.parseColor("#00E5FF"))
            setTypeface(null, Typeface.BOLD)
            setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#2200E5FF"))
                cornerRadius = 12f * density
                setStroke((1 * density).toInt(), Color.parseColor("#00E5FF"))
            }
        }

        aimModeChip.setOnClickListener {
            activeAimSmoothMode = when (activeAimSmoothMode) {
                AimSmoothMode.HEADSHOT_DRAG -> AimSmoothMode.SMOOTH_S_CURVE
                AimSmoothMode.SMOOTH_S_CURVE -> AimSmoothMode.SNIPER_SNAP
                AimSmoothMode.SNIPER_SNAP -> AimSmoothMode.SMOOTH_RECOIL
                AimSmoothMode.SMOOTH_RECOIL -> AimSmoothMode.INSTANT_TAP
                AimSmoothMode.INSTANT_TAP -> AimSmoothMode.HEADSHOT_DRAG
            }
            aimModeChip.text = getAimModeChipLabel(activeAimSmoothMode)
            Toast.makeText(this, "Aim Physics: ${activeAimSmoothMode.displayName}", Toast.LENGTH_SHORT).show()
            updateFireButtonsVisual()
        }
        contentContainer.addView(aimModeChip)

        // Spacer
        contentContainer.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams((6 * density).toInt(), 1)
        })

        // Live Test Fire Tap Button
        val testShotBtn = TextView(this).apply {
            text = "🎯 TEST"
            textSize = 10f
            setTextColor(Color.parseColor("#FFD600"))
            setTypeface(null, Typeface.BOLD)
            setPadding((6 * density).toInt(), (4 * density).toInt(), (6 * density).toInt(), (4 * density).toInt())
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#22FFD600"))
                cornerRadius = 10f * density
                setStroke((1 * density).toInt(), Color.parseColor("#FFD600"))
            }
        }
        testShotBtn.setOnClickListener {
            executeTestFire()
        }
        contentContainer.addView(testShotBtn)

        // Spacer
        contentContainer.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams((6 * density).toInt(), 1)
        })

        // Accessibility Status Badge / One-Click Link
        val accStatusBadge = TextView(this).apply {
            val isAccRunning = MacricAccessibilityService.isRunning
            text = if (isAccRunning) "🟢 TOUCH ON" else "⚠️ ACCESSIBILITY"
            textSize = 10f
            setTextColor(if (isAccRunning) Color.parseColor("#45B558") else Color.parseColor("#FFA000"))
            setTypeface(null, Typeface.BOLD)
            setPadding((6 * density).toInt(), (4 * density).toInt(), (6 * density).toInt(), (4 * density).toInt())
            background = GradientDrawable().apply {
                setColor(if (isAccRunning) Color.parseColor("#1A45B558") else Color.parseColor("#33FFA000"))
                cornerRadius = 10f * density
            }
        }

        accStatusBadge.setOnClickListener {
            if (!MacricAccessibilityService.isRunning) {
                try {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                    Toast.makeText(this, "Find 'MACRIC' and turn it ON to enable in-game fire taps!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Open Android Settings > Accessibility to enable MACRIC", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "MACRIC Touch & Headshot Engine is Active!", Toast.LENGTH_SHORT).show()
            }
        }
        contentContainer.addView(accStatusBadge)

        // Spacer
        contentContainer.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams((6 * density).toInt(), 1)
        })

        // Ghost Mode Toggle (Pass-through for BlueStacks keymapper)
        val ghostToggle = TextView(this).apply {
            text = if (isGhostMode) "👻 PASS" else "🎮 ACTIVE"
            textSize = 10f
            setTextColor(if (isGhostMode) Color.parseColor("#E0E0E0") else Color.parseColor("#CE93D8"))
            setPadding((6 * density).toInt(), (4 * density).toInt(), (6 * density).toInt(), (4 * density).toInt())
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#229312FF"))
                cornerRadius = 10f * density
            }
        }

        ghostToggle.setOnClickListener {
            isGhostMode = !isGhostMode
            ghostToggle.text = if (isGhostMode) "👻 PASS" else "🎮 ACTIVE"
            toggleGhostMode(isGhostMode)
            val msg = if (isGhostMode) "Click-Through enabled (Mouse clicks pass to BlueStacks keymapper)" else "Active Fire mode (Clicks trigger MACRIC gestures)"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
        contentContainer.addView(ghostToggle)

        rootLayout.addView(contentContainer)

        // Spacer
        rootLayout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams((6 * density).toInt(), 1)
        })

        // Minimize / Expand Toggle Button
        val minButton = TextView(this).apply {
            text = "◀"
            textSize = 11f
            setTextColor(Color.WHITE)
            setPadding((6 * density).toInt(), (2 * density).toInt(), (6 * density).toInt(), (2 * density).toInt())
        }
        minButton.setOnClickListener {
            isMinimized = !isMinimized
            if (isMinimized) {
                contentContainer.visibility = View.GONE
                minButton.text = "▶"
            } else {
                contentContainer.visibility = View.VISIBLE
                minButton.text = "◀"
                accStatusBadge.text = if (MacricAccessibilityService.isRunning) "🟢 TOUCH ON" else "⚠️ ACCESSIBILITY"
            }
        }
        rootLayout.addView(minButton)

        // Close HUD Button
        val closeBtn = TextView(this).apply {
            text = "✕"
            textSize = 12f
            setTextColor(Color.parseColor("#FF5252"))
            setTypeface(null, Typeface.BOLD)
            setPadding((6 * density).toInt(), (2 * density).toInt(), (4 * density).toInt(), (2 * density).toInt())
        }
        closeBtn.setOnClickListener {
            stopForegroundOverlay()
            stopSelf()
        }
        rootLayout.addView(closeBtn)

        // Draggable Control Bar Header
        rootLayout.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var touchX = 0f
            private var touchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        touchX = event.rawX
                        touchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - touchX).toInt()
                        params.y = initialY + (event.rawY - touchY).toInt()
                        try {
                            wm.updateViewLayout(rootLayout, params)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        return true
                    }
                }
                return false
            }
        })

        hudControlBarView = rootLayout
        try {
            wm.addView(rootLayout, params)
            isControlBarAttached = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getAimModeChipLabel(mode: AimSmoothMode): String {
        return when (mode) {
            AimSmoothMode.HEADSHOT_DRAG -> "👑 Pro J-Drag"
            AimSmoothMode.SMOOTH_S_CURVE -> "🌊 S-Curve"
            AimSmoothMode.SNIPER_SNAP -> "🎯 Sniper Snap"
            AimSmoothMode.SMOOTH_RECOIL -> "🛡️ Anti-Recoil"
            AimSmoothMode.INSTANT_TAP -> "⚡ Direct Tap"
        }
    }

    private fun toggleHudLock() {
        isHudLocked = !isHudLocked
        ProfileRepository(this).setHudLocked(isHudLocked)
        updateFireButtonsVisual()
        val text = if (isHudLocked) {
            "HUD Position LOCKED! Fire buttons are anchored for combat (Zero slide on tap)."
        } else {
            "HUD Position UNLOCKED! Drag fire buttons and crosshair to adjust positions."
        }
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun toggleGhostMode(ghost: Boolean) {
        val wm = windowManager ?: return

        for (v in fireButtonViews) {
            val params = v.currentLayoutParams ?: (v.layoutParams as? WindowManager.LayoutParams) ?: continue
            if (ghost) {
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            } else {
                params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            }
            try {
                wm.updateViewLayout(v, params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateFireButtonsVisual() {
        for (v in fireButtonViews) {
            v.invalidate()
        }
    }

    private fun removeFireButtonOverlays() {
        for (v in fireButtonViews) {
            try {
                windowManager?.removeView(v)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        fireButtonViews.clear()
    }

    private fun removeControlBar() {
        if (isControlBarAttached && hudControlBarView != null) {
            try {
                windowManager?.removeView(hudControlBarView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            hudControlBarView = null
            isControlBarAttached = false
        }
    }

    private fun updateOverlayContent() {
        val repo = ProfileRepository(this)
        val connectedGameId = repo.getConnectedGameId()
        val profile = repo.getProfiles().find { it.gameId == connectedGameId }
            ?: repo.getProfiles().firstOrNull()

        // 1. Update crosshair
        if (isCrosshairAttached && crosshairView != null) {
            val crosshair = profile?.crosshair ?: CrosshairConfig()
            val container = crosshairView as? FrameLayout
            if (container != null) {
                container.removeAllViews()
                val customCanvas = CrosshairCanvasView(this, crosshair)
                container.addView(customCanvas)
            }
        }

        // 2. Refresh fire buttons
        removeFireButtonOverlays()
        attachFireButtonOverlays(profile)
    }

    private fun stopForegroundOverlay() {
        isRunning = false
        if (isCrosshairAttached && crosshairView != null) {
            try {
                windowManager?.removeView(crosshairView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            crosshairView = null
            isCrosshairAttached = false
        }
        removeFireButtonOverlays()
        removeControlBar()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        if (activeInstance == this) {
            activeInstance = null
        }
        stopForegroundOverlay()
        super.onDestroy()
    }

    // Custom view that renders the crosshair based on CrosshairConfig
    class CrosshairCanvasView(context: Context, private val config: CrosshairConfig) : View(context) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = try {
                Color.parseColor(config.colorHex)
            } catch (e: Exception) {
                Color.parseColor("#9312FF")
            }
            alpha = (config.opacity * 255).toInt().coerceIn(20, 255)
            strokeWidth = config.thickness * resources.displayMetrics.density
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = paint.color
            alpha = paint.alpha
            style = Paint.Style.FILL
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val d = (config.size * resources.displayMetrics.density * 2.2f).toInt().coerceAtLeast(64)
            setMeasuredDimension(d, d)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (!config.enabled) return

            val cx = width / 2f
            val cy = height / 2f
            val s = config.size * resources.displayMetrics.density * 0.5f
            val g = config.gap * resources.displayMetrics.density * 0.4f
            val t = config.thickness * resources.displayMetrics.density

            when (config.style) {
                CrosshairStyle.DOT -> {
                    canvas.drawCircle(cx, cy, (t * 1.5f).coerceAtLeast(4f), fillPaint)
                }
                CrosshairStyle.PLUS -> {
                    canvas.drawLine(cx - s, cy, cx + s, cy, paint)
                    canvas.drawLine(cx, cy - s, cx, cy + s, paint)
                }
                CrosshairStyle.CIRCLE -> {
                    canvas.drawCircle(cx, cy, s * 0.75f, paint)
                }
                CrosshairStyle.CROSS_GAP -> {
                    canvas.drawLine(cx - s, cy, cx - g, cy, paint)
                    canvas.drawLine(cx + g, cy, cx + s, cy, paint)
                    canvas.drawLine(cx, cy - s, cx, cy - g, paint)
                    canvas.drawLine(cx, cy + g, cx, cy + s, paint)
                }
                CrosshairStyle.CIRCLE_DOT -> {
                    canvas.drawCircle(cx, cy, s * 0.8f, paint)
                    canvas.drawCircle(cx, cy, (t * 1.3f).coerceAtLeast(3f), fillPaint)
                }
                CrosshairStyle.T_SHAPE -> {
                    canvas.drawLine(cx - s, cy, cx - g, cy, paint)
                    canvas.drawLine(cx + g, cy, cx + s, cy, paint)
                    canvas.drawLine(cx, cy + g, cx, cy + s, paint)
                }
                CrosshairStyle.CHEVRON -> {
                    canvas.drawLine(cx - s * 0.7f, cy + s * 0.4f, cx, cy - s * 0.4f, paint)
                    canvas.drawLine(cx, cy - s * 0.4f, cx + s * 0.7f, cy + s * 0.4f, paint)
                }
                CrosshairStyle.DIAMOND -> {
                    canvas.drawLine(cx, cy - s * 0.7f, cx + s * 0.7f, cy, paint)
                    canvas.drawLine(cx + s * 0.7f, cy, cx, cy + s * 0.7f, paint)
                    canvas.drawLine(cx, cy + s * 0.7f, cx - s * 0.7f, cy, paint)
                    canvas.drawLine(cx - s * 0.7f, cy, cx, cy - s * 0.7f, paint)
                }
            }
        }
    }

    // Custom view that renders a floating tactical gaming HUD Fire Button with Lock & Aim Smooth Support
    class FireButtonCanvasView(
        context: Context,
        val config: FireButtonConfig,
        private val isLockedProvider: () -> Boolean,
        private val aimSmoothProvider: () -> AimSmoothMode,
        private val onDragMove: (view: View, dx: Int, dy: Int) -> Unit,
        private val onDragFinished: () -> Unit,
        private val onFireAction: (isDown: Boolean, view: FireButtonCanvasView) -> Unit
    ) : View(context) {

        var currentCenterX: Float = 0f
        var currentCenterY: Float = 0f
        var currentLayoutParams: WindowManager.LayoutParams? = null

        private var initialTouchX = 0f
        private var initialTouchY = 0f
        private var isDragging = false
        private var isPressedVisual = false

        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        private val editHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f * resources.displayMetrics.density
            pathEffect = DashPathEffect(floatArrayOf(12f, 12f), 0f)
            color = Color.parseColor("#FFD600")
        }

        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0E0E0")
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT
        }

        private val hexPath = Path()

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val isLocked = isLockedProvider()

            if (isLocked) {
                // Combat / Play mode: FIRMLY LOCKED! Absolute zero dragging!
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isPressedVisual = true
                        invalidate()
                        onFireAction(true, this)
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isPressedVisual = false
                        invalidate()
                        onFireAction(false, this)
                        return true
                    }
                }
                return true
            }

            // Unlocked mode: Draggable to adjust position on screen
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    isPressedVisual = true
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
                        isDragging = true
                    }
                    if (isDragging) {
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        onDragMove(this, dx, dy)
                    }
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isPressedVisual = false
                    invalidate()
                    if (isDragging) {
                        onDragFinished()
                    }
                    return true
                }
            }
            return super.onTouchEvent(event)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val isLocked = isLockedProvider()
            val aimMode = aimSmoothProvider()

            val baseColor = try {
                Color.parseColor(config.colorHex)
            } catch (e: Exception) {
                Color.parseColor("#9312FF")
            }

            val density = resources.displayMetrics.density
            val baseAlpha = (config.opacity * 255).toInt().coerceIn(30, 255)
            val fillAlpha = if (isPressedVisual) (baseAlpha * 0.85f).toInt().coerceIn(70, 255) else (baseAlpha * 0.35f).toInt()
            val strokeAlpha = if (isPressedVisual) 255 else baseAlpha

            fillPaint.color = baseColor
            fillPaint.alpha = fillAlpha

            strokePaint.color = if (isPressedVisual) Color.parseColor("#00E5FF") else baseColor
            strokePaint.alpha = strokeAlpha
            strokePaint.strokeWidth = (if (isPressedVisual) 4.5f else 3f) * density

            glowPaint.color = if (isPressedVisual) Color.parseColor("#00E5FF") else baseColor
            glowPaint.alpha = (strokeAlpha * 0.4f).toInt()
            glowPaint.strokeWidth = (if (isPressedVisual) 8f else 5f) * density

            val cx = width / 2f
            val cy = height / 2f
            val padding = 6f * density
            val radius = (Math.min(width, height) / 2f) - padding

            when (config.shape) {
                FireButtonShape.CIRCLE -> {
                    canvas.drawCircle(cx, cy, radius, glowPaint)
                    canvas.drawCircle(cx, cy, radius, fillPaint)
                    canvas.drawCircle(cx, cy, radius, strokePaint)
                    canvas.drawCircle(cx, cy, radius * 0.76f, glowPaint)
                }
                FireButtonShape.ROUNDED_SQUARE -> {
                    val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
                    val corner = radius * 0.28f
                    canvas.drawRoundRect(rect, corner, corner, glowPaint)
                    canvas.drawRoundRect(rect, corner, corner, fillPaint)
                    canvas.drawRoundRect(rect, corner, corner, strokePaint)
                }
                FireButtonShape.HEXAGON -> {
                    hexPath.reset()
                    for (i in 0 until 6) {
                        val angleDeg = 60.0 * i - 30.0
                        val angleRad = Math.toRadians(angleDeg)
                        val x = cx + radius * Math.cos(angleRad).toFloat()
                        val y = cy + radius * Math.sin(angleRad).toFloat()
                        if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
                    }
                    hexPath.close()
                    canvas.drawPath(hexPath, glowPaint)
                    canvas.drawPath(hexPath, fillPaint)
                    canvas.drawPath(hexPath, strokePaint)
                }
            }

            // Tactical tick markings on rim
            val markLen = radius * 0.2f
            canvas.drawLine(cx - radius, cy, cx - radius + markLen, cy, strokePaint)
            canvas.drawLine(cx + radius - markLen, cy, cx + radius, cy, strokePaint)
            canvas.drawLine(cx, cy - radius, cx, cy - radius + markLen, strokePaint)
            canvas.drawLine(cx, cy + radius - markLen, cx, cy + radius, strokePaint)

            // If unlocked, draw yellow dashed ring to indicate movable adjustment state
            if (!isLocked) {
                canvas.drawCircle(cx, cy, radius + 3.5f * density, editHaloPaint)
            }

            // Draw label & mode text
            val labelSize = (radius * 0.36f).coerceIn(16f, 40f)
            textPaint.textSize = labelSize
            val subTextSize = (radius * 0.22f).coerceIn(11f, 22f)
            subTextPaint.textSize = subTextSize

            val labelText = if (!isLocked) "DRAG" else config.label
            val subText = if (!isLocked) "Adjust" else when (aimMode) {
                AimSmoothMode.HEADSHOT_DRAG -> "👑 J-DRAG"
                AimSmoothMode.SMOOTH_S_CURVE -> "🌊 S-CURVE"
                AimSmoothMode.SNIPER_SNAP -> "🎯 SNIPER"
                AimSmoothMode.SMOOTH_RECOIL -> "🛡️ RECOIL"
                AimSmoothMode.INSTANT_TAP -> "⚡ TAP"
            }

            canvas.drawText(labelText, cx, cy - (labelSize * 0.08f), textPaint)
            canvas.drawText(subText, cx, cy + (subTextSize * 1.35f), subTextPaint)
        }
    }
}
