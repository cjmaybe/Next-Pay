package com.example.platform

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

/**
 * TYPE_APPLICATION_OVERLAY window painted ABOVE the carrier USSD dialog so
 * the user only ever sees Next Pay branded UI while a session runs.
 *
 * Critical flag on both windows: `FLAG_NOT_FOCUSABLE`. If the overlay grabs
 * keyboard focus, the underlying carrier dialog can't reliably receive
 * AccessibilityService setText/click actions.
 */
class OverlayControllerImpl(private val context: Context) : OverlayController {

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())

    // ── Full overlay state ─────────────────────────────────────────────────────
    private var overlayView: FrameLayout? = null
    private var titleView: TextView? = null
    private var subtitleView: TextView? = null
    private var stepLabelView: TextView? = null
    private var spinnerView: ProgressBar? = null

    // ── Minimal floating bar state ─────────────────────────────────────────────
    private var minimalView: LinearLayout? = null
    private var minimalLabel: TextView? = null
    private var minimalStepCount: TextView? = null
    private var minimalProgressFill: View? = null
    private var minimalProgressTrack: FrameLayout? = null

    override var onCancel: (() -> Unit)? = null
    override var onMinimalTapped: (() -> Unit)? = null

    private fun fullParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }

    private fun minimalParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(48)
        }
    }

    override fun canShow(): Boolean = Settings.canDrawOverlays(context)

    // ─── Full overlay ──────────────────────────────────────────────────────────

    override fun show(title: String, subtitle: String, stepLabel: String) {
        if (!canShow()) return
        removeMinimalView()
        handler.post {
            if (overlayView != null) {
                update(title, subtitle, stepLabel)
                return@post
            }
            try {
                val v = buildFullView(title, subtitle, stepLabel)
                windowManager.addView(v, fullParams())
                overlayView = v
            } catch (_: Exception) {
                overlayView = null
            }
        }
    }

    override fun update(title: String, subtitle: String, stepLabel: String) {
        handler.post {
            spinnerView?.visibility = View.VISIBLE
            titleView?.setTextColor(COLOR_WHITE)
            titleView?.text = title
            subtitleView?.text = subtitle
            stepLabelView?.text = stepLabel.uppercase()
        }
    }

    // ─── Minimal bar ───────────────────────────────────────────────────────────

    override fun showMinimal(progress: Int, total: Int, label: String) {
        if (!canShow()) return
        removeFullView()
        handler.post {
            if (minimalView != null) {
                updateMinimal(progress, total, label)
                return@post
            }
            try {
                val v = buildMinimalView(progress, total, label)
                windowManager.addView(v, minimalParams())
                minimalView = v
            } catch (_: Exception) {
                minimalView = null
            }
        }
    }

    override fun updateMinimal(progress: Int, total: Int, label: String) {
        handler.post {
            minimalLabel?.text = label.uppercase() + "…"
            minimalLabel?.setTextColor(COLOR_WHITE)
            val safeTotal = total.coerceAtLeast(1)
            val displayedStep = (progress + 1).coerceIn(1, safeTotal)
            minimalStepCount?.text = "STEP $displayedStep / $safeTotal"

            minimalProgressTrack?.let { track ->
                track.post {
                    val fraction = (progress.toFloat() / safeTotal).coerceIn(0f, 1f)
                    val newWidth = (track.width * fraction).toInt()
                    minimalProgressFill?.layoutParams =
                        FrameLayout.LayoutParams(
                            newWidth,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                }
            }
        }
    }

    override fun showError(title: String, message: String, holdMs: Long) {
        handler.post {
            if (minimalView != null) {
                minimalLabel?.text = title.uppercase()
                minimalLabel?.setTextColor(COLOR_DANGER)
                minimalStepCount?.text = message.take(40)
                handler.postDelayed({ hide() }, holdMs)
                return@post
            }
            if (overlayView == null) {
                show(title, message, "")
            }
            spinnerView?.visibility = View.GONE
            titleView?.setTextColor(COLOR_DANGER)
            titleView?.text = title
            subtitleView?.text = message
            stepLabelView?.text = "TAP CANCEL TO DISMISS"
            handler.postDelayed({ hide() }, holdMs)
        }
    }

    override fun hide() {
        handler.post {
            removeFullView()
            removeMinimalView()
        }
    }

    private fun removeFullView() {
        overlayView?.let { view ->
            try {
                if (view.parent != null) windowManager.removeView(view)
            } catch (_: Exception) {
            }
        }
        overlayView = null
        titleView = null
        subtitleView = null
        stepLabelView = null
        spinnerView = null
    }

    private fun removeMinimalView() {
        minimalView?.let { view ->
            try {
                if (view.parent != null) windowManager.removeView(view)
            } catch (_: Exception) {
            }
        }
        minimalView = null
        minimalLabel = null
        minimalStepCount = null
        minimalProgressFill = null
        minimalProgressTrack = null
    }

    // ─── Full view construction ────────────────────────────────────────────────

    private fun buildFullView(
        title: String,
        subtitle: String,
        stepLabel: String
    ): FrameLayout {
        val dim = FrameLayout(context).apply {
            setBackgroundColor(COLOR_DIM)
            isClickable = true
        }

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = nextPayCardBg()
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }

        val brand = TextView(context).apply {
            text = "NEXT PAY // OFFLINE SECURE"
            setTextColor(COLOR_ACCENT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            letterSpacing = 0.15f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        spinnerView = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            indeterminateTintList = android.content.res.ColorStateList.valueOf(COLOR_ACCENT)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(20)
            layoutParams = lp
        }

        titleView = TextView(context).apply {
            text = title
            setTextColor(COLOR_WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(16)
            layoutParams = lp
        }

        subtitleView = TextView(context).apply {
            text = subtitle
            setTextColor(COLOR_TEXT_SECONDARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(6)
            layoutParams = lp
        }

        stepLabelView = TextView(context).apply {
            text = stepLabel.uppercase()
            setTextColor(COLOR_ACCENT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            letterSpacing = 0.18f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(14)
            layoutParams = lp
        }

        val cancel = Button(context).apply {
            text = "CANCEL"
            setTextColor(COLOR_DANGER)
            background = nextPayButtonBg(COLOR_DANGER)
            setPadding(dp(20), dp(10), dp(20), dp(10))
            isAllCaps = true
            letterSpacing = 0.1f
            setOnClickListener {
                onCancel?.invoke()
                hide()
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = Gravity.END
            lp.topMargin = dp(20)
            layoutParams = lp
        }

        card.addView(brand)
        card.addView(spinnerView)
        card.addView(titleView)
        card.addView(subtitleView)
        card.addView(stepLabelView)
        card.addView(cancel)

        val cardLp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            topMargin = dp(80)
            leftMargin = dp(20)
            rightMargin = dp(20)
        }
        dim.addView(card, cardLp)

        return dim
    }

    // ─── Minimal floating bar view construction ────────────────────────────────

    private fun buildMinimalView(progress: Int, total: Int, label: String): LinearLayout {
        val safeTotal = total.coerceAtLeast(1)
        val displayedStep = (progress + 1).coerceIn(1, safeTotal)

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = minimalBarBg()
            setPadding(dp(16), dp(12), dp(16), dp(0))
            val sideMargin = dp(20)
            setPadding(dp(16) + sideMargin, dp(12), dp(16) + sideMargin, dp(0))
            isClickable = true
            setOnClickListener {
                onMinimalTapped?.invoke()
            }
        }

        val topRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams = lp
        }

        val spinner = ProgressBar(context).apply {
            indeterminateTintList = android.content.res.ColorStateList.valueOf(COLOR_ACCENT)
            val lp = LinearLayout.LayoutParams(dp(18), dp(18))
            lp.rightMargin = dp(10)
            layoutParams = lp
        }

        minimalLabel = TextView(context).apply {
            text = label.uppercase() + "…"
            setTextColor(COLOR_WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            letterSpacing = 0.12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val lp = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            layoutParams = lp
        }

        minimalStepCount = TextView(context).apply {
            text = "STEP $displayedStep / $safeTotal"
            setTextColor(COLOR_TEXT_SECONDARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            letterSpacing = 0.1f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        topRow.addView(spinner)
        topRow.addView(minimalLabel)
        topRow.addView(minimalStepCount)

        minimalProgressTrack = FrameLayout(context).apply {
            setBackgroundColor(COLOR_BORDER)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(2)
            )
            lp.topMargin = dp(10)
            lp.bottomMargin = dp(10)
            layoutParams = lp
        }

        minimalProgressFill = View(context).apply {
            setBackgroundColor(COLOR_ACCENT)
            val lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            lp.width = 0
            layoutParams = lp
        }

        minimalProgressTrack?.addView(minimalProgressFill)
        container.addView(topRow)
        container.addView(minimalProgressTrack)

        return container
    }

    private fun minimalBarBg(): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            setColor(COLOR_MINIMAL_FILL)
            setStroke(dp(1), COLOR_ACCENT)
        }
    }

    private fun nextPayCardBg(): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            setColor(COLOR_SURFACE_HIGH)
            setStroke(dp(1), COLOR_BORDER)
        }
    }

    private fun nextPayButtonBg(stroke: Int): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dp(8).toFloat()
            setColor(Color.TRANSPARENT)
            setStroke(dp(1), stroke)
        }
    }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    companion object {
        private const val COLOR_DIM = 0xCC000000.toInt()
        private const val COLOR_SURFACE_HIGH = 0xFF1A1A1A.toInt()
        private const val COLOR_BORDER = 0xFF3A3A3A.toInt()
        private const val COLOR_WHITE = 0xFFFFFFFF.toInt()
        private const val COLOR_TEXT_SECONDARY = 0xFFA0A0A0.toInt()
        private const val COLOR_ACCENT = 0xFFFFFFFF.toInt() // Monochrome accent
        private const val COLOR_DANGER = 0xFFD0D0D0.toInt()
        private const val COLOR_MINIMAL_FILL = 0xEB000000.toInt()
    }
}
