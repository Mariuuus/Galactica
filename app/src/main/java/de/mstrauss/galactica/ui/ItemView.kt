package de.mstrauss.galactica.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Choreographer
import android.view.View

abstract class ItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var durationSeconds: Int = 10

    // ----- timer state (accessible to subclasses) -----

    protected var timeLeftMs: Long = 0L
    protected var isRunning: Boolean = false
    private var lastFrameNanos = 0L

    // ----- frame loop -----

    private val choreographer = Choreographer.getInstance()

    private val frameCallback: Choreographer.FrameCallback = Choreographer.FrameCallback { nanos ->
        if (isRunning) {
            val dt = if (lastFrameNanos == 0L) 0f
                     else (nanos - lastFrameNanos) / 1_000_000_000f
            lastFrameNanos = nanos
            onFrame(dt)
            tickTimer(dt)
            invalidate()
            choreographer.postFrameCallback(frameCallback)
        }
    }

    open fun start() {
        if (isRunning) return
        timeLeftMs = durationSeconds * 1000L
        lastFrameNanos = 0L
        isRunning = true
        visibility = VISIBLE
        choreographer.postFrameCallback(frameCallback)
    }

    open fun stop() {
        isRunning = false
        choreographer.removeFrameCallback(frameCallback)
        visibility = INVISIBLE
    }

    private fun tickTimer(dt: Float) {
        if (dt <= 0f) return
        timeLeftMs = (timeLeftMs - (dt * 1000).toLong()).coerceAtLeast(0)
        if (timeLeftMs == 0L) {
            isRunning = false
            choreographer.removeFrameCallback(frameCallback)
            post {
                onTimerFinished()
                visibility = INVISIBLE
            }
        }
    }

    /** Called every frame. Subclasses implement physics/animation here. */
    protected abstract fun onFrame(dt: Float)

    /** Called (on main thread) when the countdown reaches zero. */
    protected open fun onTimerFinished() {}

    // ----- timer drawing -----

    private val timerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f * resources.displayMetrics.density
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val timerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(160, 0x0B, 0x13, 0x2B)
    }

    private val timerBgRect = RectF()
    private val cornerRadius = 8f * resources.displayMetrics.density

    protected fun drawTimer(canvas: Canvas) {
        val timerText = "%.1f".format(timeLeftMs / 1000f)
        val pad = 12f * resources.displayMetrics.density
        val textWidth = timerPaint.measureText(timerText)
        val textHeight = timerPaint.textSize
        val cx = width / 2f

        timerBgRect.set(
            cx - textWidth / 2f - pad,
            pad / 2f,
            cx + textWidth / 2f + pad,
            pad / 2f + textHeight + pad
        )
        canvas.drawRoundRect(timerBgRect, cornerRadius, cornerRadius, timerBgPaint)
        canvas.drawText(timerText, cx, pad + textHeight, timerPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }
}
