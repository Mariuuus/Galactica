package de.mstrauss.galactica.ui

import android.content.Context
import android.util.AttributeSet
import de.mstrauss.galactica.game.Game
import de.mstrauss.galactica.game.MultiplayerGame

class BombItemButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ItemButtonView(context, attrs, defStyleAttr) {

    override fun onItemUsed(game: Game) {
        if(game.state != Game.GameState.RUNNING || game.bombItemLeft < 1) return
        if(game is MultiplayerGame && (game).multiplayerState != MultiplayerGame.MultiplayerState.MY_TURN) return
        game.startBombItem?.invoke(game)
        game.bombItemLeft--
        game.onUIRefresh?.invoke(null)
    }
}
