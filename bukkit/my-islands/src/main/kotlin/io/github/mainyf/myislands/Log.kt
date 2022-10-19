package io.github.mainyf.myislands

import io.github.mainyf.myislands.config.ConfigMI
import org.apache.logging.log4j.LogManager
import org.bukkit.entity.Player

object Log {

    private val LOGGER = LogManager.getLogger("MyIslands")

    fun debug(msg: String) {
        if (ConfigMI.debug) {
            LOGGER.info(msg)
        }
    }

    fun debugP(player: Player, msg: String) {
        if (ConfigMI.debug) {
            LOGGER.info("[${player.name}] $msg")
        }
    }

}