package de.mstrauss.galactica.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.setPadding
import com.google.android.material.button.MaterialButton
import de.mstrauss.galactica.R
import de.mstrauss.galactica.game.Game

abstract class ItemButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var game: Game? = null

    var itemCount: Int = 0
        set(value) {
            field = value
            countTextView.text = value.toString()
        }

    protected val button: MaterialButton
    private val badge: FrameLayout
    private val countTextView: TextView

    init {
        val buttonSizePx = (36 * resources.displayMetrics.density).toInt()
        val badgeSizePx = (12 * resources.displayMetrics.density).toInt()
        val buttonPaddingPx = (7 * resources.displayMetrics.density).toInt()

        button = MaterialButton(context, null, com.google.android.material.R.attr.materialButtonStyle).apply {
            layoutParams = LayoutParams(buttonSizePx, buttonSizePx)
            backgroundTintList = AppCompatResources.getColorStateList(context, R.color.white)
            letterSpacing = 0f
            minWidth = 0
            setPadding(buttonPaddingPx)
            iconPadding = 0
            iconSize = (24 * resources.displayMetrics.density).toInt()
            iconTint = AppCompatResources.getColorStateList(context, R.color.space_dark)
            insetTop = 0
            insetBottom = 0
            // space_dark (#0B132B) at ~30% alpha — visible ripple on white background
            rippleColor = ColorStateList.valueOf(Color.argb(77, 0x0B, 0x13, 0x2B))
        }

        badge = FrameLayout(context).apply {
            val lp = LayoutParams(badgeSizePx, badgeSizePx).also {
                it.gravity = Gravity.BOTTOM or Gravity.END
            }
            layoutParams = lp
            background = AppCompatResources.getDrawable(context, R.drawable.actually_rounded_rectangle)
            backgroundTintList = AppCompatResources.getColorStateList(context, R.color.space_glow)
        }

        countTextView = TextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            text = itemCount.toString()
            textSize = 8f
            textAlignment = TEXT_ALIGNMENT_CENTER
            gravity = Gravity.CENTER
            setTextColor(AppCompatResources.getColorStateList(context, R.color.space_dark))
        }

        badge.addView(countTextView)
        addView(button)
        addView(badge)

        // Read custom attributes
        context.theme.obtainStyledAttributes(attrs, R.styleable.ItemButtonView, 0, 0).apply {
            try {
                val iconResId = getResourceId(R.styleable.ItemButtonView_itemIcon, 0)
                if (iconResId != 0) {
                    button.icon = AppCompatResources.getDrawable(context, iconResId)
                }
                itemCount = getInteger(R.styleable.ItemButtonView_itemCount, 0)
            } finally {
                recycle()
            }
        }

        button.setOnClickListener {
            val currentGame = game ?: return@setOnClickListener
            onItemUsed(currentGame)
        }
    }

    fun setIcon(drawable: Drawable?) {
        button.icon = drawable
    }

    /**
     * Override this in subclasses to define what happens when the item button is clicked.
     */
    protected abstract fun onItemUsed(game: Game)
}
