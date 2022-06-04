package io.github.mainyf.playersettings.storage

import io.github.mainyf.newmclib.storage.BaseTable

object PlayerCommandLogs : BaseTable("t_PlayerCommandLog") {

    val playerUUID = uuid("player_uuid")

    val cmd = varchar("cmd", 255)

}

object PlayerLocLogs : BaseTable("t_PlayerLocLog") {

    val playerUUID = uuid("player_uuid")

    val serverId = varchar("server_name", 255)

    val world = varchar("world", 255)

    val xPos = double("xPos")

    val yPos = double("yPos")

    val zPos = double("zPos")

    val pitch = float("pitch")

    val yaw = float("yaw")

}

object PlayerMessageLogs : BaseTable("t_PlayerMessageLog") {

    val playerUUID = uuid("player_uuid")

    val message = varchar("message", 255)

}