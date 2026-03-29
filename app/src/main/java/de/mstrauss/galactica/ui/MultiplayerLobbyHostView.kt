package de.mstrauss.galactica.ui

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import de.mstrauss.galactica.MultiPlayerActivity
import de.mstrauss.galactica.R
import de.mstrauss.galactica.multiplayer.BluetoothConnectionManager
import de.mstrauss.galactica.multiplayer.ConnectionLobbyPayload

class MultiplayerLobbyHostView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val playButton: Button
    private val colsTextView: TextView
    private val colsSeekBar: SeekBar
    private val rowsTextView: TextView
    private val rowsSeekBar: SeekBar
    private val planetsTextView: TextView
    private val planetsSeekBar: SeekBar

    private val bombsTextView: TextView
    private val bombsSeekBar: SeekBar

    private val rocketShipsTextView: TextView
    private val rocketShipsSeekBar: SeekBar

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
            Log.d(this::class.toString(), "received message $message")
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

        playButton = findViewById(R.id.start_host_lobby_button)

        colsTextView = findViewById(R.id.cols_text)
        colsSeekBar = findViewById(R.id.seek_cols)

        rowsTextView = findViewById(R.id.rows_text)
        rowsSeekBar = findViewById(R.id.seek_rows)

        planetsTextView = findViewById(R.id.my_planets)
        planetsSeekBar = findViewById(R.id.seek_planets)

        bombsTextView = findViewById(R.id.bombs_text)
        bombsSeekBar = findViewById(R.id.seek_bombs)

        rocketShipsTextView = findViewById(R.id.rocketships_text)
        rocketShipsSeekBar = findViewById(R.id.seek_rocketships)


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

        bombsSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                bombsTextView.text = progress.toString()
                sendConfigUpdate()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })


        rocketShipsSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                rocketShipsTextView.text = progress.toString()
                sendConfigUpdate()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })


        playButton.setOnClickListener {
            val seed = System.currentTimeMillis()
            context.startActivity(
                MultiPlayerActivity.createIntent(
                    context = context,
                    gridRows = rowsSeekBar.progress,
                    gridCols = colsSeekBar.progress,
                    planetAmount = planetsSeekBar.progress,
                    bombAmount = bombsSeekBar.progress,
                    rocketshipAmount = rocketShipsSeekBar.progress,
                    randomSeed = seed,
                    role = BluetoothConnectionManager.Role.HOST
                )
            )
        }
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
        Log.d(this.javaClass.toString(), "Sending update to client!")
        val payload = ConnectionLobbyPayload(timestamp = System.currentTimeMillis(), rows = rowsSeekBar.progress, cols= colsSeekBar.progress, planets = planetsSeekBar.progress, bombs =  bombsSeekBar.progress, rocketShips = rocketShipsSeekBar.progress,  start = false)
        val sent = BluetoothConnectionManager.send(payload.encode())
        if (!sent) {
            Toast.makeText(context, "No active Bluetooth connection.", Toast.LENGTH_SHORT).show()
            return
        }
    }
}
