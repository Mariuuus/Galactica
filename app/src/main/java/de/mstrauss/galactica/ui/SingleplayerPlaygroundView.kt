package de.mstrauss.galactica.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import de.mstrauss.galactica.R
import de.mstrauss.galactica.SinglePlayerActivity

class SingleplayerPlaygroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val playButton: Button
    private val colsTextView: TextView
    private val colsSeekBar: SeekBar
    private val rowsTextView: TextView
    private val rowsSeekBar: SeekBar
    private val planetsTextView: TextView
    private val planetsSeekBar: SeekBar
    private val bombsTextView: TextView
    private val bombsSeekBar: SeekBar
    private val rocketShipsTextView: TextView
    private val rocketShipsSeekBar: SeekBar

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_singleplayer_playground, this, true)

        playButton = findViewById(R.id.start_singleplayer_playground_button)

        colsTextView = findViewById(R.id.cols_text)
        colsSeekBar = findViewById(R.id.seek_cols)

        rowsTextView = findViewById(R.id.rows_text)
        rowsSeekBar = findViewById(R.id.seek_rows)

        planetsTextView = findViewById(R.id.my_planets)
        planetsSeekBar = findViewById(R.id.seek_planets)

        bombsTextView = findViewById(R.id.bombs_text)
        bombsSeekBar = findViewById(R.id.seek_bombs)

        rocketShipsTextView = findViewById(R.id.rocketships_text)
        rocketShipsSeekBar = findViewById(R.id.seek_rocketships)

        planetsSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                planetsTextView.text = progress.toString()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        rowsSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                rowsTextView.text = progress.toString()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        colsSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                colsTextView.text = progress.toString()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        bombsSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                bombsTextView.text = progress.toString()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        rocketShipsSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                rocketShipsTextView.text = progress.toString()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        playButton.setOnClickListener {
            context.startActivity(
                SinglePlayerActivity.createIntent(
                    context = context,
                    gridRows = rowsSeekBar.progress,
                    gridCols = colsSeekBar.progress,
                    planetAmount = planetsSeekBar.progress,
                    bombAmount = bombsSeekBar.progress,
                    rocketshipAmount = rocketShipsSeekBar.progress,
                    allowedMoves = rowsSeekBar.progress * colsSeekBar.progress
                )
            )
        }
    }
}
