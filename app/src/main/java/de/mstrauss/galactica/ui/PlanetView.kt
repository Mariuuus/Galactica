package de.mstrauss.galactica.ui

import android.content.Context
import android.graphics.Canvas
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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        shadow.setBounds(0, 0, w, h)
    }

    private fun drawLine(canvas: Canvas, from: PointF, to: PointF, paint: Paint) {
        canvas.drawLine(from.x, from.y, to.x, to.y, paint)
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


        var mesh = Array(10, init = {u -> Array(10, {v -> MeshNode((u.toFloat()/9), (v.toFloat()/9))})})

        planetPaint.color = 0xfffc07ff.toInt()
        for (node in mesh.flatten().map { meshNode -> Coordinate(meshNode.u*width, meshNode.v*width) }) {
            Log.d(this::class.toString(), "x: ${node.x}, y: ${node.y}")
            canvas.drawCircle(node.x, node.y, 3.5f, planetPaint)
        }

        planetPaint.color = 0xffe50200.toInt()
        val factor = .7f

        var warpedMesh = mesh.mapIndexed { u, array -> array.mapIndexed { v, meshNode -> MeshNode(meshNode.u + ((factor * (1/10 - Random.nextFloat()* 1/20))) , meshNode.v + ((factor * (1/10 - Random.nextFloat()* 1/20)))) } }

        for (node in warpedMesh.flatten().map { n -> Coordinate(n.u * width, n.v * width) }) {
            Log.d(this::class.toString(), "x: ${node.x}, y: ${node.y}")
            canvas.drawCircle(node.x, node.y, 3.5f, planetPaint)
        }

        val edges = mesh.flatten().flatMap { meshNode ->
            listOf(
                Edge(meshNode, MeshNode(meshNode.u + 1f / 9f, meshNode.v)),
                Edge(meshNode, MeshNode(meshNode.u, meshNode.v + 1f / 9f))
            )
        }


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

        planetPaint.color = 0xffe50200.toInt()
        for (edge in edgesWarped.map { edge -> CoordinateEdge(Coordinate(edge.n1.u*width, edge.n1.v*width), Coordinate(edge.n2.u*width, edge.n2.v*width)) }) {
            // edge is Edge
            canvas.drawLine(edge.n1.x, edge.n1.y, edge.n2.x, edge.n2.y, planetPaint)
        }



        //https://paulbourke.net/dataformats/meshwarp/
        //https://davis.wpi.edu/~matt/courses/morph/2d.htm
        //canvas.
    }
}
