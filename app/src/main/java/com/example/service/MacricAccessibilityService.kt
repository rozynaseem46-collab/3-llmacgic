package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class MacricAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "MacricAccessibility"
        private var activeInstance: MacricAccessibilityService? = null

        val isRunning: Boolean
            get() = activeInstance != null

        fun getInstance(): MacricAccessibilityService? = activeInstance

        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            return isRunning
        }

        /**
         * Dispatches a synthetic instant tap gesture at (x, y) on the screen.
         */
        fun dispatchTap(x: Float, y: Float, durationMs: Long = 45L): Boolean {
            val service = activeInstance ?: run {
                Log.w(TAG, "Cannot dispatch tap: AccessibilityService is not enabled or running.")
                return false
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false

            val path = Path().apply {
                moveTo(x, y)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs.coerceIn(20L, 250L))
            val gesture = GestureDescription.Builder().addStroke(stroke).build()

            return try {
                service.dispatchGesture(gesture, object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        super.onCompleted(gestureDescription)
                        Log.d(TAG, "Tap gesture completed successfully at ($x, $y)")
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        super.onCancelled(gestureDescription)
                        Log.d(TAG, "Tap gesture was cancelled at ($x, $y)")
                    }
                }, Handler(Looper.getMainLooper()))
            } catch (e: Exception) {
                Log.e(TAG, "Error dispatching tap gesture", e)
                false
            }
        }

        /**
         * Dispatches a synthetic smooth drag / recoil upward swipe gesture starting at (startX, startY)
         * and moving up by [dragUpPx] pixels over [durationMs].
         * This produces the real 'Drag Headshot / Aim Smoothing' effect in games like Free Fire & shooters.
         */
        fun dispatchSmoothDrag(
            startX: Float,
            startY: Float,
            dragUpPx: Float,
            durationMs: Long = 180L
        ): Boolean {
            val service = activeInstance ?: run {
                Log.w(TAG, "Cannot dispatch drag: AccessibilityService is not enabled or running.")
                return false
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false

            val endY = (startY - dragUpPx).coerceAtLeast(10f)
            val path = Path().apply {
                moveTo(startX, startY)
                // Gentle curve for natural hand aim smoothing simulation
                quadTo(startX + 2f, (startY + endY) / 2f, startX, endY)
            }

            val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs.coerceIn(80L, 600L))
            val gesture = GestureDescription.Builder().addStroke(stroke).build()

            return try {
                service.dispatchGesture(gesture, object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        super.onCompleted(gestureDescription)
                        Log.d(TAG, "Smooth aim drag completed from ($startX, $startY) to ($startX, $endY)")
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        super.onCancelled(gestureDescription)
                        Log.d(TAG, "Smooth aim drag was cancelled")
                    }
                }, Handler(Looper.getMainLooper()))
            } catch (e: Exception) {
                Log.e(TAG, "Error dispatching smooth drag gesture", e)
                false
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeInstance = this
        Log.i(TAG, "MacricAccessibilityService connected and ready for touch/drag gestures.")
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
