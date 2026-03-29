package de.mstrauss.galactica.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
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

class RocketshipItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ItemView(context, attrs, defStyleAttr), SensorEventListener {

    private enum class Phase { IDLE, SELECTING_CELL, CHOOSING_DIRECTION }

    // ----- public API -----

    var gridRows: Int = 0
    var gridCols: Int = 0
    var gridPaddingPx: Float = 0f

    /**
     * Fired when the set of highlighted cells changes.
     * Called with an empty list when the rocketship stops.
     */
    var onHighlightCellsChanged: ((cells: List<Pair<Int, Int>>) -> Unit)? = null

    /** Called when the timer finishes with the game and the cells to reveal. */
    var onActivated: ((Game, List<Pair<Int, Int>>) -> Unit)? = null

    fun start(game: Game) {
        if (phase != Phase.IDLE) return
        this.game = game
        game.state = Game.GameState.BLOCKED
        phase = Phase.SELECTING_CELL
        selectedRow = -1
        selectedCol = -1
        isHorizontal = false
        highlightedCells = emptyList()

        // Highlight all cells to prompt selection
        val allCells = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until gridRows) {
            for (col in 0 until gridCols) {
                allCells.add(Pair(row, col))
            }
        }
        onHighlightCellsChanged?.invoke(allCells)

        visibility = VISIBLE
        invalidate()
    }

    /** Called by the activity when a cell is clicked during the selection phase. */
    fun selectCell(row: Int, col: Int) {
        if (phase != Phase.SELECTING_CELL) return
        selectedRow = row
        selectedCol = col
        phase = Phase.CHOOSING_DIRECTION

        // Clear all-cell highlight, then show initial line
        onHighlightCellsChanged?.invoke(emptyList())

        @Suppress("DEPRECATION")
        displayRotation = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            .defaultDisplay.rotation

        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        // Start the countdown timer (calls super.start() which begins the frame loop)
        super.start()
        updateHighlightedLine()
    }

    override fun stop() {
        sensorManager.unregisterListener(this)
        if (highlightedCells.isNotEmpty() || phase == Phase.SELECTING_CELL) {
            onHighlightCellsChanged?.invoke(emptyList())
        }
        highlightedCells = emptyList()
        phase = Phase.IDLE
        super.stop()
    }

    // ----- internals -----

    private lateinit var game: Game
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var phase = Phase.IDLE
    private var selectedRow = -1
    private var selectedCol = -1
    private var isHorizontal = false
    private var highlightedCells: List<Pair<Int, Int>> = emptyList()

    @Volatile private var accelX = 0f
    @Volatile private var accelY = 0f
    private var displayRotation = Surface.ROTATION_0

    private val iconSizePx = (56 * resources.displayMetrics.density).toInt()
    private val iconBitmap: Bitmap
    private val rotatedIconBitmap: Bitmap

    private val promptPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f * resources.displayMetrics.density
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val promptBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(160, 0x0B, 0x13, 0x2B)
    }
    private val promptBgRect = RectF()
    private val cornerRadius = 8f * resources.displayMetrics.density

    init {
        val drawable = androidx.appcompat.content.res.AppCompatResources.getDrawable(
            context, R.drawable._icon_item_rocketship
        )!!
        iconBitmap = drawable.toBitmap(iconSizePx, iconSizePx)

        val matrix = Matrix()
        matrix.postRotate(90f)
        rotatedIconBitmap = Bitmap.createBitmap(iconBitmap, 0, 0, iconSizePx, iconSizePx, matrix, true)

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
        if (phase != Phase.CHOOSING_DIRECTION) return
        val newHorizontal = accelX.absoluteValue > accelY.absoluteValue
        if (newHorizontal != isHorizontal) {
            isHorizontal = newHorizontal
            updateHighlightedLine()
        }
    }

    override fun onTimerFinished() {
        sensorManager.unregisterListener(this)
        val cells = highlightedCells.toList()
        phase = Phase.IDLE
        onActivated?.invoke(game, cells)
    }

    override fun onDraw(canvas: Canvas) {
        when (phase) {
            Phase.SELECTING_CELL -> drawPrompt(canvas)
            Phase.CHOOSING_DIRECTION -> {
                drawRocketshipOnCell(canvas)
                drawTimer(canvas)
            }
            Phase.IDLE -> {}
        }
    }

    // ----- drawing helpers -----

    private fun drawPrompt(canvas: Canvas) {
        val text = "Select a cell!"
        val pad = 16f * resources.displayMetrics.density
        val textWidth = promptPaint.measureText(text)
        val textHeight = promptPaint.textSize
        val cx = width / 2f
        val cy = height / 2f

        promptBgRect.set(
            cx - textWidth / 2f - pad,
            cy - textHeight / 2f - pad,
            cx + textWidth / 2f + pad,
            cy + textHeight / 2f + pad
        )
        canvas.drawRoundRect(promptBgRect, cornerRadius, cornerRadius, promptBgPaint)
        canvas.drawText(text, cx, cy + textHeight / 3f, promptPaint)
    }

    private fun drawRocketshipOnCell(canvas: Canvas) {
        if (selectedRow < 0 || selectedCol < 0) return
        if (gridRows <= 0 || gridCols <= 0) return

        val effectiveW = width - 2f * gridPaddingPx
        val effectiveH = height - 2f * gridPaddingPx
        val cellW = effectiveW / gridCols
        val cellH = effectiveH / gridRows

        val cellCenterX = gridPaddingPx + selectedCol * cellW + cellW / 2f
        val cellCenterY = gridPaddingPx + selectedRow * cellH + cellH / 2f

        val bitmap = if (isHorizontal) rotatedIconBitmap else iconBitmap
        canvas.drawBitmap(
            bitmap,
            cellCenterX - iconSizePx / 2f,
            cellCenterY - iconSizePx / 2f,
            null
        )
    }

    // ----- highlight logic -----

    private fun updateHighlightedLine() {
        if (selectedRow < 0 || selectedCol < 0) return

        val cells = mutableListOf<Pair<Int, Int>>()
        if (isHorizontal) {
            for (col in 0 until gridCols) {
                cells.add(Pair(selectedRow, col))
            }
        } else {
            for (row in 0 until gridRows) {
                cells.add(Pair(row, selectedCol))
            }
        }

        highlightedCells = cells
        onHighlightCellsChanged?.invoke(cells)
    }
}
