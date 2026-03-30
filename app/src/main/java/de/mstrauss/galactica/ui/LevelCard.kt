package de.mstrauss.galactica.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StyleableRes
import de.mstrauss.galactica.R
import de.mstrauss.galactica.SinglePlayerActivity
import java.lang.Integer.getInteger


@SuppressLint("ServiceCast")
class LevelCard @JvmOverloads
constructor(private val ctx: Context, private val attributeSet: AttributeSet? = null, private val defStyleAttr: Int = 0)  : LinearLayout(ctx, attributeSet, defStyleAttr) {
    lateinit var levelTitleTextView: TextView
    lateinit var descriptionTextView: TextView
    lateinit var bestTryTextView: TextView
    lateinit var playButton: Button

    init {
        // get the inflater service from the android system
        val inflater = ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        // inflate the layout into "this" component
        inflater.inflate(R.layout.level_card, this)

        val rows : Int
        val cols: Int
        val number: Int
        val planets: Int
        val bombs: Int
        val rocketships: Int
        val availableMoves: Int

        context.theme.obtainStyledAttributes(
            attributeSet,
            R.styleable.LevelCard,
            0, 0).apply {
            try {
                rows = getInteger(R.styleable.LevelCard_rows, 7)
                cols = getInteger(R.styleable.LevelCard_columns, 9)
                number = getInteger(R.styleable.LevelCard_LevelNumber, -1)
                planets = getInteger(R.styleable.LevelCard_planetAmount, 4)
                bombs = getInteger(R.styleable.LevelCard_bombs, 0)
                rocketships = getInteger(R.styleable.LevelCard_rocketships, 0)
                availableMoves = getInteger(R.styleable.LevelCard_availableMoves, rows*cols)
            } finally {
                recycle()
            }
        }

        levelTitleTextView = findViewById(R.id.level_title)
        descriptionTextView = findViewById(R.id.description)
        bestTryTextView = findViewById(R.id.best_try)
        playButton = findViewById(R.id.play_level_button)

        levelTitleTextView.text = "Level $number"
        descriptionTextView.text = "Cols: $cols\nRows: $rows\nPlanets: $planets\nAvalailableMoves: $availableMoves\nBombs: $bombs\nRocketships: $rocketships"
        playButton.setOnClickListener {
            ctx.startActivity(
                SinglePlayerActivity.createIntent(
                    context = ctx,
                    gridRows = rows,
                    gridCols = cols,
                    planetAmount = planets,
                    bombAmount = bombs,
                    rocketshipAmount = rocketships,
                    allowedMoves = availableMoves
                )
            )
        }
        bestTryTextView.text = ""
    }
}
