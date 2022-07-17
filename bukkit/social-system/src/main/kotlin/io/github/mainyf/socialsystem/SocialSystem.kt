package io.github.mainyf.socialsystem

import io.github.mainyf.newmclib.exts.pluginManager
import io.github.mainyf.socialsystem.config.ConfigManager
import io.github.mainyf.socialsystem.listeners.PlayerListeners
import io.github.mainyf.socialsystem.module.FriendHandler
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
        StorageManager.init()
        CommandHandler.register()
        pluginManager().registerEvents(PlayerListeners, this)
    }

    override fun onDisable() {
        StorageManager.close()
    }

}