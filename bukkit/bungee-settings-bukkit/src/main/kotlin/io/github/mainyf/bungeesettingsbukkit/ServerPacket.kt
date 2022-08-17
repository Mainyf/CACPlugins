package io.github.mainyf.bungeesettingsbukkit

data class ServerPacket(
    val name: String
) {

    companion object {

        val packets = mutableMapOf<String, ServerPacket>()

        val UPDATE_SERVERIDS = registerPacket("update_serverid")

        val UPDATE_PLAYERS = registerPacket("update_players")

        val PLAYER_CHAT = registerPacket("player_chat")

        val CMD = registerPacket("cmd")

        val CMD_ALL = registerPacket("cmd_all")

        val TP_POS = registerPacket("tp_pos")

        val TP_PLAYER = registerPacket("tp_player")

        val KEEP_ALIVE = registerPacket("keep_alive")

        fun registerPacket(name: String): ServerPacket {
            if (packets.containsKey(name)) {
                return packets[name]!!
//                throw java.lang.RuntimeException("$name 已经注册过")
            }
            val packet = ServerPacket(name)
            packets[name] = packet
            return packet
        }

        fun getPacket(name: String): ServerPacket? {
            return packets[name]
        }

    }

}