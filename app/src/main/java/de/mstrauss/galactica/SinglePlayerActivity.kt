package de.mstrauss.galactica

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import de.mstrauss.galactica.game.Cell
import de.mstrauss.galactica.game.Game
import de.mstrauss.galactica.game.GridLinesOverlayView
import de.mstrauss.galactica.ui.BombItemButtonView
import de.mstrauss.galactica.ui.BombItemView
import de.mstrauss.galactica.ui.RocketshipItemView
import de.mstrauss.galactica.ui.IngameModalView
import de.mstrauss.galactica.ui.RocketshipItemButtonView
import de.mstrauss.galactica.ui.applyFullscreen

class SinglePlayerActivity : AppCompatActivity() {
    companion object {
        private const val EXTRA_GRID_ROWS = "extra_grid_rows"
        private const val EXTRA_GRID_COLS = "extra_grid_cols"
        private const val EXTRA_PLANET_AMOUNT = "extra_planet_amount"
        private const val EXTRA_BOMB_ITEM_AMOUNT = "extra_bombs_amount"
        private const val EXTRA_ROCKETSHIP_ITEM_AMOUNT = "extra_rocketship_amount"
        private const val EXTRA_ALLOWED_MOVES = "extra_allowed_moves"
        private const val EXTRA_RANDOM_SEED = "extra_random_seed"
        private const val EXTRA_WON = "extra_won"

        private const val DEFAULT_GRID_ROWS = 7
        private const val DEFAULT_GRID_COLS = 9
        private const val DEFAULT_PLANET_AMOUNT = 4
        private const val DEFAULT_BOMB_ITEM_AMOUNT = 0
        private const val DEFAULT_ROCKETSHIP_ITEM_AMOUNT = 0

        fun createIntent(
            context: Context,
            gridRows: Int = DEFAULT_GRID_ROWS,
            gridCols: Int = DEFAULT_GRID_COLS,
            planetAmount: Int = DEFAULT_PLANET_AMOUNT,
            bombAmount: Int = DEFAULT_BOMB_ITEM_AMOUNT,
            rocketshipAmount: Int = DEFAULT_ROCKETSHIP_ITEM_AMOUNT,
            allowedMoves: Int = gridRows * gridCols,
            randomSeed: Long? = null
        ): Intent = Intent(context, SinglePlayerActivity::class.java).apply {
            putExtra(EXTRA_GRID_ROWS, gridRows)
            putExtra(EXTRA_GRID_COLS, gridCols)
            putExtra(EXTRA_PLANET_AMOUNT, planetAmount)
            putExtra(EXTRA_ALLOWED_MOVES, allowedMoves)
            putExtra(EXTRA_BOMB_ITEM_AMOUNT, bombAmount)
            putExtra(EXTRA_ROCKETSHIP_ITEM_AMOUNT, rocketshipAmount)
            if (randomSeed != null) putExtra(EXTRA_RANDOM_SEED, randomSeed)
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

    lateinit var bombItem: BombItemButtonView
    lateinit var rocketshipItem: RocketshipItemButtonView


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

        var gridRows = intent.getIntExtra(EXTRA_GRID_ROWS, DEFAULT_GRID_ROWS)
        var gridCols = intent.getIntExtra(EXTRA_GRID_COLS, DEFAULT_GRID_COLS)
        val planetAmount = intent.getIntExtra(EXTRA_PLANET_AMOUNT, DEFAULT_PLANET_AMOUNT)
        val bombAmount = intent.getIntExtra(EXTRA_BOMB_ITEM_AMOUNT, DEFAULT_BOMB_ITEM_AMOUNT)
        val rocketshipAmount = intent.getIntExtra(EXTRA_ROCKETSHIP_ITEM_AMOUNT, DEFAULT_ROCKETSHIP_ITEM_AMOUNT)
        val allowedMoves = intent.getIntExtra(EXTRA_ALLOWED_MOVES, gridRows * gridCols)
        val randomSeed = if (intent.hasExtra(EXTRA_RANDOM_SEED)) intent.getLongExtra(EXTRA_RANDOM_SEED, 0L) else null

        game = Game(
            gridRows = gridRows,
            gridCols = gridCols,
            planetAmount = planetAmount,
            allowedMoves = allowedMoves,
            bombItemAmount = bombAmount,
            rocketshipItemAmount = rocketshipAmount,
            context = this,
            onUIRefresh = { refreshUITextElements(it) },
            handleRevealFlaggedField = { handleRevealFlaggedField(it) },
            startBombItem = { findViewById<BombItemView>(R.id.bomb_container).start(game) },
            startRocketshipItem = { findViewById<RocketshipItemView>(R.id.rocketship_container).start(game) },
            randomSeed = randomSeed
        )

        bombItem = findViewById<BombItemButtonView>(R.id.item_button_bomb)
        rocketshipItem = findViewById<RocketshipItemButtonView>(R.id.item_button_rocketship)
        bombItem.game = game
        rocketshipItem.game = game

        findViewById<BombItemView>(R.id.bomb_container).apply {
            this.gridRows = game.gridRows
            this.gridCols = game.gridCols
            this.gridPaddingPx = (10 * resources.displayMetrics.density)
            onNearestCellsChanged = { cells ->
                for (row in game.field) row.forEach { it.dehighlight() }
                cells.forEach { (row, col) -> game.field[row][col].highlight() }
            }
        }

        findViewById<RocketshipItemView>(R.id.rocketship_container).apply {
            this.gridRows = game.gridRows
            this.gridCols = game.gridCols
            this.gridPaddingPx = (10 * resources.displayMetrics.density)
            onHighlightCellsChanged = { cells ->
                for (row in game.field) row.forEach { it.dehighlight() }
                cells.forEach { (row, col) -> game.field[row][col].highlight() }
            }
            onActivated = { g, cells ->
                g.useRocketship(cells)
            }
        }

        game.onBlockedCellClick = { cell ->
            findViewById<RocketshipItemView>(R.id.rocketship_container).selectCell(cell.posY, cell.posX)
        }

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

        pauseButton.setOnClickListener {
            if(game.state == Game.GameState.WON || game.state == Game.GameState.LOST)  {
                refreshUITextElements(null)
            } else {
                pauseModal.show()
            }
        }
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

        for (btn in listOf<Button>(
            findViewById(R.id.single_lost_view_button),
            findViewById(R.id.single_won_view_button),
        )) {
            btn.setOnClickListener { hideAllModals() }
        }

        val flaggingModeToggle = findViewById<MaterialButton>(R.id.single_flagging_toggle)

        flaggingModeToggle.addOnCheckedChangeListener { button, isChecked ->
            if(game.state == Game.GameState.RUNNING) {
                button.icon = getDrawable(
                    if (isChecked) R.drawable._icon_flagtriangleright_crossed
                    else R.drawable._icon_flagtriangleright
                )
                game.flagMode = isChecked
            }
        }


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
            (findViewById<TextView>(R.id.win_modal_description)).text = getString(R.string.single_win_description, (game.allowedMoves - game.movesLeft))
            winModal.show()
        }

        if(game.state == Game.GameState.LOST) {
            hideAllModals()
            loseModal.show()
        }

        bombItem.itemCount = game.bombItemLeft
        rocketshipItem.itemCount = game.rocketShipItemLeft

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
                bombAmount = game.bombItemAmount,
                rocketshipAmount = game.rocketshipItemAmount,
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
