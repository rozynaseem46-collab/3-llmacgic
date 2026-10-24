package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.ProfileRepository
import com.example.model.CrosshairConfig
import com.example.model.CrosshairStyle
import com.example.model.FireButtonConfig
import com.example.model.FireButtonShape
import com.example.model.GameProfile

class MacricOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isOverlayAttached = false
    private val fireButtonViews: MutableList<View> = mutableListOf()

    companion object {
        const val ACTION_START_OVERLAY = "com.example.macric.ACTION_START_OVERLAY"
        const val ACTION_STOP_OVERLAY = "com.example.macric.ACTION_STOP_OVERLAY"
        const val ACTION_UPDATE_CONFIG = "com.example.macric.ACTION_UPDATE_CONFIG"
        private const val CHANNEL_ID = "macric_overlay_channel"
        private const val NOTIF_ID = 1001

        var isRunning: Boolean = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
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

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MACRIC Gaming HUD Active")
            .setContentText("Crosshair & Fire Buttons running. Tap to customize.")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close Overlay", stopPending)
            .setOngoing(true)
            .build()
    }

    private fun showFloatingOverlay() {
        val repo = ProfileRepository(this)
        val connectedGameId = repo.getConnectedGameId()
        val profile = repo.getProfiles().find { it.gameId == connectedGameId }
            ?: repo.getProfiles().firstOrNull()

        // 1. Show Crosshair Overlay (if not already attached)
        if (!isOverlayAttached) {
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
                gravity = Gravity.CENTER
                x = 0
                y = 0
            }

            val crosshair = profile?.crosshair ?: CrosshairConfig()
            val customCanvas = CrosshairCanvasView(this, crosshair)

            val container = FrameLayout(this).apply {
                addView(customCanvas)
            }

            // Draggable touch listener for crosshair
            container.setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f

                override fun onTouch(v: View, event: MotionEvent): Boolean {
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

            overlayView = container
            try {
                windowManager?.addView(container, params)
                isOverlayAttached = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Attach Fire Buttons Overlays
        attachFireButtonOverlays(profile)
    }

    private fun attachFireButtonOverlays(profile: GameProfile?) {
        val wm = windowManager ?: return
        val currentProfile = profile ?: return
        val buttons = currentProfile.fireButtons

        val metrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        for (btn in buttons) {
            val sizePx = (btn.size * metrics.density).toInt().coerceIn(52, 280)
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                sizePx,
                sizePx,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = ((btn.xPercent / 100f) * screenWidth - sizePx / 2f).toInt().coerceIn(0, (screenWidth - sizePx).coerceAtLeast(0))
                y = ((btn.yPercent / 100f) * screenHeight - sizePx / 2f).toInt().coerceIn(0, (screenHeight - sizePx).coerceAtLeast(0))
            }

            val fireButtonView = FireButtonCanvasView(
                context = this,
                config = btn,
                onDragMove = { v, dx, dy ->
                    params.x = (params.x + dx).coerceIn(0, (screenWidth - sizePx).coerceAtLeast(0))
                    params.y = (params.y + dy).coerceIn(0, (screenHeight - sizePx).coerceAtLeast(0))
                    try {
                        wm.updateViewLayout(v, params)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                onDragFinished = {
                    val finalCenterX = params.x + sizePx / 2f
                    val finalCenterY = params.y + sizePx / 2f
                    val newXPercent = (finalCenterX / screenWidth * 100f).coerceIn(0f, 100f)
                    val newYPercent = (finalCenterY / screenHeight * 100f).coerceIn(0f, 100f)
                    val repo = ProfileRepository(this)
                    repo.updateFireButtonPosition(currentProfile.id, btn.id, newXPercent, newYPercent)
                }
            )

            try {
                wm.addView(fireButtonView, params)
                fireButtonViews.add(fireButtonView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

    private fun updateOverlayContent() {
        val repo = ProfileRepository(this)
        val connectedGameId = repo.getConnectedGameId()
        val profile = repo.getProfiles().find { it.gameId == connectedGameId }
            ?: repo.getProfiles().firstOrNull()

        // 1. Update crosshair
        if (isOverlayAttached && overlayView != null) {
            val crosshair = profile?.crosshair ?: CrosshairConfig()
            val container = overlayView as? FrameLayout
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
        if (isOverlayAttached && overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
            isOverlayAttached = false
        }
        removeFireButtonOverlays()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
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

    // Custom view that renders a floating tactical gaming HUD Fire Button
    class FireButtonCanvasView(
        context: Context,
        val config: FireButtonConfig,
        private val onDragMove: (view: View, dx: Int, dy: Int) -> Unit,
        private val onDragFinished: () -> Unit
    ) : View(context) {

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
                    if (Math.abs(dx) > 6 || Math.abs(dy) > 6) {
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

            val baseColor = try {
                Color.parseColor(config.colorHex)
            } catch (e: Exception) {
                Color.parseColor("#9312FF")
            }

            val density = resources.displayMetrics.density
            val baseAlpha = (config.opacity * 255).toInt().coerceIn(30, 255)
            val fillAlpha = if (isPressedVisual) (baseAlpha * 0.75f).toInt().coerceIn(50, 255) else (baseAlpha * 0.35f).toInt()
            val strokeAlpha = if (isPressedVisual) 255 else baseAlpha

            fillPaint.color = baseColor
            fillPaint.alpha = fillAlpha

            strokePaint.color = baseColor
            strokePaint.alpha = strokeAlpha
            strokePaint.strokeWidth = 3f * density

            glowPaint.color = baseColor
            glowPaint.alpha = (strokeAlpha * 0.35f).toInt()
            glowPaint.strokeWidth = 6f * density

            val cx = width / 2f
            val cy = height / 2f
            val padding = 6f * density
            val radius = (Math.min(width, height) / 2f) - padding

            when (config.shape) {
                FireButtonShape.CIRCLE -> {
                    canvas.drawCircle(cx, cy, radius, glowPaint)
                    canvas.drawCircle(cx, cy, radius, fillPaint)
                    canvas.drawCircle(cx, cy, radius, strokePaint)
                    canvas.drawCircle(cx, cy, radius * 0.78f, glowPaint)
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

            // Draw label & keybinding text
            val labelSize = (radius * 0.36f).coerceIn(16f, 40f)
            textPaint.textSize = labelSize
            val subTextSize = (radius * 0.22f).coerceIn(11f, 22f)
            subTextPaint.textSize = subTextSize

            val hasBinding = config.keyBinding.isNotBlank()
            val labelY = if (hasBinding) cy - (labelSize * 0.1f) else cy + (labelSize * 0.35f)
            canvas.drawText(config.label, cx, labelY, textPaint)

            if (hasBinding) {
                val keyText = if (config.keyBinding.startsWith("[")) config.keyBinding else "[${config.keyBinding}]"
                canvas.drawText(keyText, cx, cy + (subTextSize * 1.35f), subTextPaint)
            }
        }
    }
}
