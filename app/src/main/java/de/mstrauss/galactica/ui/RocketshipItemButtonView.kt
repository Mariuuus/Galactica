package de.mstrauss.galactica.ui

import android.content.Context
import android.util.AttributeSet
import de.mstrauss.galactica.game.Game
import de.mstrauss.galactica.game.MultiplayerGame

class RocketshipItemButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ItemButtonView(context, attrs, defStyleAttr) {

    override fun onItemUsed(game: Game) {
        // TODO: implement rocketship item logic
        if(game.state != Game.GameState.RUNNING || game.rocketShipItemLeft < 1) return
        if(game is MultiplayerGame && (game).multiplayerState != MultiplayerGame.MultiplayerState.MY_TURN) return
        game.startRocketshipItem?.invoke(game)
        game.rocketShipItemLeft--
        game.onUIRefresh?.invoke(null)

    }
}
