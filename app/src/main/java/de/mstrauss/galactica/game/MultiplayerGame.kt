package de.mstrauss.galactica.game

import android.content.Context
import android.util.Log
import android.widget.Toast
import de.mstrauss.galactica.multiplayer.BluetoothConnectionManager
import de.mstrauss.galactica.multiplayer.ConnectionIngamePayload
import kotlin.random.Random

class MultiplayerGame(
    gridRows: Int = 7,
    gridCols: Int = 9,
    planetAmount: Int = 4,
    context: Context,
    allowedMoves: Int = gridRows * gridCols,
    onUIRefresh: ((Cell?) -> Unit)? = null,
    handleRevealFlaggedField: ((Cell) -> Unit)? = null,
    role: BluetoothConnectionManager.Role,
    randomSeed: Long
) : Game(
    gridRows,
    gridCols,
    planetAmount,
    context,
    allowedMoves,
    onUIRefresh,
    handleRevealFlaggedField,
    randomSeed
) {

    enum class MultiplayerState {
        MY_TURN, WAITING_FOR_TURN
    }

    var multiplayerState = MultiplayerState.WAITING_FOR_TURN
        private set(value) {
            field = value
            //TODO: allow clicks or so/ update UI
        }

    val bluetoothConnectionListener = object : BluetoothConnectionManager.Listener {
        override fun onConnected(role: BluetoothConnectionManager.Role) {
        }

        override fun onDisconnected() {
        }

        override fun onMessageReceived(message: String) {
            val payload = ConnectionIngamePayload.decode(message) ?: return
            Log.d(this::class.toString(), "received: $payload")
            if(payload.type == ConnectionIngamePayload.Type.JOINED && role == BluetoothConnectionManager.Role.HOST) {
                multiplayerState = MultiplayerState.MY_TURN
            }
            if(payload.type == ConnectionIngamePayload.Type.NEXT_TURN) {
                multiplayerState = MultiplayerState.MY_TURN
            }
            if(payload.type == ConnectionIngamePayload.Type.WON) {
                multiplayerState = MultiplayerState.WAITING_FOR_TURN
            }
        }

        override fun onError(message: String) {

        }
    }

    override fun onFieldClicked(field: Cell) {
        if(multiplayerState == MultiplayerState.MY_TURN || flagMode) {
            if(!field.revealed) {
                super.onFieldClicked(field)
                if(!flagMode && !field.isPlanet()) completeRound();
            }
        }
    }

    private fun completeRound() {
        multiplayerState = MultiplayerState.WAITING_FOR_TURN
        val payload = ConnectionIngamePayload(timestamp = System.currentTimeMillis(), type = ConnectionIngamePayload.Type.NEXT_TURN, planetsFound=planetsFound)
        val sent = BluetoothConnectionManager.send(payload.encode())
        if (!sent) {
            Toast.makeText(context, "No active Bluetooth connection.", Toast.LENGTH_SHORT).show()
            return
        }
    }


}