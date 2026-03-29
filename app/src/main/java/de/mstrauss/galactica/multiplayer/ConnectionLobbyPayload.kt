package de.mstrauss.galactica.multiplayer

data class ConnectionLobbyPayload(val timestamp: Long, val rows: Int, val cols : Int, val planets: Int, val bombs: Int, val rocketShips: Int, val start: Boolean)  : ConnectionPayload {
    override fun encode(): String = "$timestamp;$rows;$cols;$planets;$bombs;$rocketShips$start"

    companion object {
        fun decode(raw: String): ConnectionLobbyPayload? {
            val parts = raw.split(";")
            if (parts.size != 7) return null

            val timestamp = parts[0].toLongOrNull() ?: return null
            val rows = parts[1].toIntOrNull() ?: return null
            val cols = parts[2].toIntOrNull() ?: return null
            val planets = parts[3].toIntOrNull() ?: return null
            val bombs = parts[4].toIntOrNull() ?: return null
            val rocketShips = parts[5].toIntOrNull() ?: return null
            val start = parts[6].toBooleanStrictOrNull() ?: return null

            return ConnectionLobbyPayload(
                timestamp = timestamp,
                rows = rows,
                cols = cols,
                planets = planets,
                bombs = bombs,
                rocketShips = rocketShips,
                start = start
            )
        }
    }
    override fun toString(): String {
        return "$timestamp;$rows;$cols;$planets;$bombs;$rocketShips$start"
    }
}
