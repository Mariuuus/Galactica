package de.mstrauss.galactica.game

import android.content.Context
import android.view.ContextThemeWrapper
import de.mstrauss.galactica.R
import org.junit.Before
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.intArrayOf

class GameTest {
    data class InvalidGameConfig(
        val rows: Int,
        val cols: Int,
        val planets: Int,
        val allowedMoves: Int
    )

    @Test
    fun defaultConfigurationIsValid() {
        assertDoesNotThrow {
            Game.validateConfiguration(7, 9, 4, 7*9)
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidConfigurations")
    fun invalidConfigurationsThrow(config: InvalidGameConfig) {
        assertThrows(IllegalArgumentException::class.java) {
            Game.validateConfiguration(config.rows, config.cols, config.planets, config.allowedMoves)
        }
    }

    companion object {
        @JvmStatic
        fun invalidConfigurations(): List<InvalidGameConfig> = listOf(
            // too many planets
            InvalidGameConfig(1, 1, 2, 2),
            InvalidGameConfig(2, 2, 5, 5),
            InvalidGameConfig(3, 3, 10, 10),
            InvalidGameConfig(7, 9, 64, 64),

            // negative values
            InvalidGameConfig(-1, 1, 1,1),
            InvalidGameConfig(1, -1, 1,1),
            InvalidGameConfig(1, 1, -1,1),
            InvalidGameConfig(1, 1, 1,-1),

            // zeros in any arg
            InvalidGameConfig(0, 1, 1,1),
            InvalidGameConfig(1, 0, 1,1),
            InvalidGameConfig(1, 1, 0,1),
            InvalidGameConfig(1, 1, 1,0),

            //exactly the same planetAmount
            InvalidGameConfig(2, 2, 4,4),

            //moves left out of bounds
            InvalidGameConfig(2, 2, 2,5),
            InvalidGameConfig(2, 2, 2,1),
        )
    }
}


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GameInstrumentedTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        val app = RuntimeEnvironment.getApplication()
        app.setTheme(R.style.Theme_Galactica)
        context = ContextThemeWrapper(app, R.style.Theme_Galactica)
    }

    @org.junit.Test
    fun testPlanetDuplicateCoordinateBruteForce() {
        val game = Game(2,2,3, 0,0,context)
        (0..100).forEach { _ ->
            var planetCount = 0
            for (y in 0..1){
                for (x in 0..1) {
                    if (game.field[y][x].isPlanet()) planetCount++;
                }
            }
            Assertions.assertEquals(3, planetCount)
        }

    }


    @org.junit.Test
    fun testHintCalculation() {
        //val game = Game()
    }
}
