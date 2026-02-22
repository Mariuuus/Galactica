package de.mstrauss.galactica.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import de.mstrauss.galactica.R
import de.mstrauss.galactica.multiplayer.BluetoothConnectionManager
import de.mstrauss.galactica.multiplayer.ConnectionLobbyPayload

class MultiplayerLobbyHostView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val testButton: Button
    private val testTextView: TextView
    private val colsTextView: TextView
    private val colsSeekBar: SeekBar
    private val rowsTextView: TextView
    private val rowsSeekBar: SeekBar
    private val planetsTextView: TextView
    private val planetsSeekBar: SeekBar

    private val connectionListener = object : BluetoothConnectionManager.Listener {
        override fun onConnected(role: BluetoothConnectionManager.Role) {
            post {
                //testTextView.text = "Connected: ${role.name.lowercase()}"
            }
        }

        override fun onDisconnected() {
            post {
                //testTextView.text = "Connection closed."
            }
        }

        override fun onMessageReceived(message: String) {
            val payload = ConnectionLobbyPayload.decode(message) ?: return
            post {
                //testTextView.text = "Received: ${payload.timestamp}"
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
        LayoutInflater.from(context).inflate(R.layout.view_multiplayer_lobby_host, this, true)

        testButton = findViewById(R.id.testConnectionButton)
        testTextView = findViewById(R.id.testConnectionText)

        colsTextView = findViewById(R.id.cols_text)
        colsSeekBar = findViewById(R.id.seek_cols)

        rowsTextView = findViewById(R.id.rows_text)
        rowsSeekBar = findViewById(R.id.seek_rows)

        planetsTextView = findViewById(R.id.planets_text)
        planetsSeekBar = findViewById(R.id.seek_planets)


        planetsSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                planetsTextView.text = progress.toString()
                sendConfigUpdate()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        rowsSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                rowsTextView.text = progress.toString()
                sendConfigUpdate()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        colsSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                colsTextView.text = progress.toString()
                sendConfigUpdate()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        testButton.setOnClickListener { /*TODO: add start logic in here*/ }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        BluetoothConnectionManager.addListener(connectionListener)
    }

    override fun onDetachedFromWindow() {
        BluetoothConnectionManager.removeListener(connectionListener)
        super.onDetachedFromWindow()
    }

    private fun sendConfigUpdate() {
        val payload = ConnectionLobbyPayload(timestamp = System.currentTimeMillis(), rows = rowsSeekBar.progress, cols= colsSeekBar.progress, planets = planetsSeekBar.progress)
        val sent = BluetoothConnectionManager.send(payload.encode())
        if (!sent) {
            Toast.makeText(context, "No active Bluetooth connection.", Toast.LENGTH_SHORT).show()
            return
        }
    }
}
