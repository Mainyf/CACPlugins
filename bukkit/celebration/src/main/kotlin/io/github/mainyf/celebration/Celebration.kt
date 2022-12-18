package io.github.mainyf.celebration

import org.apache.logging.log4j.LogManager
import org.bukkit.plugin.java.JavaPlugin

class Celebration : JavaPlugin() {

    companion object {

        val LOGGER = LogManager.getLogger("Celebration")

        lateinit var INSTANCE: Celebration

    }

    override fun onEnable() {
        INSTANCE = this
        
    }
}