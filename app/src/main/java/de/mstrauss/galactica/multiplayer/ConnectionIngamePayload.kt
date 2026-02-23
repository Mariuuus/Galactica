package de.mstrauss.galactica.multiplayer


interface ConnectionPayload {
    fun encode(): String
}

data class ConnectionIngamePayload(val timestamp: Long, val type: Type, val planetsFound: Int) : ConnectionPayload {
    enum class Type {
        JOINED, WON, NEXT_TURN
    }

    override fun encode(): String = "$timestamp;${type.toString()};$planetsFound"

    companion object {
        fun decode(raw: String): ConnectionIngamePayload? {
            val parts = raw.split(";")
            if (parts.size != 3) return null

            val timestamp = parts[0].toLongOrNull() ?: return null
            val type = parts[1]
            val planetLeft = parts[2].toIntOrNull() ?: return null

            return ConnectionIngamePayload(
                timestamp = timestamp,
                type = Type.valueOf(type),
                planetsFound = planetLeft,
            )
        }
    }

    override fun toString(): String {
        return "$timestamp;${type.toString()};$planetsFound"
    }
}
