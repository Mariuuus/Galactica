package de.mstrauss.galactica.multiplayer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.CopyOnWriteArraySet

object BluetoothConnectionManager {
    enum class Role {
        HOST,
        CLIENT
    }

    interface Listener {
        fun onConnected(role: Role)
        fun onDisconnected()
        fun onMessageReceived(message: String)
        fun onError(message: String)
    }

    private val listeners = CopyOnWriteArraySet<Listener>()

    private var acceptThread: AcceptThread? = null
    private var connectThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? = null
    private var currentSocket: BluetoothSocket? = null

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun isConnected(): Boolean = connectedThread != null

    @SuppressLint("MissingPermission")
    @Synchronized
    fun startHosting(adapter: BluetoothAdapter, serviceName: String, serviceUuid: UUID) {
        disconnect()
        acceptThread = AcceptThread(adapter, serviceName, serviceUuid).also { it.start() }
    }

    @SuppressLint("MissingPermission")
    @Synchronized
    fun connectToDevice(adapter: BluetoothAdapter, device: BluetoothDevice, serviceUuid: UUID) {
        disconnect()
        connectThread = ConnectThread(adapter, device, serviceUuid).also { it.start() }
    }

    @Synchronized
    fun send(message: String): Boolean = connectedThread?.write(message) == true

    @Synchronized
    fun disconnect() {
        val hadActiveConnection = connectedThread != null || currentSocket != null
        acceptThread?.cancel()
        connectThread?.cancel()
        connectedThread?.cancel()
        acceptThread = null
        connectThread = null
        connectedThread = null
        closeQuietly(currentSocket)
        currentSocket = null
        if (hadActiveConnection) {
            notifyDisconnected()
        }
    }

    @Synchronized
    private fun onSocketConnected(socket: BluetoothSocket, role: Role) {
        closeQuietly(currentSocket)
        currentSocket = socket
        connectedThread?.cancel()
        connectedThread = ConnectedThread(socket).also { it.start() }
        notifyConnected(role)
    }

    private fun onSocketClosed() {
        synchronized(this) {
            connectedThread = null
            closeQuietly(currentSocket)
            currentSocket = null
        }
        notifyDisconnected()
    }

    private fun notifyConnected(role: Role) {
        listeners.forEach { it.onConnected(role) }
    }

    private fun notifyDisconnected() {
        listeners.forEach { it.onDisconnected() }
    }

    private fun notifyMessage(message: String) {
        listeners.forEach { it.onMessageReceived(message) }
    }

    private fun notifyError(message: String) {
        listeners.forEach { it.onError(message) }
    }

    private fun closeQuietly(socket: BluetoothSocket?) {
        try {
            socket?.close()
        } catch (_: IOException) {
        }
    }

    private class AcceptThread(
        adapter: BluetoothAdapter,
        serviceName: String,
        serviceUuid: UUID
    ) : Thread() {
        private val serverSocket: BluetoothServerSocket? = try {
            adapter.listenUsingRfcommWithServiceRecord(serviceName, serviceUuid)
        } catch (_: IOException) {
            null
        }

        override fun run() {
            val socket = try {
                serverSocket?.accept()
            } catch (_: IOException) {
                null
            }

            if (socket != null) {
                BluetoothConnectionManager.onSocketConnected(socket, Role.HOST)
            } else {
                BluetoothConnectionManager.notifyError("Host accept failed.")
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

    private class ConnectThread(
        private val adapter: BluetoothAdapter,
        private val device: BluetoothDevice,
        private val serviceUuid: UUID
    ) : Thread() {
        private val socket: BluetoothSocket? = try {
            device.createRfcommSocketToServiceRecord(serviceUuid)
        } catch (_: IOException) {
            null
        }

        @SuppressLint("MissingPermission")
        override fun run() {
            adapter.cancelDiscovery()
            try {
                socket?.connect()
                if (socket != null) {
                    BluetoothConnectionManager.onSocketConnected(socket, Role.CLIENT)
                } else {
                    BluetoothConnectionManager.notifyError("Client connect socket was null.")
                }
            } catch (_: IOException) {
                BluetoothConnectionManager.closeQuietly(socket)
                BluetoothConnectionManager.notifyError("Bluetooth connection failed.")
            }
        }

        fun cancel() {
            BluetoothConnectionManager.closeQuietly(socket)
        }
    }

    private class ConnectedThread(private val socket: BluetoothSocket) : Thread() {
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
                BluetoothConnectionManager.notifyMessage(message)
            }
            BluetoothConnectionManager.onSocketClosed()
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
            BluetoothConnectionManager.closeQuietly(socket)
        }
    }
}
