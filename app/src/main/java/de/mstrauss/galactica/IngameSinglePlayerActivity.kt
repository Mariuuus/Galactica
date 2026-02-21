package de.mstrauss.galactica

import android.os.Bundle
import android.view.View
import android.widget.GridLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import de.mstrauss.galactica.game.Field

class IngameSinglePlayerActivity : AppCompatActivity() {

    private val gridRows = 8
    private val gridCols = 7
    private lateinit var fields: Array<Array<Field>>

    private val flagMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ingame_single_player)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val grid = findViewById<GridLayout>(R.id.single_player_grid)
        grid.rowCount = gridRows
        grid.columnCount = gridCols

        fields = Array(gridRows) { row ->
            Array(gridCols) { col ->
                Field(this).apply {
                    id = View.generateViewId()
                    posX = col
                    posY = row
                    text = ""
                    onFieldClick = { field -> onFieldClicked(field) }
                }
            }
        }

        for (row in 0 until gridRows) {
            for (col in 0 until gridCols) {
                grid.addView(fields[row][col])
            }
        }
    }

    private fun onFieldClicked(field: Field) {
        // TODO: handle field click
        if(!flagMode) {
            field.revealed = true;
            field.invalidate()
        }
    }
}
