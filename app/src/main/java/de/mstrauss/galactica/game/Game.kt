package de.mstrauss.galactica.game

import android.content.Context
import android.view.View
import kotlin.random.Random

class Game(val gridRows: Int=7, val gridCols: Int=9, val planetAmount: Int=4, val context: Context, val allowedMoves: Int=gridRows*gridCols) {
    class Coordinate(val x: Int, val y: Int)
    companion object {
        internal fun validateConfiguration(gridRows: Int, gridCols: Int, planetAmount: Int, allowedMoves: Int) {
            require(planetAmount <= gridCols * gridRows -1) {
                "More planets than available grid positions! (or equal!)"
            }
            require(gridRows > 0 ) {
                "Rows must be greater than 0!"
            }
            require(gridCols > 0 ) {
                "cols must be greater than 0!"
            }
            require(planetAmount > 0 ) {
                "planets must be greater than 0!"
            }
            require(allowedMoves in planetAmount..(gridCols*gridRows)) {
                "allowedMoves is out of Bounds!"
            }
        }
    }

    var field: Array<Array<Cell>>

    operator fun Array<Array<Cell>>.get(c: Coordinate): Cell =
        this[c.y][c.x]

    operator fun Array<Array<Cell>>.set(c: Coordinate, value: Cell) {
        this[c.y][c.x] = value
    }
    val flagMode = false

    init {
        validateConfiguration(gridRows, gridCols, planetAmount, allowedMoves)

        field = Array(gridRows) { row ->
            Array(gridCols) { col ->
                Cell(context).apply {
                    id = View.generateViewId()
                    posX = col
                    posY = row
                    text = ""
                    onCellClick = { field -> onFieldClicked(field) }
                }
            }
        }
        val planets = generatePlanets()
        generateHints(planets)
    }

    private fun generateHints(planets : List<Coordinate>) {

        fun inBounds(c: Coordinate): Boolean =
            c.y in 0 until field.size && c.x in 0 until field[0].size

        val directions = listOf(
            0 to -1,  // up
            0 to  1,  // down
            -1 to  0,  // left
            1 to  0,  // right
            -1 to -1,  // up-left
            1 to -1,  // up-right
            -1 to  1,  // down-left
            1 to  1   // down-right
        )

        for (planet in planets) {
            for ((dx, dy) in directions) {
                var x = planet.x + dx
                var y = planet.y + dy

                while (true) {
                    val c = Coordinate(x, y)
                    if (!inBounds(c)) break
                    if (field[c].isPlanet()) break

                    field[c].hintNumber++

                    x += dx
                    y += dy
                }
            }
        }
    }

    private fun generatePlanets(): List<Coordinate> {
        val r = Random.Default
        val set = mutableSetOf<Coordinate>()

        while (set.size < planetAmount) {
            set += Coordinate(
                r.nextInt(gridCols),
                r.nextInt(gridRows)
            )
        }

        for (planet in set) {
            field[planet].cellType = Cell.CellType.PLANET
        }

        return set.toList()
    }

    private fun onFieldClicked(field: Cell) {
        if(!flagMode) {
            field.revealed = true
        } else {
            field.flagged = true
        }
    }
}
