package de.mstrauss.galactica.ui

import android.content.Context
import android.util.AttributeSet
import de.mstrauss.galactica.game.Game

class RocketshipItemButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ItemButtonView(context, attrs, defStyleAttr) {

    override fun onItemUsed(game: Game) {
        // TODO: implement rocketship item logic
        if(game.state != Game.GameState.RUNNING || game.rocketshipItemAmount < 1) return
        game.startRocketshipItem?.invoke(game)
        game.rocketshipItemAmount--
        game.onUIRefresh?.invoke(null)

    }
}
