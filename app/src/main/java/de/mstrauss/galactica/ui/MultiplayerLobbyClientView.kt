package de.mstrauss.galactica.ui

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import de.mstrauss.galactica.MultiPlayerActivityClient
import de.mstrauss.galactica.MultiPlayerActivityHost
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
    private val planetsTextView: TextView

    private val connectionListener = object : BluetoothConnectionManager.Listener {

        override fun onConnected(role: BluetoothConnectionManager.Role) {
        }
        override fun onDisconnected() {
        }

        override fun onMessageReceived(message: String) {
            Log.d("Bluetooth", "received message $message")
            val payload = ConnectionLobbyPayload.decode(message) ?: return
            post {
                colsTextView.text = payload.cols.toString();
                rowsTextView.text = payload.rows.toString();
                planetsTextView.text = payload.planets.toString();

                if(payload.start) {
                    context.startActivity(
                        MultiPlayerActivityClient.createIntent(
                            context = context,
                            gridRows = payload.rows,
                            gridCols = payload.cols,
                            planetAmount = payload.planets,
                            randomSeed = payload.timestamp
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
        planetsTextView = findViewById(R.id.planets_text)
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
