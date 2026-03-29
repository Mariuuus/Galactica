package de.mstrauss.galactica.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.AttributeSet
import android.view.Surface
import android.view.WindowManager
import androidx.core.graphics.drawable.toBitmap
import de.mstrauss.galactica.R
import de.mstrauss.galactica.game.Game
import kotlin.math.absoluteValue
import kotlin.math.pow

class BombItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ItemView(context, attrs, defStyleAttr), SensorEventListener {

    // ----- public API -----

    /** Grid dimensions and padding — set before calling start(). */
    var gridRows: Int = 0
    var gridCols: Int = 0
    var gridPaddingPx: Float = 0f

    /**
     * Fired when the set of 4 nearest cells changes while the bomb is active.
     * Called with an empty list when the bomb stops.
     */
    var onNearestCellsChanged: ((cells: List<Pair<Int, Int>>) -> Unit)? = null

    /** Start the bomb session. Sets game state to BLOCKED until the timer expires. */
    fun start(game: Game) {
        if (isRunning) return
        this.game = game
        game.state = Game.GameState.BLOCKED
        ballX = width / 2f
        ballY = height / 2f
        velX = 0f
        velY = 0f
        lastNearestCells = emptyList()

        @Suppress("DEPRECATION")
        displayRotation = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            .defaultDisplay.rotation

        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        super.start()
    }

    override fun stop() {
        sensorManager.unregisterListener(this)
        if (lastNearestCells.isNotEmpty()) {
            lastNearestCells = emptyList()
            onNearestCellsChanged?.invoke(emptyList())
        }
        super.stop()
    }

    // ----- internals -----

    private lateinit var game: Game
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val ballSizePx = (40 * resources.displayMetrics.density)
    private val ballBitmap: Bitmap
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var ballX = 0f
    private var ballY = 0f
    private var velX = 0f
    private var velY = 0f

    @Volatile private var accelX = 0f
    @Volatile private var accelY = 0f

    private var displayRotation = Surface.ROTATION_0
    private var lastNearestCells: List<Pair<Int, Int>> = emptyList()

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

    // ----- SensorEventListener -----

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        when (displayRotation) {
            Surface.ROTATION_90  -> { accelX =  event.values[1]; accelY =  event.values[0] }
            Surface.ROTATION_270 -> { accelX = -event.values[1]; accelY = -event.values[0] }
            else                 -> { accelX =  event.values[0]; accelY =  event.values[1] }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    // ----- ItemView overrides -----

    override fun onFrame(dt: Float) {
        if (dt <= 0f || dt > 0.1f) return

        val damping = 0.55f.pow(dt)
        val sensitivity = 300f

        velX = (velX + accelX * sensitivity * dt) * damping
        velY = (velY + accelY * sensitivity * dt) * damping

        val ballRadius = ballSizePx / 2f
        val newX = ballX + velX * dt
        val newY = ballY + velY * dt

        if (newX < ballRadius) {
            ballX = ballRadius; velX = velX.absoluteValue * 0.4f
        } else if (newX > width - ballRadius) {
            ballX = width - ballRadius; velX = -velX.absoluteValue * 0.4f
        } else ballX = newX

        if (newY < ballRadius) {
            ballY = ballRadius; velY = velY.absoluteValue * 0.4f
        } else if (newY > height - ballRadius) {
            ballY = height - ballRadius; velY = -velY.absoluteValue * 0.4f
        } else ballY = newY

        updateNearestCells()
    }

    override fun onTimerFinished() {
        sensorManager.unregisterListener(this)
        game.useBomb(lastNearestCells)
    }

    override fun onDraw(canvas: Canvas) {
        val ballRadius = ballSizePx / 2f
        val glowRadius = ballRadius * 2.5f
        glowPaint.shader = RadialGradient(
            ballX, ballY, glowRadius,
            Color.argb(100, 0xA9, 0x9B, 0x45),
            Color.argb(0,   0x89, 0x2D, 0x2D),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(ballX, ballY, glowRadius, glowPaint)
        canvas.drawBitmap(ballBitmap, ballX - ballRadius, ballY - ballRadius, null)
        drawTimer(canvas)
    }

    // ----- nearest-cell tracking -----

    private fun updateNearestCells() {
        if (gridRows <= 0 || gridCols <= 0 || width <= 0 || height <= 0) return

        val effectiveW = width - 2f * gridPaddingPx
        val effectiveH = height - 2f * gridPaddingPx
        if (effectiveW <= 0f || effectiveH <= 0f) return

        val cellW = effectiveW / gridCols
        val cellH = effectiveH / gridRows
        val localX = ballX - gridPaddingPx
        val localY = ballY - gridPaddingPx

        val leftCol = (localX / cellW - 0.5f).toInt().coerceIn(0, gridCols - 2)
        val topRow  = (localY / cellH - 0.5f).toInt().coerceIn(0, gridRows - 2)

        val newCells = listOf(
            Pair(topRow,     leftCol),
            Pair(topRow,     leftCol + 1),
            Pair(topRow + 1, leftCol),
            Pair(topRow + 1, leftCol + 1)
        )

        if (newCells != lastNearestCells) {
            lastNearestCells = newCells
            onNearestCellsChanged?.invoke(newCells)
        }
    }
}
