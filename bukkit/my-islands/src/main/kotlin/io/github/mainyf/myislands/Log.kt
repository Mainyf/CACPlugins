package io.github.mainyf.myislands

import io.github.mainyf.myislands.config.ConfigManager
import org.apache.logging.log4j.LogManager
import org.bukkit.entity.Player

object Log {

    private val LOGGER = LogManager.getLogger("WorldSettings")

    fun debug(msg: String) {
        if (ConfigManager.debug) {
            LOGGER.info(msg)
        }
    }

    fun debugP(player: Player, msg: String) {
        if (ConfigManager.debug) {
            LOGGER.info("[${player.name}] $msg")
        }
    }

}