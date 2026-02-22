package de.mstrauss.galactica.game

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.minus
import androidx.core.graphics.plus

class GridLinesOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var rows: Int = 1
        private set
    var cols: Int = 1
        private set

    var cellWidth: Float = 0f
        private set
    var cellHeight: Float = 0f
        private set

    val topLeft = PointF()
    val topRight = PointF()
    val bottomLeft = PointF()
    val bottomRight = PointF()

    fun setGridSize(rows: Int, cols: Int) {
        this.rows = rows.coerceAtLeast(1)
        this.cols = cols.coerceAtLeast(1)
        updateCornerCenters()
        invalidate()
    }

    private fun updateCornerCenters() {
        if (width <= 0 || height <= 0) {
            topLeft.set(0f, 0f)
            topRight.set(0f, 0f)
            bottomLeft.set(0f, 0f)
            bottomRight.set(0f, 0f)
            return
        }

        val contentLeft = paddingLeft.toFloat()
        val contentTop = paddingTop.toFloat()
        val contentRight = (width - paddingRight).toFloat()
        val contentBottom = (height - paddingBottom).toFloat()

        this.cellWidth = (contentRight - contentLeft) / cols
        this.cellHeight = (contentBottom - contentTop) / rows

        val halfCellWidth = cellWidth / 2f
        val halfCellHeight = cellHeight / 2f

        topLeft.set(contentLeft + halfCellWidth, contentTop + halfCellHeight)
        topRight.set(contentRight - halfCellWidth, contentTop + halfCellHeight)
        bottomLeft.set(contentLeft + halfCellWidth, contentBottom - halfCellHeight)
        bottomRight.set(contentRight - halfCellWidth, contentBottom - halfCellHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateCornerCenters()
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0x99FFFFFF.toInt()
        strokeWidth = 2f * resources.displayMetrics.density // ~2dp
        strokeCap = Paint.Cap.ROUND
    }

    private fun drawLine(canvas: Canvas, from: PointF, to: PointF) {
        canvas.drawLine(from.x, from.y, to.x, to.y, linePaint)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)


        for (y in (0..rows).map { n -> n * cellHeight }) {
            drawLine(canvas, topLeft + PointF(0f, y), topRight + PointF(0f, y))

        }

        for (x in (0..cols).map { n -> n * cellWidth }) {
            drawLine(canvas, topLeft + PointF(x, 0f), bottomLeft + PointF(x, 0f))
        }

        for (y in (0..<rows-1).map { n -> n * cellHeight }) {
            for (x in (0..<cols-1).map { n -> n * cellWidth }) {
                drawLine(canvas, topLeft + PointF(x, y), topLeft + PointF(x + cellWidth, y + cellHeight))
                drawLine(canvas, topLeft + PointF(x+cellWidth, y), topLeft + PointF(x, y + cellHeight))
            }
        }
    }
}
