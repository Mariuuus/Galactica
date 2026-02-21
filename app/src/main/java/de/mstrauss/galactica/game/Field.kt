package de.mstrauss.galactica.game

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.widget.GridLayout
import com.google.android.material.button.MaterialButton
import de.mstrauss.galactica.R

class Field @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.materialButtonStyle
) : MaterialButton(ContextThemeWrapper(context, R.style.GalacticaButton_Grid), attrs, defStyleAttr) {

    enum class FieldType {
        PLANET,
        ZERO,
        ONE,
        TWO,
        THREE,
        FOUR
    }

    var posX: Int = 0
    var posY: Int = 0
    var fieldType: FieldType = FieldType.ZERO
        set(value) {
            field = value
            updateAppearance()
        }
    var flagged: Boolean = false
    var revealed: Boolean = false
        set(value) {
            field = value
            updateAppearance()
        }

    var onFieldClick: ((Field) -> Unit)? = null

    init {
        setOnClickListener { onFieldClick?.invoke(this) }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        val parentView = parent
        if (parentView is GridLayout) {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
        }
    }

    private fun updateAppearance() {
        text = if (revealed) {
            when (fieldType) {
                FieldType.PLANET -> "P"
                FieldType.ZERO -> "0"
                FieldType.ONE -> "1"
                FieldType.TWO -> "2"
                FieldType.THREE -> "3"
                FieldType.FOUR -> "4"
            }
        } else {
            ""
        }
        invalidate()
    }
}
