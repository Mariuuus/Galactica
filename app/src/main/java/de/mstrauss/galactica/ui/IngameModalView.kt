package de.mstrauss.galactica.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.LinearLayout
import de.mstrauss.galactica.R

class IngameModalView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var cardContainer: LinearLayout

    init {
        LayoutInflater.from(context).inflate(R.layout.view_ingame_modal, this, true)
        cardContainer = findViewById(R.id.ingame_modal_card)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        val extraChildren = mutableListOf<View>()
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.id != R.id.ingame_modal_overlay && child.id != R.id.ingame_modal_card) {
                extraChildren.add(child)
            }
        }

        for (child in extraChildren) {
            removeView(child)
            cardContainer.addView(child)
        }
    }

    fun show() {
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
    }
}
