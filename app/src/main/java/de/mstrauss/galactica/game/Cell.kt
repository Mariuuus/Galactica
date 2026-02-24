package de.mstrauss.galactica.game

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.GridLayout
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.button.MaterialButton
import de.mstrauss.galactica.R
import de.mstrauss.galactica.util.exceptions.CellChangeNotAllowedGameExceptions
import kotlin.math.max

class Cell @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class ContentMode {
        BUTTON,
        TEXT,
        FLAGGED
    }

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

    private val buttonView = MaterialButton(
        ContextThemeWrapper(context, R.style.ThemeOverlay_Galactica_GridCell),
        null,
        com.google.android.material.R.attr.materialButtonStyle
    ).apply {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        text = ""
        setOnClickListener { onCellClick?.invoke(this@Cell) }
    }

    private fun newCircleOverlayTextView() = object : androidx.appcompat.widget.AppCompatTextView(context) {
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            val size = max(measuredWidth, measuredHeight)
            setMeasuredDimension(size, size)
        }
    }.apply {
        layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )
        gravity = Gravity.CENTER
        textAlignment = TEXT_ALIGNMENT_CENTER
        setPadding(
            (6 * resources.displayMetrics.density).toInt(),
            (4 * resources.displayMetrics.density).toInt(),
            (6 * resources.displayMetrics.density).toInt(),
            (4 * resources.displayMetrics.density).toInt()
        )
        background = AppCompatResources.getDrawable(context, R.drawable.cell_text_circle)
        text = ""
        visibility = GONE
        setOnClickListener { onCellClick?.invoke(this@Cell) }
    }

    private val textView = newCircleOverlayTextView()
    private val flagCircleView = newCircleOverlayTextView()

    var contentMode: ContentMode = ContentMode.BUTTON
        private set

    var onCellClick: ((Cell) -> Unit)? = null

    init {
        addView(buttonView)
        addView(textView)
        addView(flagCircleView)
        isClickable = true
        isFocusable = true
        setOnClickListener { onCellClick?.invoke(this) }
        updateAppearance()
    }

    fun isPlanet(): Boolean = cellType == CellType.PLANET

    fun showButton() {
        contentMode = ContentMode.BUTTON
        buttonView.visibility = VISIBLE
        textView.visibility = GONE
        flagCircleView.visibility = GONE
    }

    fun showText() {
        contentMode = ContentMode.TEXT
        buttonView.visibility = GONE
        textView.visibility = VISIBLE
        flagCircleView.visibility = GONE
    }

    fun showFlagged() {
        contentMode = ContentMode.FLAGGED
        buttonView.visibility = VISIBLE
        textView.visibility = GONE
        flagCircleView.visibility = VISIBLE
    }

    fun showPlanet() {

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
        val label = if (revealed) {
            when (cellType) {
                CellType.PLANET -> "P"
                CellType.HINT -> hintNumber.toString()
            }
        } else {
            ""
        }

        textView.text = label

        if(isPlanet()) {
            showPlanet()
        } else if (revealed) {
            showText()
        } else if (flagged) {
            showFlagged()
        } else {
            showButton()
        }

        invalidate()
    }
}
