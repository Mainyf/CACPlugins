package io.github.mainyf.worldsettings

import io.github.mainyf.worldsettings.config.ConfigManager
import org.apache.logging.log4j.LogManager

object Log {

    private val LOGGER = LogManager.getLogger("WorldSettings")

    fun debug(msg: String) {
        if (ConfigManager.debug) {
            LOGGER.info(msg)
        }
    }

}