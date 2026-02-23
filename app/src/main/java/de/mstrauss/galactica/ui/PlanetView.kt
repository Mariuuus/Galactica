package de.mstrauss.galactica.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.ColorUtils
import de.mstrauss.galactica.R
import kotlin.random.Random
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.graphics.set
import kotlin.math.sqrt
import androidx.core.graphics.withClip

class PlanetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    @ColorInt var primaryColor: Int
    @ColorInt var highlightColor: Int

    private val planetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val shadow: Drawable
    private lateinit var sourceBitmap: Bitmap
    private lateinit var sourceCanvas: Canvas
    private lateinit var circlePath: Path


    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.PlanetView,
            0, 0).apply {
            try {
                primaryColor = getColor(R.styleable.PlanetView_primaryColor, 0xFF5E81AC.toInt())
                highlightColor = getColor(R.styleable.PlanetView_highlightColor, 0xFF88C0D0.toInt())
            } finally {
                recycle()
            }
        }

        shadow = AppCompatResources.getDrawable(context, R.drawable.planet_shadow)
            ?: error("Missing drawable: planet_shadow")
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val size = minOf(w, h)
        setMeasuredDimension(size, size)
    }
    private fun drawLine(canvas: Canvas, from: PointF, to: PointF, paint: Paint) {
        canvas.drawLine(from.x, from.y, to.x, to.y, paint)
    }

    private lateinit var pixelBitmap: Bitmap

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        pixelBitmap = createBitmap(w, h)
        sourceBitmap = createBitmap(w, h)
        sourceCanvas = Canvas(sourceBitmap)
        shadow.setBounds(0, 0, w, h)
        circlePath = Path().apply {
            addCircle(w / 2f, h / 2f, minOf(w, h) / 2.02f, Path.Direction.CW)
        }
        regeneratePlanetBitmap()
    }

    private fun regeneratePlanetBitmap() {
        sourceBitmap.eraseColor(Color.TRANSPARENT)
        planetPaint.color = primaryColor

        val bitmapW = pixelBitmap.width
        val bitmapH = pixelBitmap.height
        val cx = bitmapW / 2f
        val cy = bitmapH / 2f
        val radius = minOf(bitmapW, bitmapH) / 2f
        sourceCanvas.drawCircle(cx, cy, radius*2, planetPaint)

        for (i in 0..15) {
            val randomH1 = Random.nextFloat()*2 - 1
            val randomH2 = (randomH1 + (Random.nextFloat() * 0.5f - 0.25f)).coerceIn(-1f, 1f)

            highlightPaint.strokeWidth = Random.nextFloat() * (bitmapW / 4f)
            highlightPaint.color = ColorUtils.blendARGB(primaryColor, highlightColor, Random.nextFloat())
            drawLine(
                sourceCanvas,
                PointF((-1 * bitmapW).toFloat(), cy + randomH1 * radius),
                PointF((1 * bitmapW).toFloat(), cy + randomH2 * radius),
                highlightPaint
            )
        }
        data class MeshNode(val u : Float, val v : Float)
        val gridSize = 8
        val mesh = Array(gridSize) { u ->
            Array(gridSize) { v ->
                MeshNode((u.toFloat() / (gridSize - 1)), (v.toFloat() / (gridSize - 1)))
            }
        }

        val factor = .5f

        val maxOffset = factor * (1f / gridSize)
        val warpedMesh = mesh.map { row ->
            row.map { meshNode ->
                val du = (Random.nextFloat() * 2f - 1f) * maxOffset
                val dv = (Random.nextFloat() * 2f - 1f) * maxOffset
                MeshNode(
                    (meshNode.u + du).coerceIn(0f, 1f),
                    (meshNode.v + dv).coerceIn(0f, 1f)
                )
            }
        }

        //https://paulbourke.net/dataformats/meshwarp/
        //https://davis.wpi.edu/~matt/courses/morph/2d.htm

        // Precompute node positions and displacements once per frame.
        val originalNodes = mesh.flatten()
        val warpedNodes = warpedMesh.flatten()
        val nodeCount = originalNodes.size
        val warpedU = FloatArray(nodeCount)
        val warpedV = FloatArray(nodeCount)
        val dispU = FloatArray(nodeCount)
        val dispV = FloatArray(nodeCount)
        for (i in 0 until nodeCount) {
            val o = originalNodes[i]
            val w = warpedNodes[i]
            warpedU[i] = w.u
            warpedV[i] = w.v
            dispU[i] = o.u - w.u
            dispV[i] = o.v - w.v
        }

        val invW = 1f / bitmapW
        val invH = 1f / bitmapH
        val maxX = bitmapW - 1
        val maxY = bitmapH - 1

        for (y in 0 until bitmapH) {
            val py = y * invH
            for (x in 0 until bitmapW) {
                val px = x * invW
                var dSumX = 0f
                var dSumY = 0f

                for (i in 0 until nodeCount) {
                    val dx = px - warpedU[i]
                    val dy = py - warpedV[i]
                    val dist = sqrt(dx * dx + dy * dy)
                    val w = 1f / (1f + dist)
                    dSumX += dispU[i] * w
                    dSumY += dispV[i] * w
                }

                val sx = ((px + dSumX) * bitmapW).toInt().coerceIn(0, maxX)
                val sy = ((py + dSumY) * bitmapH).toInt().coerceIn(0, maxY)
                pixelBitmap[x, y] = sourceBitmap[sx, sy]
            }
        }
    }

    fun mirrorIndex(i: Int, max: Int): Int {
        if (max <= 0) return 0
        val period = 2 * max
        val m = ((i % period) + period) % period   // positive modulo
        return if (m <= max) m else period - m
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!::pixelBitmap.isInitialized || !::circlePath.isInitialized) return
        canvas.withClip(circlePath) {
            drawBitmap(pixelBitmap, 0f, 0f, null)
            shadow.draw(this)
        }
    }
}
