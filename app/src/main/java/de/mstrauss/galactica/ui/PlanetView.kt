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
import android.util.Log
import android.view.View
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.ColorUtils
import de.mstrauss.galactica.R
import kotlin.random.Random
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.graphics.set
import kotlin.math.*

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
        pixelBitmap = createBitmap(w, h)
        sourceBitmap = createBitmap(w, h)
        sourceCanvas = Canvas(sourceBitmap)
        shadow.setBounds(0, 0, w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        sourceBitmap.eraseColor(Color.TRANSPARENT)
        planetPaint.color = primaryColor

        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(width, height) / 2f
        sourceCanvas.drawCircle(cx, cy, radius*2, planetPaint)

        val circlePath = Path().apply {
            addCircle(cx, cy, radius, Path.Direction.CW)
        }

        for (i in 0..15) {
            val randomH1 = Random.nextFloat()*2 - 1
            val randomH2 = (randomH1 + (Random.nextFloat() * 0.5f - 0.25f)).coerceIn(-1f, 1f)

            highlightPaint.strokeWidth = Random.nextFloat() * (width/4)
            highlightPaint.color = ColorUtils.blendARGB(primaryColor, highlightColor, Random.nextFloat())
            drawLine(
                sourceCanvas,
                PointF((-1 * width).toFloat(), cy + randomH1 * radius),
                PointF((1 * width).toFloat(), cy + randomH2 * radius),
                highlightPaint
            )
        }

        data class MeshNode(val u : Float, val v : Float)
        data class Edge(val n1 : MeshNode, val n2: MeshNode)
        data class Coordinate(var x : Float, var y : Float)

        val gridSize = 20
        var mesh = Array(gridSize, init = {u -> Array(gridSize, {v -> MeshNode((u.toFloat()/(gridSize-1)), (v.toFloat()/(gridSize-1)))})})

        val factor = .8f

        val maxOffset = factor * (1f / gridSize)
        var warpedMesh = mesh.map { row ->
            row.map { meshNode ->
                val du = (Random.nextFloat() * 2f - 1f) * maxOffset
                val dv = (Random.nextFloat() * 2f - 1f) * maxOffset
                MeshNode(
                    (meshNode.u + du).coerceIn(0f, 1f),
                    (meshNode.v + dv).coerceIn(0f, 1f)
                )
            }
        }

//        val edges= mesh.mapIndexed { i, row ->
//            row.mapIndexedNotNull { j, node ->
//                val edges = buildList {
//                    mesh.getOrNull(i + 1)?.getOrNull(j)?.let { down ->
//                        add(Edge(node, down))
//                    }
//                    row.getOrNull(j + 1)?.let { right ->
//                        add(Edge(node, right))
//                    }
//                }
//                edges.takeIf { it.isNotEmpty() }
//            }
//        }.flatten().flatten()
//
//
//        val edgesWarped = warpedMesh.mapIndexed { i, row ->
//            row.mapIndexedNotNull { j, node ->
//                val edges = buildList {
//                    warpedMesh.getOrNull(i + 1)?.getOrNull(j)?.let { down ->
//                        add(Edge(node, down))
//                    }
//                    row.getOrNull(j + 1)?.let { right ->
//                        add(Edge(node, right))
//                    }
//                }
//                edges.takeIf { it.isNotEmpty() }
//            }
//        }.flatten().flatten()

//        planetPaint.color = 0xfffc07ff.toInt()
//        for (edge in edges.map { edge -> CoordinateEdge(Coordinate(edge.n1.u*width, edge.n1.v*width), Coordinate(edge.n2.u*width, edge.n2.v*width)) }) {
//            // edge is Edge
//            canvas.drawLine(edge.n1.x, edge.n1.y, edge.n2.x, edge.n2.y, planetPaint)
//        }
//
        //https://paulbourke.net/dataformats/meshwarp/
        //https://davis.wpi.edu/~matt/courses/morph/2d.htm
        //canvas.

        for (y in 0 until pixelBitmap.height) {
            for (x in 0 until pixelBitmap.width) {

                val p = Coordinate(x.toFloat()/pixelBitmap.width, y.toFloat()/pixelBitmap.height)
                var dSumX = 0f
                var dSumY = 0f
                var weightSum = 0f

                for ((warpedNode, originalNode) in warpedMesh.flatten().zip(mesh.flatten())) {
                    // Node displacement (warped -> original) in UV space.
                    val displacementX = originalNode.u - warpedNode.u
                    val displacementY = originalNode.v - warpedNode.v

                    // Weight by distance from current pixel to warped node.
                    val dx = p.x - warpedNode.u
                    val dy = p.y - warpedNode.v
                    val dist = sqrt(dx * dx + dy * dy)
                    val w = 1f / (1f + dist)

                    dSumX += displacementX * w
                    dSumY += displacementY * w
                    weightSum += w
                }
                val srcX = (p.x + dSumX) * pixelBitmap.width
                val srcY = (p.y + dSumY) * pixelBitmap.height

                val sx = srcX.toInt().coerceIn(0, pixelBitmap.width - 1)
                val sy = srcY.toInt().coerceIn(0, pixelBitmap.height - 1)

                val color = sourceBitmap[sx, sy]
                pixelBitmap[x, y] = color
            }
        }
        canvas.save()
        canvas.clipPath(circlePath)
        //canvas.drawBitmap(sourceBitmap, 0f, 0f, null)
        canvas.drawBitmap(pixelBitmap, 0f, 0f, null)
        shadow.draw(canvas)
        canvas.restore()
    }
}
