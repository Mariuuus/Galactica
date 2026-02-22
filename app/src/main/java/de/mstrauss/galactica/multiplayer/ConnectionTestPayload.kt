package de.mstrauss.galactica.multiplayer

data class ConnectionTestPayload(val timestamp: Long) {
    fun encode(): String = timestamp.toString()

    companion object {
        fun decode(raw: String): ConnectionTestPayload? {
            val value = raw.toLongOrNull() ?: return null
            return ConnectionTestPayload(timestamp = value)
        }
    }
}
