package de.mstrauss.galactica.multiplayer

data class ConnectionLobbyPayload(val timestamp: Long, val rows: Int, val cols : Int, val planets: Int) {
    fun encode(): String = "$timestamp;$rows;$cols;$planets"

    companion object {
        fun decode(raw: String): ConnectionLobbyPayload? {
            val parts = raw.split(";")
            if (parts.size != 4) return null

            val timestamp = parts[0].toLongOrNull() ?: return null
            val rows = parts[1].toIntOrNull() ?: return null
            val cols = parts[2].toIntOrNull() ?: return null
            val planets = parts[3].toIntOrNull() ?: return null

            return ConnectionLobbyPayload(
                timestamp = timestamp,
                rows = rows,
                cols = cols,
                planets = planets
            )
        }
    }
}
