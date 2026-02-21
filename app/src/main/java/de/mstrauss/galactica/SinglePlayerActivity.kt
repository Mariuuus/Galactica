package de.mstrauss.galactica

import android.os.Bundle
import android.widget.GridLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import de.mstrauss.galactica.game.Cell
import de.mstrauss.galactica.game.Game

class SinglePlayerActivity : AppCompatActivity() {

    lateinit var game: Game

    lateinit var movesLeftTextView: TextView
    lateinit var planetsFoundTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ingame_single_player)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        game = Game(context=this, onUIRefresh = { refreshUITextElements(it) })

        val grid = findViewById<GridLayout>(R.id.single_player_grid)
        grid.rowCount = game.gridRows
        grid.columnCount = game.gridCols

        for (row in 0 until game.gridRows) {
            for (col in 0 until game.gridCols) {
                grid.addView(game.field[row][col])
            }
        }

        movesLeftTextView = findViewById<TextView>(R.id.moves_left_text_view)
        planetsFoundTextView = findViewById<TextView>(R.id.planets_founds_text_view)
    }


    fun refreshUITextElements(cell: Cell) {
        movesLeftTextView.text = getString(R.string.x_moves_left, game.movesLeft)
        planetsFoundTextView.text = getString(R.string.y_von_z_planeten_gefunden, game.planetsFound, game.planetAmount)
    }
}
