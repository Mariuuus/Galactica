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
        shadow.setBounds(0, 0, w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        planetPaint.color = primaryColor

        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(width, height) / 2f
        canvas.drawCircle(cx, cy, radius, planetPaint)

        val circlePath = Path().apply {
            addCircle(cx, cy, radius, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(circlePath)

        for (i in 0..10) {
            val randomH1 = Random.nextFloat()*2 - 1
            val randomH2 = (randomH1 + (Random.nextFloat() * 0.6f - 0.3f)).coerceIn(-1f, 1f)

            highlightPaint.strokeWidth = Random.nextFloat() * (width/3)
            highlightPaint.color = ColorUtils.blendARGB(primaryColor, highlightColor, Random.nextFloat())
            drawLine(
                canvas,
                PointF((-1 * width).toFloat(), cy + randomH1 * radius),
                PointF((1 * width).toFloat(), cy + randomH2 * radius),
                highlightPaint
            )
        }


        shadow.draw(canvas)
        canvas.restore()

        data class MeshNode(val u : Float, val v : Float)
        data class Edge(val n1 : MeshNode, val n2: MeshNode)
        data class Coordinate(var x : Float, var y : Float)
        data class CoordinateEdge(val n1 : Coordinate, val n2: Coordinate)

        val gridSize = 5

        var mesh = Array(gridSize, init = {u -> Array(gridSize, {v -> MeshNode((u.toFloat()/(gridSize-1)), (v.toFloat()/(gridSize-1)))})})

//        planetPaint.color = 0xfffc07ff.toInt()
//        for (node in mesh.flatten().map { meshNode -> Coordinate(meshNode.u*width, meshNode.v*width) }) {
//            Log.d(this::class.toString(), "x: ${node.x}, y: ${node.y}")
//            canvas.drawCircle(node.x, node.y, 3.5f, planetPaint)
//        }

        planetPaint.color = 0xffe50200.toInt()
        val factor = .4f

        var warpedMesh = mesh.mapIndexed { u, array -> array.mapIndexed { v, meshNode -> MeshNode(meshNode.u + ((factor * ((1f/gridSize) - Random.nextFloat()* 1f/(gridSize*2)))) , meshNode.v + ((factor * ((1f/gridSize) - Random.nextFloat()* (1f/gridSize*2))))) } }

//        for (node in warpedMesh.flatten().map { n -> Coordinate(n.u * width, n.v * width) }) {
//            Log.d(this::class.toString(), "x: ${node.x}, y: ${node.y}")
//            canvas.drawCircle(node.x, node.y, 3.5f, planetPaint)
//        }

        val edges= mesh.mapIndexed { i, row ->
            row.mapIndexedNotNull { j, node ->
                val edges = buildList {
                    mesh.getOrNull(i + 1)?.getOrNull(j)?.let { down ->
                        add(Edge(node, down))
                    }
                    row.getOrNull(j + 1)?.let { right ->
                        add(Edge(node, right))
                    }
                }
                edges.takeIf { it.isNotEmpty() }
            }
        }.flatten().flatten()


        val edgesWarped = warpedMesh.mapIndexed { i, row ->
            row.mapIndexedNotNull { j, node ->
                val edges = buildList {
                    warpedMesh.getOrNull(i + 1)?.getOrNull(j)?.let { down ->
                        add(Edge(node, down))
                    }
                    row.getOrNull(j + 1)?.let { right ->
                        add(Edge(node, right))
                    }
                }
                edges.takeIf { it.isNotEmpty() }
            }
        }.flatten().flatten()

//        planetPaint.color = 0xfffc07ff.toInt()
//        for (edge in edges.map { edge -> CoordinateEdge(Coordinate(edge.n1.u*width, edge.n1.v*width), Coordinate(edge.n2.u*width, edge.n2.v*width)) }) {
//            // edge is Edge
//            canvas.drawLine(edge.n1.x, edge.n1.y, edge.n2.x, edge.n2.y, planetPaint)
//        }
//
        planetPaint.color = 0xffe50200.toInt()
        for (edge in edgesWarped.map { edge -> CoordinateEdge(Coordinate(edge.n1.u*width, edge.n1.v*width), Coordinate(edge.n2.u*width, edge.n2.v*width)) }) {
            // edge is Edge
            canvas.drawLine(edge.n1.x, edge.n1.y, edge.n2.x, edge.n2.y, planetPaint)
        }
        //https://paulbourke.net/dataformats/meshwarp/
        //https://davis.wpi.edu/~matt/courses/morph/2d.htm
        //canvas.

        fun clamp01(t: Float) = when {
            t < 0f -> 0f
            t > 1f -> 1f
            else -> t
        }

        fun uvToSegment(edge: CoordinateEdge, p: Coordinate): MeshNode {
            val ax = edge.n1.x
            val ay = edge.n1.y
            val bx = edge.n2.x
            val by = edge.n2.y
            val dx = bx - ax
            val dy = by - ay
            val len2 = dx * dx + dy * dy
            if (len2 == 0f) {
                val ex = p.x - ax
                val ey = p.y - ay
                val dist = kotlin.math.sqrt(ex * ex + ey * ey)
                return MeshNode(0f, dist)
            }
            val px = p.x - ax
            val py = p.y - ay
            val t = (px * dx + py * dy) / len2
            val u = clamp01(t)
            val cx = ax + u * dx
            val cy = ay + u * dy
            val ex = p.x - cx
            val ey = p.y - cy
            val dist = kotlin.math.sqrt(ex * ex + ey * ey)
            val len = kotlin.math.sqrt(len2)
            val cross = dx * py - dy * px
            val vLine = cross / len
            val v = kotlin.math.sign(vLine) * dist
            return MeshNode(u, v)
        }

        for (y in 0 until pixelBitmap.height) {
            for (x in 0 until pixelBitmap.width) {

                val p = Coordinate(x.toFloat(), y.toFloat())
                var dSumX = 0f
                var dSumY = 0f
                var weightSum = 0f

                for ((psiQsiEdge, piQiEdge) in edgesWarped.zip(edges)) {
                    val psi = CoordinateEdge(
                        Coordinate(psiQsiEdge.n1.u * width, psiQsiEdge.n1.v * height),
                        Coordinate(psiQsiEdge.n2.u * width, psiQsiEdge.n2.v * height)
                    )
                    val pi = CoordinateEdge(
                        Coordinate(piQiEdge.n1.u * width, piQiEdge.n1.v * height),
                        Coordinate(piQiEdge.n2.u * width, piQiEdge.n2.v * height)
                    )

                    val uv = uvToSegment(psi, p)
                    val u = uv.u
                    val v = uv.v

                    val ax = pi.n1.x
                    val ay = pi.n1.y
                    val bx = pi.n2.x
                    val by = pi.n2.y
                    val dx = bx - ax
                    val dy = by - ay
                    val len2 = dx * dx + dy * dy
                    if (len2 == 0f) continue
                    val len = kotlin.math.sqrt(len2)
                    val nx = -dy / len
                    val ny = dx / len

                    val mappedX = ax + u * dx + v * nx
                    val mappedY = ay + u * dy + v * ny

                    val dispX = mappedX - p.x
                    val dispY = mappedY - p.y

                    val dist = kotlin.math.abs(v)
                    val w = 1f / (1f + dist)

                    dSumX += dispX * w
                    dSumY += dispY * w
                    weightSum += w
                }

                val srcX = if (weightSum != 0f) p.x + dSumX / weightSum else p.x
                val srcY = if (weightSum != 0f) p.y + dSumY / weightSum else p.y

                val sx = srcX.toInt().coerceIn(0, pixelBitmap.width - 1)
                val sy = srcY.toInt().coerceIn(0, pixelBitmap.height - 1)

                val color = pixelBitmap[sx, sy]
                pixelBitmap[x, y] = color
            }
        }

        canvas.drawBitmap(pixelBitmap, 0f,0f,null)

    }
}
