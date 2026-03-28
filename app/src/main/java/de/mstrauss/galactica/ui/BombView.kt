package de.mstrauss.galactica.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.AttributeSet
import android.view.Choreographer
import android.view.Surface
import android.view.View
import android.view.WindowManager
import androidx.core.graphics.drawable.toBitmap
import de.mstrauss.galactica.R
import kotlin.math.absoluteValue
import kotlin.math.pow

class BombView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), SensorEventListener {

    var durationSeconds: Int = 10

    /**
     * Called when the countdown reaches zero.
     * @param xFraction  0..1 relative to view width (left=0, right=1)
     * @param yFraction  0..1 relative to view height (top=0, bottom=1)
     * @param xPixel     absolute pixel x inside the view
     * @param yPixel     absolute pixel y inside the view
     */
    var onTimerExpired: ((xFraction: Float, yFraction: Float, xPixel: Float, yPixel: Float) -> Unit)? = null

    fun start() {
        if (isRunning) return
        ballX = width / 2f
        ballY = height / 2f
        velX = 0f
        velY = 0f
        timeLeftMs = durationSeconds * 1000L
        lastFrameNanos = 0L
        isRunning = true
        visibility = VISIBLE

        // Capture rotation once so onSensorChanged can remap axes correctly
        @Suppress("DEPRECATION")
        displayRotation = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            .defaultDisplay.rotation

        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { accel ->
            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME)
        }
        choreographer.postFrameCallback(frameCallback)
    }

    fun stop() {
        isRunning = false
        sensorManager.unregisterListener(this)
        choreographer.removeFrameCallback(frameCallback)
        visibility = INVISIBLE
    }

    // ----- internals -----

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val choreographer = Choreographer.getInstance()

    private val ballSizePx = (40 * resources.displayMetrics.density)
    private val ballBitmap: Bitmap

    private var ballX = 0f
    private var ballY = 0f
    private var velX = 0f
    private var velY = 0f

    @Volatile private var accelX = 0f
    @Volatile private var accelY = 0f

    private var displayRotation = Surface.ROTATION_0
    private var timeLeftMs = 0L
    private var lastFrameNanos = 0L
    private var isRunning = false

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

    private val frameCallback: Choreographer.FrameCallback = Choreographer.FrameCallback { frameTimeNanos ->
        if (isRunning) {
            val dt = if (lastFrameNanos == 0L) 0f
                     else (frameTimeNanos - lastFrameNanos) / 1_000_000_000f
            lastFrameNanos = frameTimeNanos
            updateBall(dt)
            invalidate()
            choreographer.postFrameCallback(frameCallback)
        }
    }

    init {
        val drawable = androidx.appcompat.content.res.AppCompatResources.getDrawable(
            context, R.drawable._icon_item_bomb
        )!!
        ballBitmap = drawable.toBitmap(ballSizePx.toInt(), ballSizePx.toInt())

        context.theme.obtainStyledAttributes(attrs, R.styleable.BombView, 0, 0).apply {
            try {
                durationSeconds = getInteger(R.styleable.BombView_durationSeconds, 10)
            } finally {
                recycle()
            }
        }

        visibility = INVISIBLE
    }

    // ----- sensor -----

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        // Manually remap hardware sensor axes to screen axes.
        // Accelerometer is always in the fixed hardware frame:
        //   values[0] (hw-X+) = right side of device in portrait
        //   values[1] (hw-Y+) = top of device in portrait
        // After remapping, accelX/Y align with screen X/Y so the ball rolls
        // toward whichever side is tilted down, and stays still when flat.
        when (displayRotation) {
            // Standard landscape (CCW 90°): screen-right = hw-Y+, screen-down = hw-X-
            Surface.ROTATION_90 -> {
                accelX =  event.values[1]
                accelY =  event.values[0]
            }
            // Reverse landscape (CW 90°): screen-right = hw-Y-, screen-down = hw-X+
            Surface.ROTATION_270 -> {
                accelX = -event.values[1]
                accelY = -event.values[0]
            }
            else -> {
                accelX = event.values[0]
                accelY = event.values[1]
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    // ----- physics -----

    private fun updateBall(dt: Float) {
        if (dt <= 0f || dt > 0.1f) return

        // Frame-rate independent damping: retain 55% velocity per second
        val damping = 0.55f.pow(dt)
        val sensitivity = 300f

        // After remapping, positive accelX = screen tilted right (ball rolls right)
        //                   positive accelY = screen tilted down  (ball rolls down)
        velX = (velX + accelX * sensitivity * dt) * damping
        velY = (velY + accelY * sensitivity * dt) * damping

        val ballRadius = ballSizePx / 2f
        val newX = ballX + velX * dt
        val newY = ballY + velY * dt

        // Clamp and bounce off edges
        if (newX < ballRadius) {
            ballX = ballRadius
            velX = velX.absoluteValue * 0.4f
        } else if (newX > width - ballRadius) {
            ballX = width - ballRadius
            velX = -velX.absoluteValue * 0.4f
        } else {
            ballX = newX
        }

        if (newY < ballRadius) {
            ballY = ballRadius
            velY = velY.absoluteValue * 0.4f
        } else if (newY > height - ballRadius) {
            ballY = height - ballRadius
            velY = -velY.absoluteValue * 0.4f
        } else {
            ballY = newY
        }

        // Tick timer
        timeLeftMs = (timeLeftMs - (dt * 1000).toLong()).coerceAtLeast(0)
        if (timeLeftMs == 0L) {
            isRunning = false
            sensorManager.unregisterListener(this)
            val xFrac = ballX / width
            val yFrac = ballY / height
            val px = ballX
            val py = ballY
            post {
                onTimerExpired?.invoke(xFrac, yFrac, px, py)
                visibility = INVISIBLE
            }
        }
    }

    // ----- drawing -----

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val timerText = "%.1f".format(timeLeftMs / 1000f)
        val pad = 12f * resources.displayMetrics.density
        val textWidth = timerPaint.measureText(timerText)
        val textHeight = timerPaint.textSize
        val cx = width / 2f
        val timerY = pad + textHeight

        timerBgRect.set(
            cx - textWidth / 2f - pad,
            pad / 2f,
            cx + textWidth / 2f + pad,
            pad / 2f + textHeight + pad
        )
        canvas.drawRoundRect(timerBgRect, cornerRadius, cornerRadius, timerBgPaint)
        canvas.drawText(timerText, cx, timerY, timerPaint)

        val ballRadius = ballSizePx / 2f
        canvas.drawBitmap(ballBitmap, ballX - ballRadius, ballY - ballRadius, null)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }
}
