package de.mstrauss.galactica.game

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class GameTest {
    data class InvalidGameConfig(
        val rows: Int,
        val cols: Int,
        val planets: Int,
    )

    @Test
    fun defaultConfigurationIsValid() {
        assertDoesNotThrow {
            Game.validateConfiguration(7, 9, 4)
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidConfigurations")
    fun invalidConfigurationsThrow(config: InvalidGameConfig) {
        assertThrows(IllegalArgumentException::class.java) {
            Game.validateConfiguration(config.rows, config.cols, config.planets)
        }
    }

    companion object {
        @JvmStatic
        fun invalidConfigurations(): List<InvalidGameConfig> = listOf(
            // too many planets
            InvalidGameConfig(1, 1, 2),
            InvalidGameConfig(2, 2, 5),
            InvalidGameConfig(3, 3, 10),
            InvalidGameConfig(7, 9, 64),

            // negative values
            InvalidGameConfig(-1, 1, 1),
            InvalidGameConfig(1, -1, 1),
            InvalidGameConfig(1, 1, -1),

            // zeros in any arg
            InvalidGameConfig(0, 1, 1),
            InvalidGameConfig(1, 0, 1),
            InvalidGameConfig(1, 1, 0),

            //exactly the same planetAmount
            InvalidGameConfig(2, 2, 4),
        )
    }
}
