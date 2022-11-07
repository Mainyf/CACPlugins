package io.github.mainyf.worldsettings

import io.github.mainyf.worldsettings.config.ConfigWS
import org.apache.logging.log4j.LogManager

object Log {

    private val LOGGER = LogManager.getLogger("WorldSettings")

    fun debug(msg: String) {
        if (ConfigWS.debug) {
            LOGGER.info(msg)
        }
    }

}