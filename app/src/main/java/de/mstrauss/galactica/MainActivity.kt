package de.mstrauss.galactica

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import de.mstrauss.galactica.patterns.MainMenuState
import de.mstrauss.galactica.patterns.MenuStateMachine
import de.mstrauss.galactica.patterns.MenuUi
import de.mstrauss.galactica.patterns.MultiPlayerState
import de.mstrauss.galactica.patterns.SettingsState
import de.mstrauss.galactica.patterns.SinglePlayerCampaignState
import de.mstrauss.galactica.patterns.SinglePlayerPlaygroundState
import de.mstrauss.galactica.patterns.SinglePlayerState
import de.mstrauss.galactica.multiplayer.ConnectionTestPayload
import de.mstrauss.galactica.ui.applyFullscreen
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.UUID


class MainActivity : AppCompatActivity() {
    private companion object {
        private const val BLUETOOTH_SERVICE_NAME = "GalacticaBluetoothService"
        private val BLUETOOTH_SERVICE_UUID: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
    }

    private enum class PendingBluetoothAction {
        HOST,
        JOIN
    }

    private lateinit var stateMachine: MenuStateMachine
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var pendingBluetoothAction: PendingBluetoothAction? = null

    private var acceptThread: AcceptThread? = null
    private var connectThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? = null
    private var hostSocket: BluetoothSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var devicePickerDialog: AlertDialog? = null
    private lateinit var testConnectionButton: Button
    private lateinit var testConnectionText: TextView

    private val discoveredDevices = mutableListOf<BluetoothDevice>()
    private val discoveredDeviceLabels = mutableListOf<String>()
    private lateinit var discoveredDeviceAdapter: ArrayAdapter<String>

    private val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    } ?: return
                    addDiscoveredDevice(device)
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    if (discoveredDevices.isEmpty()) {
                        showToast("No Bluetooth hosts found.")
                    }
                }
            }
        }
    }

    private val requestBluetoothPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            if (hasBluetoothPermissions()) {
                runPendingBluetoothAction()
            } else {
                pendingBluetoothAction = null
                showToast("Bluetooth permissions are required.")
            }
        }

    private val requestEnableBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            if (isBluetoothEnabled()) {
                runPendingBluetoothAction()
            } else {
                pendingBluetoothAction = null
                showToast("Please enable Bluetooth first.")
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)

        applyFullscreen()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bluetoothAdapter = getSystemService(BluetoothManager::class.java)?.adapter
        discoveredDeviceAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            discoveredDeviceLabels
        )
        registerReceiver(
            discoveryReceiver,
            IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
        )

        stateMachine = MenuStateMachine(MenuUi.bind(this))
        findViewById<View>(R.id.menu_singleplayer_button).setOnClickListener {
            stateMachine.changeState(SinglePlayerState())
        }
        findViewById<View>(R.id.menu_multiplayer_button).setOnClickListener {
            stateMachine.changeState(MultiPlayerState())
        }
        findViewById<View>(R.id.menu_settings_button).setOnClickListener {
            stateMachine.changeState(SettingsState())
        }
        findViewById<View>(R.id.menu_back_button_inner).setOnClickListener {
            stateMachine.changeState(MainMenuState())
        }
        findViewById<View>(R.id.single_player_playground_button).setOnClickListener {
            stateMachine.changeState((SinglePlayerPlaygroundState()))
        }
        findViewById<Button>(R.id.campaign_button).setOnClickListener {
            stateMachine.changeState((SinglePlayerCampaignState()))
        }
        findViewById<View>(R.id.start_single_player_game_button).setOnClickListener {
            startActivity(
                SinglePlayerActivity.createIntent(
                    context = this,
                    gridRows = 7,
                    gridCols = 9,
                    planetAmount = 4,
                    allowedMoves = 7 * 9
                )
            )
        }
        findViewById<Button>(R.id.host_game_button).setOnClickListener {
            startHostingAsServer()
        }
        findViewById<Button>(R.id.join_game_button).setOnClickListener {
            startJoinDiscovery()
        }
        testConnectionButton = findViewById(R.id.testConnectionButton)
        testConnectionText = findViewById(R.id.testConnectionText)
        testConnectionButton.setOnClickListener { sendTestTimestampMessage() }
        stateMachine.changeState(MainMenuState())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyFullscreen()
    }

    override fun onDestroy() {
        super.onDestroy()
        acceptThread?.cancel()
        connectThread?.cancel()
        connectedThread?.cancel()
        devicePickerDialog?.dismiss()
        cancelDiscovery()
        unregisterReceiver(discoveryReceiver)
        closeQuietly(hostSocket)
        closeQuietly(clientSocket)
    }

    @SuppressLint("MissingPermission")
    private fun startHostingAsServer() {
        if (!ensureBluetoothReady(PendingBluetoothAction.HOST)) return

        connectedThread?.cancel()
        acceptThread?.cancel()
        acceptThread = AcceptThread(bluetoothAdapter!!).also { it.start() }
        showToast("Hosting started. Waiting for client...")
    }

    @SuppressLint("MissingPermission")
    private fun startJoinDiscovery() {
        if (!ensureBluetoothReady(PendingBluetoothAction.JOIN)) return

        connectedThread?.cancel()
        cancelDiscovery()
        discoveredDevices.clear()
        discoveredDeviceLabels.clear()
        discoveredDeviceAdapter.notifyDataSetChanged()

        bluetoothAdapter!!.bondedDevices.forEach { addDiscoveredDevice(it) }
        showHostPickerDialog()

        val started = bluetoothAdapter!!.startDiscovery()
        if (!started && discoveredDevices.isEmpty()) {
            showToast("Could not start Bluetooth discovery.")
        }
    }

    private fun ensureBluetoothReady(action: PendingBluetoothAction): Boolean {
        if (bluetoothAdapter == null) {
            showToast("Bluetooth not supported on this device.")
            return false
        }

        if (!hasBluetoothPermissions()) {
            pendingBluetoothAction = action
            requestBluetoothPermissions.launch(requiredBluetoothPermissions())
            return false
        }

        if (!isBluetoothEnabled()) {
            pendingBluetoothAction = action
            requestEnableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return false
        }

        return true
    }

    private fun runPendingBluetoothAction() {
        when (pendingBluetoothAction) {
            PendingBluetoothAction.HOST -> startHostingAsServer()
            PendingBluetoothAction.JOIN -> startJoinDiscovery()
            null -> Unit
        }
        pendingBluetoothAction = null
    }

    private fun requiredBluetoothPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    private fun hasBluetoothPermissions(): Boolean =
        requiredBluetoothPermissions().all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }

    @SuppressLint("MissingPermission")
    private fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun sendTestTimestampMessage() {
        val payload = ConnectionTestPayload(timestamp = System.currentTimeMillis())
        val sent = connectedThread?.write(payload.encode()) == true
        if (!sent) {
            showToast("No active Bluetooth connection.")
            return
        }
        testConnectionText.text = "Sent: ${payload.timestamp}"
    }

    private fun handleIncomingPayload(rawPayload: String) {
        val payload = ConnectionTestPayload.decode(rawPayload) ?: return
        runOnUiThread {
            testConnectionText.text = "Received: ${payload.timestamp}"
        }
    }

    private fun onSocketConnected(socket: BluetoothSocket, roleLabel: String) {
        connectedThread?.cancel()
        connectedThread = ConnectedThread(socket).also { it.start() }
        runOnUiThread {
            showToast("Connected as $roleLabel. Test messaging ready.")
        }
    }

    private fun closeQuietly(socket: BluetoothSocket?) {
        try {
            socket?.close()
        } catch (_: IOException) {
        }
    }

    @SuppressLint("MissingPermission")
    private fun addDiscoveredDevice(device: BluetoothDevice) {
        val alreadyPresent = discoveredDevices.any { it.address == device.address }
        if (alreadyPresent) return

        discoveredDevices.add(device)
        val label = "${device.name ?: "Unknown device"} (${device.address})"
        discoveredDeviceLabels.add(label)
        runOnUiThread { discoveredDeviceAdapter.notifyDataSetChanged() }
    }

    private fun showHostPickerDialog() {
        if (devicePickerDialog?.isShowing == true) return

        devicePickerDialog = AlertDialog.Builder(this)
            .setTitle("Select host device")
            .setAdapter(discoveredDeviceAdapter) { _, which ->
                val device = discoveredDevices.getOrNull(which) ?: return@setAdapter
                connectToDevice(device)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setOnDismissListener {
                cancelDiscovery()
                devicePickerDialog = null
            }
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        connectThread?.cancel()
        connectThread = ConnectThread(bluetoothAdapter!!, device).also { it.start() }
        showToast("Trying to connect to ${device.name ?: device.address}...")
        devicePickerDialog?.dismiss()
    }

    @SuppressLint("MissingPermission")
    private fun cancelDiscovery() {
        val adapter = bluetoothAdapter ?: return
        if (adapter.isDiscovering) adapter.cancelDiscovery()
    }

    private inner class AcceptThread(adapter: BluetoothAdapter) : Thread() {
        private val serverSocket: BluetoothServerSocket? = try {
            adapter.listenUsingRfcommWithServiceRecord(BLUETOOTH_SERVICE_NAME, BLUETOOTH_SERVICE_UUID)
        } catch (_: IOException) {
            null
        }

        override fun run() {
            var socket: BluetoothSocket? = null
            while (socket == null) {
                socket = try {
                    serverSocket?.accept()
                } catch (_: IOException) {
                    break
                }
            }

            if (socket != null) {
                hostSocket = socket
                closeQuietly(clientSocket)
                onSocketConnected(socket, "host")
            }

            try {
                serverSocket?.close()
            } catch (_: IOException) {
            }
        }

        fun cancel() {
            try {
                serverSocket?.close()
            } catch (_: IOException) {
            }
        }
    }

    private inner class ConnectThread(
        private val adapter: BluetoothAdapter,
        private val device: BluetoothDevice
    ) : Thread() {
        private val socket: BluetoothSocket? = try {
            device.createRfcommSocketToServiceRecord(BLUETOOTH_SERVICE_UUID)
        } catch (_: IOException) {
            null
        }

        @SuppressLint("MissingPermission")
        override fun run() {
            adapter.cancelDiscovery()

            try {
                socket?.connect()
                clientSocket = socket
                closeQuietly(hostSocket)
                if (socket != null) {
                    onSocketConnected(socket, "client")
                }
            } catch (_: IOException) {
                closeQuietly(socket)
                runOnUiThread { showToast("Bluetooth connection failed.") }
            }
        }

        fun cancel() {
            closeQuietly(socket)
        }
    }

    private inner class ConnectedThread(private val socket: BluetoothSocket) : Thread() {
        private val input = DataInputStream(BufferedInputStream(socket.inputStream))
        private val output = DataOutputStream(BufferedOutputStream(socket.outputStream))
        @Volatile private var running = true

        override fun run() {
            while (running) {
                val message = try {
                    input.readUTF()
                } catch (_: IOException) {
                    break
                }
                handleIncomingPayload(message)
            }

            runOnUiThread {
                showToast("Bluetooth connection closed.")
            }
        }

        fun write(message: String): Boolean {
            if (!running) return false
            return try {
                output.writeUTF(message)
                output.flush()
                true
            } catch (_: IOException) {
                false
            }
        }

        fun cancel() {
            running = false
            closeQuietly(socket)
        }
    }
}
