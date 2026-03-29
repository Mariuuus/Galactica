package de.mstrauss.galactica

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.GridLayout
import android.widget.Switch
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import de.mstrauss.galactica.game.Cell
import de.mstrauss.galactica.game.Game
import de.mstrauss.galactica.game.GridLinesOverlayView
import de.mstrauss.galactica.game.MultiplayerGame
import de.mstrauss.galactica.multiplayer.BluetoothConnectionManager
import de.mstrauss.galactica.multiplayer.ConnectionIngamePayload
import de.mstrauss.galactica.multiplayer.ConnectionIngamePayload.Type
import de.mstrauss.galactica.multiplayer.ConnectionLobbyPayload
import de.mstrauss.galactica.multiplayer.ConnectionPayload
import de.mstrauss.galactica.ui.BombItemButtonView
import de.mstrauss.galactica.ui.BombItemView
import de.mstrauss.galactica.ui.IngameModalView
import de.mstrauss.galactica.ui.RocketshipItemButtonView
import de.mstrauss.galactica.ui.RocketshipItemView
import de.mstrauss.galactica.ui.applyFullscreen

class MultiPlayerActivity : AppCompatActivity() {

    companion object {

        private const val EXTRA_GRID_ROWS = "extra_grid_rows"
        private const val EXTRA_GRID_COLS = "extra_grid_cols"
        private const val EXTRA_PLANET_AMOUNT = "extra_planet_amount"
        private const val EXTRA_BOMB_ITEM_AMOUNT = "extra_bombs_amount"
        private const val EXTRA_ROCKETSHIP_ITEM_AMOUNT = "extra_rocketship_amount"
        private const val EXTRA_SEED = "extra_seed"
        private const val EXTRA_ROLE = "extra_role"

        private const val DEFAULT_GRID_ROWS = 7
        private const val DEFAULT_GRID_COLS = 9
        private const val DEFAULT_PLANET_AMOUNT = 4

        private const val DEFAULT_BOMB_ITEM_AMOUNT = 0
        private const val DEFAULT_ROCKETSHIP_ITEM_AMOUNT = 0

        fun createIntent(
            context: Context,
            gridRows: Int,
            gridCols: Int,
            planetAmount: Int,
            bombAmount: Int = DEFAULT_BOMB_ITEM_AMOUNT,
            rocketshipAmount: Int = DEFAULT_ROCKETSHIP_ITEM_AMOUNT,
            randomSeed: Long,
            role: BluetoothConnectionManager.Role
        ): Intent = Intent(context, MultiPlayerActivity::class.java).apply {
            putExtra(EXTRA_GRID_ROWS, gridRows)
            putExtra(EXTRA_GRID_COLS, gridCols)
            putExtra(EXTRA_PLANET_AMOUNT, planetAmount)
            putExtra(EXTRA_SEED, randomSeed)
            putExtra(EXTRA_BOMB_ITEM_AMOUNT, bombAmount)
            putExtra(EXTRA_ROCKETSHIP_ITEM_AMOUNT, rocketshipAmount)
            putExtra(EXTRA_ROLE, role.toString())
        }
    }

    lateinit var roleTextView: TextView
    lateinit var gameStatusTextView: TextView
    lateinit var myPlanetsTextView: TextView
    lateinit var enemyPlanetsTextView: TextView

    lateinit var game: MultiplayerGame

    lateinit var winModal: IngameModalView
    lateinit var loseModal: IngameModalView
    lateinit var pauseModal: IngameModalView
    lateinit var revealFlaggedModal: IngameModalView

    lateinit var bombItem: BombItemButtonView
    lateinit var rocketshipItem: RocketshipItemButtonView

    private var winNotificationSent = false


    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_multi_player)

        applyFullscreen()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // extra params
        val gridRows = intent.getIntExtra(EXTRA_GRID_ROWS, DEFAULT_GRID_ROWS)
        val gridCols = intent.getIntExtra(EXTRA_GRID_COLS, DEFAULT_GRID_COLS)
        val planetAmount = intent.getIntExtra(EXTRA_PLANET_AMOUNT, DEFAULT_PLANET_AMOUNT)
        val randomSeed = intent.getLongExtra(EXTRA_SEED, 42)
        val bombAmount = intent.getIntExtra(EXTRA_BOMB_ITEM_AMOUNT, DEFAULT_BOMB_ITEM_AMOUNT)
        val rocketshipAmount = intent.getIntExtra(EXTRA_ROCKETSHIP_ITEM_AMOUNT, DEFAULT_ROCKETSHIP_ITEM_AMOUNT)
        val role: BluetoothConnectionManager.Role =
            if (intent.getStringExtra(EXTRA_ROLE) != null) BluetoothConnectionManager.Role.valueOf(
                intent.getStringExtra(EXTRA_ROLE)!!
            ) else BluetoothConnectionManager.Role.CLIENT

        Log.d(this::class.toString(), "Your role is $role")

        // create game
        game = MultiplayerGame(
            gridRows,
            gridCols,
            planetAmount,
            bombAmount,
            rocketshipAmount,
            this,
            gridRows * gridCols,
            { refreshUITextElements(it) },
            {handleRevealFlaggedField(it)},
            startBombItem = { findViewById<BombItemView>(R.id.bomb_container).start(game) },
            startRocketshipItem = { findViewById<RocketshipItemView>(R.id.rocketship_container).start(game) },
            role,
            randomSeed
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

        // setup grid view
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

        // bluetooth listener
        Log.d(this::class.toString(), "Adding Listener")
        BluetoothConnectionManager.addListener(game.bluetoothConnectionListener)

        // complete handshake, depending on role
        val payload: ConnectionPayload = (if (role == BluetoothConnectionManager.Role.CLIENT)
            ConnectionIngamePayload(timestamp = randomSeed, type = Type.JOINED, 0)
        else ConnectionLobbyPayload(
            timestamp = randomSeed,
            rows = gridRows,
            cols = gridCols,
            planets = planetAmount,
            bombs = bombAmount,
            rocketShips = rocketshipAmount,
            start = true
        ))

        val sent = BluetoothConnectionManager.send(payload.encode())
        if (!sent) {
            //TODO: return to lobby or smth
        }

        roleTextView = findViewById(R.id.role_text)
        gameStatusTextView = findViewById(R.id.game_state)
        myPlanetsTextView = findViewById(R.id.my_planets)
        enemyPlanetsTextView = findViewById(R.id.enemies_planets)

        val flaggingModeToggle = findViewById<MaterialButton>(R.id.multiplayer_flagging_toggle)

        flaggingModeToggle.addOnCheckedChangeListener { button, isChecked ->
            button.icon = getDrawable(
                if (isChecked) R.drawable._icon_flagtriangleright_crossed
                else R.drawable._icon_flagtriangleright
            )
            game.flagMode = isChecked
        }

        winModal = findViewById(R.id.win_modal)
        loseModal = findViewById(R.id.lose_modal)
        pauseModal = findViewById(R.id.pause_modal)
        revealFlaggedModal = findViewById(R.id.reveal_flagged_cell_modal)

        findViewById<Button>(R.id.pause_button).setOnClickListener { pauseModal.show() }
        findViewById<Button>(R.id.resume_button).setOnClickListener { pauseModal.hide() }
        findViewById<Button>(R.id.multi_won_end_button).setOnClickListener { BluetoothConnectionManager.disconnect() }
        findViewById<Button>(R.id.multi_lost_end_button).setOnClickListener { BluetoothConnectionManager.disconnect() }
        findViewById<Button>(R.id.multi_pause_end_button).setOnClickListener { BluetoothConnectionManager.disconnect() }

        hideAllModals()
        refreshUITextElements(null)
    }

    fun refreshUITextElements(cell: Cell?) {
        runOnUiThread {
            roleTextView.text =
                if (game.role == BluetoothConnectionManager.Role.HOST) getString(R.string.host) else getString(
                    R.string.client
                )
            myPlanetsTextView.text = getString(
                R.string.multiplayer_my_planets_found,
                game.planetsFound,
                game.planetAmount
            )
            enemyPlanetsTextView.text = getString(
                R.string.multiplayer_enemy_planets_found,
                game.enemyPlanetsFound,
                game.planetAmount
            )

            when (game.multiplayerState) {
                MultiplayerGame.MultiplayerState.MY_TURN -> {
                    gameStatusTextView.text = getString(R.string.my_turn)
                    if (!game.flagMode && cell?.isPlanet() ?: false)
                        gameStatusTextView.text = getString(R.string.my_turn_again)
                }

                MultiplayerGame.MultiplayerState.WAITING_FOR_TURN -> {
                    gameStatusTextView.text = getString(R.string.not_my_turn)
                }
            }

            bombItem.itemCount = game.bombItemLeft
            rocketshipItem.itemCount = game.rocketShipItemLeft

            //TODO: update text
            if (game.state == Game.GameState.WON) {
                hideAllModals()
                winModal.show()

                if (!winNotificationSent) {
                    winNotificationSent = true
                    sendWinNotification()
                }

                if (game.role == BluetoothConnectionManager.Role.HOST) {
                    findViewById<Button>(R.id.multi_won_reset_button).visibility = Button.VISIBLE;
                    findViewById<Button>(R.id.multi_won_reset_button).setOnClickListener {
                        replay()
                    }
                } else {
                    findViewById<Button>(R.id.multi_won_reset_button).visibility = Button.GONE;
                }
            }
            if (game.state == Game.GameState.LOST) {
                hideAllModals()
                loseModal.show()

                if (game.role == BluetoothConnectionManager.Role.HOST) {
                    findViewById<Button>(R.id.multi_lost_reset_button).visibility = Button.VISIBLE;
                    findViewById<Button>(R.id.multi_lost_reset_button).setOnClickListener {
                        replay()
                    }
                } else {
                    findViewById<Button>(R.id.multi_lost_reset_button).visibility = Button.GONE;
                }
            }
        }
    }

    fun newGame() {
        this.startActivity(
            createIntent(
                context = this,
                gridRows = game.gridRows,
                gridCols = game.gridCols,
                planetAmount = game.planetAmount,
                bombAmount = game.bombItemAmount,
                rocketshipAmount = game.rocketshipItemAmount,
                randomSeed = game.randomSeed+1,
                role = game.role
            )
        )
        this.finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        BluetoothConnectionManager.removeListener(this.game.bluetoothConnectionListener)
    }

    private fun replay() {
        if(game.role.equals(BluetoothConnectionManager.Role.HOST)) {
            sendReplayRequest()
            newGame()
        }
    }

    private fun sendWinNotification() {
        val payload: ConnectionPayload = ConnectionIngamePayload(System.currentTimeMillis(), type = Type.WON, planetsFound = game.planetsFound)

        val sent = BluetoothConnectionManager.send(payload.encode())
        if (!sent) {
            //TODO: return to lobby or smth
        }
    }

    private fun sendReplayRequest() {
        val payload: ConnectionPayload = ConnectionIngamePayload(System.currentTimeMillis(), type = Type.REPLAY, planetsFound = 0)

        val sent = BluetoothConnectionManager.send(payload.encode())
        if (!sent) {
            //TODO: return to lobby or smth
        }
    }

    private fun hideAllModals() {
        pauseModal.hide()
        winModal.hide()
        loseModal.hide()
        revealFlaggedModal.hide()
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

    override fun onDetachedFromWindow() {
        BluetoothConnectionManager.removeListener(game.bluetoothConnectionListener)
        super.onDetachedFromWindow()
    }
}
