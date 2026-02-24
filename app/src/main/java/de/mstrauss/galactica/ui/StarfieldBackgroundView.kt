package de.mstrauss.galactica.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.FrameLayout
import kotlin.random.Random

class StarfieldBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private data class Star(
        val x: Float,
        val y: Float,
        val radiusPx: Float,
        val glowRadiusPx: Float
    )

    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 255, 255, 255)
        style = Paint.Style.FILL
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val stars = mutableListOf<Star>()
    private val minStarRadiusPx: Float by lazy {
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PT, 0.4f, resources.displayMetrics)
    }
    private val maxStarRadiusPx: Float by lazy {
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PT, 1.5f, resources.displayMetrics)
    }

    init {
        setWillNotDraw(false)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return

        stars.clear()
        repeat(70) {
            val radiusPx = minStarRadiusPx + Random.nextFloat() * (maxStarRadiusPx - minStarRadiusPx)
            stars.add(
                Star(
                    x = Random.nextFloat() * w,
                    y = Random.nextFloat() * h,
                    radiusPx = radiusPx,
                    glowRadiusPx = radiusPx * 5f
                )
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (star in stars) {
            glowPaint.shader = RadialGradient(
                star.x,
                star.y,
                star.glowRadiusPx,
                Color.argb(30, 255, 255, 255),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(star.x, star.y, star.glowRadiusPx, glowPaint)
            canvas.drawCircle(star.x, star.y, star.radiusPx, starPaint)
        }
    }
}
