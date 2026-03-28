package de.mstrauss.galactica.game

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.GridLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import de.mstrauss.galactica.R
import de.mstrauss.galactica.ui.PlanetView
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
        FLAGGED,
        PLANET
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
            if (revealed) throw CellChangeNotAllowedGameExceptions()
            field = value
            updateAppearance()
        }
    var revealed: Boolean = false
        set(value) {
            if(flagged) throw CellChangeNotAllowedGameExceptions()
            field = value
            updateAppearance()
        }

    private val hintBgColor = Color.parseColor("#2F4E8C")
    private val planetBgColor = Color.parseColor("#E2B93B")

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

    private fun newCircleOverlayTextView() = object : AppCompatTextView(context) {
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
    private val planetView = PlanetView(context).apply {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
            Gravity.CENTER
        )
        visibility = GONE
        setPadding(
            (10 * resources.displayMetrics.density).toInt(),
            (10 * resources.displayMetrics.density).toInt(),
            (10 * resources.displayMetrics.density).toInt(),
            (10 * resources.displayMetrics.density).toInt()
        )
        setOnClickListener { onCellClick?.invoke(this@Cell) }
    }
    private val flagView = object : FrameLayout(context) {
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
        setPadding(
            (6 * resources.displayMetrics.density).toInt(),
            (4 * resources.displayMetrics.density).toInt(),
            (6 * resources.displayMetrics.density).toInt(),
            (4 * resources.displayMetrics.density).toInt()
        )
        background = AppCompatResources.getDrawable(context, R.drawable.cell_text_circle)
        backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(context, R.color.galactica_red)
        )
        visibility = GONE
        setOnClickListener { onCellClick?.invoke(this@Cell) }

        addView(
            AppCompatImageView(context).apply {
                layoutParams = LayoutParams(
                    (14 * resources.displayMetrics.density).toInt(),
                    (14 * resources.displayMetrics.density).toInt(),
                    Gravity.CENTER
                )
                setImageResource(R.drawable._icon_flagtriangleright)
                imageTintList = ColorStateList.valueOf(Color.WHITE)
            }
        )
    }

    var contentMode: ContentMode = ContentMode.BUTTON
        private set

    var onCellClick: ((Cell) -> Unit)? = null

    var highlighted: Boolean = false
        set(value) {
            field = value
            highlightOverlay.visibility = if (value) VISIBLE else INVISIBLE
        }

    private val highlightOverlay = FrameLayout(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        setBackgroundColor(Color.argb(80, 0x5B, 0xC0, 0xEB)) // space_glow at ~30% alpha
        visibility = INVISIBLE
        isClickable = false
    }

    fun highlight() { highlighted = true }
    fun dehighlight() { highlighted = false }

    init {
        addView(buttonView)
        addView(planetView)
        addView(textView)
        addView(flagView)
        addView(highlightOverlay) // topmost so it overlays any content mode
        isClickable = true
        isFocusable = true
        setOnClickListener { onCellClick?.invoke(this) }
        updateAppearance()
    }

    fun isPlanet(): Boolean = cellType == CellType.PLANET

    fun showButton() {
        contentMode = ContentMode.BUTTON
        buttonView.visibility = VISIBLE
        planetView.visibility = GONE
        textView.visibility = GONE
        flagView.visibility = GONE
    }

    fun showText() {
        contentMode = ContentMode.TEXT
        buttonView.visibility = GONE
        planetView.visibility = GONE
        textView.visibility = VISIBLE
        flagView.visibility = GONE
    }

    fun showFlagged() {
        contentMode = ContentMode.FLAGGED
        buttonView.visibility = VISIBLE
        planetView.visibility = GONE
        textView.visibility = GONE
        flagView.visibility = VISIBLE
    }

    fun showPlanet() {
        contentMode = ContentMode.PLANET
        buttonView.visibility = GONE
        planetView.visibility = VISIBLE
        textView.visibility = GONE
        flagView.visibility = GONE
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
        if (revealed) {
            if (isPlanet()) {
                showPlanet()
            } else {
                configureRevealedText()
                showText()
            }
        } else if (flagged) {
            showFlagged()
        } else {
            showButton()
        }
    }

    private fun configureRevealedText() {
        val isPlanet = isPlanet()
        textView.text = if (isPlanet) "P" else hintNumber.toString()
        textView.typeface = if (isPlanet) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        textView.setTextColor(if (isPlanet) Color.BLACK else Color.WHITE)
        textView.backgroundTintList = ColorStateList.valueOf(if (isPlanet) planetBgColor else hintBgColor)
    }
}
