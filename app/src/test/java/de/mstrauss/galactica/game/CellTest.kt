package de.mstrauss.galactica.game

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import android.view.ContextThemeWrapper
import de.mstrauss.galactica.R
import de.mstrauss.galactica.util.exceptions.CellChangeNotAllowedGameExceptions
import org.junit.function.ThrowingRunnable
import org.mockito.Mockito
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CellTest {

    private lateinit var cell: Cell

    @Before
    fun setup() {
        val app = RuntimeEnvironment.getApplication()
        app.setTheme(R.style.Theme_Galactica)
        val themedContext = ContextThemeWrapper(app, R.style.Theme_Galactica)
        cell = Cell(context = themedContext)
    }


    @Test fun testDefaultCell() {
        Assert.assertEquals(Cell.CellType.HINT, cell.cellType)
        Assert.assertEquals(0, cell.hintNumber)
        Assert.assertEquals(false, cell.flagged)
        Assert.assertEquals(false, cell.isPlanet())
        Assert.assertEquals(false, cell.revealed)
    }

    @Test fun testFlaggingUnrevealed() {
        Assert.assertEquals(false, cell.flagged)

        cell.flagged = true

        Assert.assertEquals(true, cell.flagged)
    }

    @Test fun testFlaggingRevealed() {
        cell.revealed = true
        Assert.assertEquals(false, cell.flagged)
        Assert.assertThrows(
            CellChangeNotAllowedGameExceptions::class.java,
            ThrowingRunnable { cell.flagged = true }
        )
    }

    @Test fun testRevealingUnflagged() {
        cell.revealed = true
        Assert.assertEquals(true, cell.revealed)
    }


    @Test fun testRevealingFlagged() {
        cell.flagged = true

        Assert.assertThrows(
            CellChangeNotAllowedGameExceptions::class.java,
            ThrowingRunnable { cell.revealed = true }
        )
    }

    @Test
    fun testOnCellClickInvoked() {
        @Suppress("UNCHECKED_CAST")
        val callback = Mockito.mock(Function1::class.java) as (Cell) -> Unit

        cell.onCellClick = callback

        cell.performClick()

        Mockito.verify(callback, Mockito.times(1)).invoke(cell)
    }

/*    @Test
    fun testCallbackCallOnReveal() {
        @Suppress("UNCHECKED_CAST")
        val callback = Mockito.mock(Function1::class.java) as (Cell) -> Unit

        cell.onCellClick = callback

        cell.revealed = true

        Mockito.verify(callback, Mockito.times(1)).invoke(cell)
    }*/
}
