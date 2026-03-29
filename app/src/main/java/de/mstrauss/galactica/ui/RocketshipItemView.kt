package de.mstrauss.galactica.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.core.graphics.drawable.toBitmap
import de.mstrauss.galactica.R
import de.mstrauss.galactica.game.Game

class RocketshipItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ItemView(context, attrs, defStyleAttr) {

    // ----- public API -----

    /** Set before calling start(). */
    var game: Game? = null

    /**
     * Called when the countdown finishes.
     * TODO: implement rocketship game logic here.
     */
    var onActivated: ((Game) -> Unit)? = null

    fun start(game: Game) {
        if (isRunning) return
        this.game = game
        super.start()
    }

    // ----- internals -----

    private val iconSizePx = (56 * resources.displayMetrics.density).toInt()
    private val iconBitmap: Bitmap

    init {
        val drawable = androidx.appcompat.content.res.AppCompatResources.getDrawable(
            context, R.drawable._icon_item_rocketship
        )!!
        iconBitmap = drawable.toBitmap(iconSizePx, iconSizePx)
        visibility = INVISIBLE
    }

    // ----- ItemView overrides -----

    override fun onFrame(dt: Float) {
        // No physics — rocketship item is aim-free (no tilt control needed)
    }

    override fun onTimerFinished() {
        val currentGame = game ?: return
        onActivated?.invoke(currentGame)
        // TODO: call the relevant Game method once implemented
    }

    override fun onDraw(canvas: Canvas) {
        // Draw icon centred in the view
        val left = (width - iconSizePx) / 2f
        val top  = (height - iconSizePx) / 2f
        canvas.drawBitmap(iconBitmap, left, top, null)
        drawTimer(canvas)
    }
}
