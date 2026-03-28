package de.mstrauss.galactica.ui

import android.content.Context
import android.util.AttributeSet
import de.mstrauss.galactica.game.Game

class BombItemButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ItemButtonView(context, attrs, defStyleAttr) {

    override fun onItemUsed(game: Game) {
        // TODO: implement bomb item logic
    }
}
