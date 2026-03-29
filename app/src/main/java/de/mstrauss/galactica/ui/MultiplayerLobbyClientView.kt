package de.mstrauss.galactica.ui

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import de.mstrauss.galactica.MultiPlayerActivity
import de.mstrauss.galactica.R
import de.mstrauss.galactica.multiplayer.BluetoothConnectionManager
import de.mstrauss.galactica.multiplayer.ConnectionLobbyPayload

class MultiplayerLobbyClientView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val colsTextView: TextView
    private val rowsTextView: TextView
    private val bombsTextView: TextView
    private val rocketShipsTextView: TextView
    private val planetsTextView: TextView

    private val connectionListener = object : BluetoothConnectionManager.Listener {

        override fun onConnected(role: BluetoothConnectionManager.Role) {
        }
        override fun onDisconnected() {
        }

        override fun onMessageReceived(message: String) {
            val payload = ConnectionLobbyPayload.decode(message) ?: return
            Log.d(this::class.toString(), "received message $payload")
            post {
                colsTextView.text = payload.cols.toString();
                rowsTextView.text = payload.rows.toString();
                planetsTextView.text = payload.planets.toString();
                rocketShipsTextView.text = payload.rocketShips.toString();
                bombsTextView.text = payload.bombs.toString();

                if(payload.start) {
                    BluetoothConnectionManager.removeListener(this)
                    context.startActivity(
                        MultiPlayerActivity.createIntent(
                            context = context,
                            gridRows = payload.rows,
                            gridCols = payload.cols,
                            planetAmount = payload.planets,
                            bombAmount =  payload.bombs,
                            rocketshipAmount =  payload.rocketShips,
                            randomSeed = payload.timestamp,
                            role = BluetoothConnectionManager.Role.CLIENT
                        )
                    )
                }
            }
        }

        override fun onError(message: String) {
            post {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_multiplayer_lobby_client, this, true)

        colsTextView = findViewById(R.id.cols_text)
        rowsTextView = findViewById(R.id.rows_text)
        bombsTextView = findViewById(R.id.bombs_text)
        rocketShipsTextView = findViewById(R.id.rocketship_text)
        planetsTextView = findViewById(R.id.my_planets)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        BluetoothConnectionManager.addListener(connectionListener)
    }

    override fun onDetachedFromWindow() {
        BluetoothConnectionManager.removeListener(connectionListener)
        super.onDetachedFromWindow()
    }
}
