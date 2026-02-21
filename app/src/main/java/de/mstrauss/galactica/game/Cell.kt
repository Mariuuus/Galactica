package de.mstrauss.galactica.game

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.widget.GridLayout
import com.google.android.material.button.MaterialButton
import de.mstrauss.galactica.R
import de.mstrauss.galactica.util.exceptions.CellChangeNotAllowedGameExceptions

class Cell @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.materialButtonStyle
) : MaterialButton(ContextThemeWrapper(context, R.style.GalacticaButton_Grid), attrs, defStyleAttr) {

    enum class CellType {
        PLANET,
        HINT,
    }

    var hintNumber: Int = 0
    var posX: Int = 0
    var posY: Int = 0
    var cellType: CellType = CellType.HINT
        set(value) {
            field = value
            updateAppearance()
        }
    var flagged: Boolean = false
        set(value) {
            if(revealed) throw CellChangeNotAllowedGameExceptions()
            field = value
            updateAppearance()
        }
    var revealed: Boolean = false
        set(value) {
            if(flagged) throw CellChangeNotAllowedGameExceptions()
            field = value
            updateAppearance()
        }

    var onCellClick: ((Cell) -> Unit)? = null

    init {
        setOnClickListener { onCellClick?.invoke(this) }
    }
    fun isPlanet() : Boolean = cellType == CellType.PLANET

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
            when (cellType) {
                CellType.PLANET -> "P"
                CellType.HINT -> hintNumber.toString()
            }
        } else if (flagged) {
            "FLAGGED"
        } else {
            ""
        }
        invalidate()
    }
}
