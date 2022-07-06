package io.github.mainyf.socialsystem

import io.github.mainyf.socialsystem.config.ConfigManager
import io.github.mainyf.socialsystem.storage.StorageManager
import org.apache.logging.log4j.LogManager
import org.bukkit.plugin.java.JavaPlugin

class SocialSystem : JavaPlugin() {

    companion object {

        val LOGGER = LogManager.getLogger("SocialSystem")

        lateinit var INSTANCE: SocialSystem

    }

    override fun onEnable() {
        INSTANCE = this
        ConfigManager.load()
        CommandHandler.register()
    }

    override fun onDisable() {
        StorageManager.close()
    }

}