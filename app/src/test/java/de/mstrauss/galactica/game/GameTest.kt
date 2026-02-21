package de.mstrauss.galactica.game

import android.content.Context
import android.view.ContextThemeWrapper
import de.mstrauss.galactica.R
import de.mstrauss.galactica.util.exceptions.CellChangeNotAllowedGameExceptions
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.function.ThrowingRunnable
import org.junit.runners.Parameterized
import org.robolectric.RuntimeEnvironment

class GameTest {

    lateinit var game : Game

    lateinit var context : Context

    @Before
    fun setup() {
        val app = RuntimeEnvironment.getApplication()
        app.setTheme(R.style.Theme_Galactica)
        context = ContextThemeWrapper(app, R.style.Theme_Galactica)
    }

    @Test
    fun testDefaultGame() {
        game = Game(context = context)
        Assert.assertEquals(7, game.gridRows)
        Assert.assertEquals(9, game.gridCols)
        Assert.assertEquals(false, game.flagMode)
        Assert.assertEquals(4, game.planetAmount)
        Assert.assertEquals(7, game.field.size)
        Assert.assertEquals(9, game.field[0].size)
    }

/*    @Test
    fun testOutOfBoundValues(row:Int, col:Int, planetAmount:Int) {
        Assert.assertThrows(
            IllegalArgumentException::class.java,
            ThrowingRunnable {
                Game(row, col, planetAmount, context)
            }
        )
    }*/
}
