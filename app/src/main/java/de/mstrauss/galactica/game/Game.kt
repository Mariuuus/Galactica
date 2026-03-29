package de.mstrauss.galactica.game

import android.content.Context
import android.view.View
import kotlin.random.Random

open class Game(
    val gridRows: Int =7,
    val gridCols: Int =9,
    val planetAmount: Int =4,
    val bombItemAmount: Int =1,
    val rocketshipItemAmount: Int =1,
    val context: Context,
    val allowedMoves: Int =gridRows*gridCols,
    val onUIRefresh: ((Cell?) -> Unit)? = null,
    val handleRevealFlaggedField: ((Cell) -> Unit)? = null,
    val startBombItem: ((Game) -> Unit)? = null,
    val startRocketshipItem: ((Game) -> Unit)? = null,
    randomSeed: Long? =null) {
    data class Coordinate(val x: Int, val y: Int)

    enum class GameState {
        RUNNING, BLOCKED, WON, LOST
    }

    var random = randomSeed?.let { Random(it) } ?: Random.Default

    companion object {
        internal fun validateConfiguration(gridRows: Int, gridCols: Int, planetAmount: Int, allowedMoves: Int) {
            println("planetAmount=$planetAmount grid=${gridCols*gridRows} allowedMoves=$allowedMoves")
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


    var movesLeft = allowedMoves
    var bombItemLeft = bombItemAmount
    var rocketShipItemLeft = rocketshipItemAmount
    var planetsFound = 0


    var field: Array<Array<Cell>>

    operator fun Array<Array<Cell>>.get(c: Coordinate): Cell =
        this[c.y][c.x]

    operator fun Array<Array<Cell>>.set(c: Coordinate, value: Cell) {
        this[c.y][c.x] = value
    }
    var flagMode = false

    var state = GameState.RUNNING
        internal set

    var onBlockedCellClick: ((Cell) -> Unit)? = null

    init {

        validateConfiguration(gridRows, gridCols, planetAmount, allowedMoves)

        field = Array(gridRows) { row ->
            Array(gridCols) { col ->
                Cell(context).apply {
                    id = View.generateViewId()
                    posX = col
                    posY = row
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
        val set = mutableSetOf<Coordinate>()

        while (set.size < planetAmount) {
            set += Coordinate(
                random.nextInt(gridCols),
                random.nextInt(gridRows)
            )
        }

        for (planet in set) {
            field[planet].cellType = Cell.CellType.PLANET
        }

        return set.toList()
    }

    open fun onFieldClicked(field: Cell) {
        if(state != GameState.RUNNING) {
            if(state == GameState.BLOCKED) onBlockedCellClick?.invoke(field)
            return
        }
        if(!flagMode) {
            if(!field.revealed) {
                if(field.flagged) {
                    handleRevealFlaggedField?.invoke(field)
                    return
                }
                field.revealed = true
                movesLeft--
                if(field.isPlanet()) planetsFound++
            }
        } else if (!field.revealed) {
            field.flagged = !field.flagged
        }

        if(movesLeft <= 0 && planetAmount != planetsFound) state = GameState.LOST
        if(planetAmount == planetsFound) state = GameState.WON
        onUIRefresh?.invoke(field)
    }


    open fun useRocketship(positions: List<Pair<Int, Int>>) {
        if(state != GameState.BLOCKED) return
        positions.forEach { (row, col) ->
            val cell = field[row][col]
            if(!cell.flagged && !cell.revealed){
                cell.revealed = true
                if(cell.isPlanet()) planetsFound++
                onUIRefresh?.invoke(cell)
            }
            cell.dehighlight()
        }
        movesLeft--

        state = GameState.RUNNING

        if(movesLeft <= 0 && planetAmount != planetsFound) state = GameState.LOST
        if(planetAmount == planetsFound) state = GameState.WON
        onUIRefresh?.invoke(null)
    }

    open fun useBomb(positions : List<Pair<Int, Int>>) {
        if(state != GameState.BLOCKED) return
        positions.forEach { (row, col) ->
            var cell = field[row][col]
            if(!cell.flagged && !cell.revealed){
                cell.revealed = true
                if(cell.isPlanet()) planetsFound++
                onUIRefresh?.invoke(cell)
            }
            cell.dehighlight()
        }
        movesLeft--

        state = GameState.RUNNING

        if(movesLeft <= 0 && planetAmount != planetsFound) state = GameState.LOST
        if(planetAmount == planetsFound) state = GameState.WON
        onUIRefresh?.invoke(null)
    }}
