package de.mstrauss.galactica

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.GridLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import de.mstrauss.galactica.game.Cell
import de.mstrauss.galactica.game.Game
import de.mstrauss.galactica.game.GridLinesOverlayView
import de.mstrauss.galactica.game.MultiplayerGame
import de.mstrauss.galactica.multiplayer.BluetoothConnectionManager
import de.mstrauss.galactica.multiplayer.ConnectionLobbyPayload

import de.mstrauss.galactica.ui.applyFullscreen

class MultiPlayerActivityHost : AppCompatActivity() {

    companion object {

        private const val EXTRA_GRID_ROWS = "extra_grid_rows"
        private const val EXTRA_GRID_COLS = "extra_grid_cols"
        private const val EXTRA_PLANET_AMOUNT = "extra_planet_amount"
        private const val EXTRA_SEED = "extra_seed"

        private const val DEFAULT_GRID_ROWS = 7
        private const val DEFAULT_GRID_COLS = 9
        private const val DEFAULT_PLANET_AMOUNT = 4

        fun createIntent(
            context: Context,
            gridRows: Int,
            gridCols: Int,
            planetAmount: Int,
            randomSeed: Long
        ): Intent = Intent(context, MultiPlayerActivityHost::class.java).apply {
            putExtra(EXTRA_GRID_ROWS, gridRows)
            putExtra(EXTRA_GRID_COLS, gridCols)
            putExtra(EXTRA_PLANET_AMOUNT, planetAmount)
            putExtra(EXTRA_SEED, randomSeed)
        }
    }

    lateinit var game : MultiplayerGame

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_multi_player_host)

        applyFullscreen()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val gridRows = intent.getIntExtra(EXTRA_GRID_ROWS, DEFAULT_GRID_ROWS)
        val gridCols = intent.getIntExtra(EXTRA_GRID_COLS, DEFAULT_GRID_COLS)
        val planetAmount = intent.getIntExtra(EXTRA_PLANET_AMOUNT, DEFAULT_PLANET_AMOUNT)
        val randomSeed = intent.getLongExtra(EXTRA_SEED, 42)

        game = MultiplayerGame(
            gridRows,
            gridCols,
            planetAmount,
            this,
            gridRows*gridCols,
            {refreshUITextElements(it)},
            {},
            BluetoothConnectionManager.Role.HOST,
            randomSeed
        )

        val grid = findViewById<GridLayout>(R.id.multiplayer_grid)
        val gridOverlay = findViewById<GridLinesOverlayView>(R.id.multiplayer_grid_overlay)
        grid.rowCount = game.gridRows
        grid.columnCount = game.gridCols
        gridOverlay.setGridSize(game.gridRows, game.gridCols)

        for (row in 0 until game.gridRows) {
            for (col in 0 until game.gridCols) {
                grid.addView(game.field[row][col])
            }
        }

        BluetoothConnectionManager.addListener(game.bluetoothConnectionListener)

        val payload = ConnectionLobbyPayload(timestamp = randomSeed, rows = gridRows, cols= gridCols, planets = planetAmount, start = true)
        val sent = BluetoothConnectionManager.send(payload.encode())
        if(!sent) {
            //TODO: return to lobby or smth
        }

        refreshUITextElements(null)
    }
    fun refreshUITextElements(cell: Cell?) {
        //TODO: update text

        if(game.state == Game.GameState.WON) {
            //TODO: won
        }

        if(game.state == Game.GameState.LOST) {
            //TODO: lost
        }
    }
}

