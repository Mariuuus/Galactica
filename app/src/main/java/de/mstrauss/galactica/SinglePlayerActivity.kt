package de.mstrauss.galactica

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import android.widget.Switch
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import de.mstrauss.galactica.game.Cell
import de.mstrauss.galactica.game.Game
import de.mstrauss.galactica.game.GridLinesOverlayView
import de.mstrauss.galactica.ui.IngameModalView
import de.mstrauss.galactica.ui.applyFullscreen

class SinglePlayerActivity : AppCompatActivity() {
    companion object {
        private const val EXTRA_GRID_ROWS = "extra_grid_rows"
        private const val EXTRA_GRID_COLS = "extra_grid_cols"
        private const val EXTRA_PLANET_AMOUNT = "extra_planet_amount"
        private const val EXTRA_ALLOWED_MOVES = "extra_allowed_moves"
        private const val EXTRA_WON = "extra_won"

        private const val DEFAULT_GRID_ROWS = 7
        private const val DEFAULT_GRID_COLS = 9
        private const val DEFAULT_PLANET_AMOUNT = 4

        fun createIntent(
            context: Context,
            gridRows: Int = DEFAULT_GRID_ROWS,
            gridCols: Int = DEFAULT_GRID_COLS,
            planetAmount: Int = DEFAULT_PLANET_AMOUNT,
            allowedMoves: Int = gridRows * gridCols
        ): Intent = Intent(context, SinglePlayerActivity::class.java).apply {
            putExtra(EXTRA_GRID_ROWS, gridRows)
            putExtra(EXTRA_GRID_COLS, gridCols)
            putExtra(EXTRA_PLANET_AMOUNT, planetAmount)
            putExtra(EXTRA_ALLOWED_MOVES, allowedMoves)
        }
    }

    lateinit var game: Game

    lateinit var movesLeftTextView: TextView
    lateinit var planetsFoundTextView: TextView
    lateinit var winModal: IngameModalView
    lateinit var loseModal: IngameModalView
    lateinit var pauseModal: IngameModalView
    lateinit var revealFlaggedModal : IngameModalView

    lateinit var resumeButton: Button
    lateinit var pauseButton: Button


    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ingame_single_player)
        applyFullscreen()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val gridRows = intent.getIntExtra(EXTRA_GRID_ROWS, DEFAULT_GRID_ROWS)
        val gridCols = intent.getIntExtra(EXTRA_GRID_COLS, DEFAULT_GRID_COLS)
        val planetAmount = intent.getIntExtra(EXTRA_PLANET_AMOUNT, DEFAULT_PLANET_AMOUNT)
        val allowedMoves = intent.getIntExtra(EXTRA_ALLOWED_MOVES, gridRows * gridCols)

        game = Game(
            gridRows = gridRows,
            gridCols = gridCols,
            planetAmount = planetAmount,
            allowedMoves = allowedMoves,
            context = this,
            onUIRefresh = { refreshUITextElements(it) },
            handleRevealFlaggedField = { handleRevealFlaggedField(it) }
        )

        val grid = findViewById<GridLayout>(R.id.single_player_grid)
        val gridOverlay = findViewById<GridLinesOverlayView>(R.id.single_player_grid_overlay)
        grid.rowCount = game.gridRows
        grid.columnCount = game.gridCols
        gridOverlay.setGridSize(game.gridRows, game.gridCols)

        for (row in 0 until game.gridRows) {
            for (col in 0 until game.gridCols) {
                grid.addView(game.field[row][col])
            }
        }

        movesLeftTextView = findViewById(R.id.moves_left_text_view)
        planetsFoundTextView = findViewById(R.id.planets_founds_text_view)

        winModal = findViewById(R.id.win_modal)
        loseModal = findViewById(R.id.lose_modal)
        pauseModal = findViewById(R.id.pause_modal)
        revealFlaggedModal = findViewById(R.id.reveal_flagged_cell_modal)

        pauseButton = findViewById(R.id.pause_button)
        resumeButton = findViewById(R.id.resume_button)

        pauseButton.setOnClickListener { pauseModal.show() }
        resumeButton.setOnClickListener { pauseModal.hide() }

        for (btn in listOf<Button>(
            findViewById(R.id.single_lost_reset_button),
            findViewById(R.id.single_won_reset_button),
            findViewById(R.id.single_pause_reset_button)
        )) {
            btn.setOnClickListener { reset() }
        }

        for (btn in listOf<Button>(
            findViewById(R.id.single_pause_end_button),
            findViewById(R.id.single_won_end_button),
            findViewById(R.id.single_lost_end_button)
        )) {
            btn.setOnClickListener { exit(game.state == Game.GameState.WON) }
        }

        val flaggingModeToggle = findViewById<Switch>(R.id.single_flagging_toggle)
        flaggingModeToggle.setOnClickListener { game.flagMode = flaggingModeToggle.isChecked }

        refreshUITextElements(null)
    }

    private fun hideAllModals() {
        pauseModal.hide()
        winModal.hide()
        loseModal.hide()
        revealFlaggedModal.hide()
    }

    fun refreshUITextElements(cell: Cell?) {
        movesLeftTextView.text = getString(R.string.x_moves_left, game.movesLeft)
        planetsFoundTextView.text = getString(R.string.y_von_z_planeten_gefunden, game.planetsFound, game.planetAmount)

        if(game.state == Game.GameState.WON) {
            hideAllModals()
            winModal.show()
        }

        if(game.state == Game.GameState.LOST) {
            hideAllModals()
            loseModal.show()
        }
    }

    fun handleRevealFlaggedField(cell: Cell) {
        hideAllModals()
        revealFlaggedModal.show()
        findViewById<Button>(R.id.reveal_flagged_cell_yes_button).setOnClickListener {
            cell.flagged = false
            hideAllModals()
            game.onFieldClicked(cell)
        }
        findViewById<Button>(R.id.reveal_flagged_cell_no_button).setOnClickListener { hideAllModals() }
    }

    fun reset() {
        startActivity(
            createIntent(
                context = this,
                gridRows = game.gridRows,
                gridCols = game.gridCols,
                planetAmount = game.planetAmount,
                allowedMoves = game.allowedMoves
            )
        )
        finish()
    }

    fun exit(won: Boolean) {
        startActivity(Intent(this, MainActivity::class.java).apply {
            putExtra(EXTRA_WON, won)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        })
        finish()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyFullscreen()
    }
}
