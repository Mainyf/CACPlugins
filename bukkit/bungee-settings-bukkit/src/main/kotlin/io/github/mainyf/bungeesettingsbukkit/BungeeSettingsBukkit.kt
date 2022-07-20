package io.github.mainyf.bungeesettingsbukkit

import io.github.mainyf.newmclib.getLogger
import org.bukkit.plugin.java.JavaPlugin

class BungeeSettingsBukkit : JavaPlugin() {

    var bcSocketPort = 24440

    companion object {

        val LOG = getLogger("BungeeSettingsBukkit")
        lateinit var INSTANCE: BungeeSettingsBukkit

    }

    fun loadConfig() {
        saveDefaultConfig()
        reloadConfig()
        bcSocketPort = config.getInt("socketPort", bcSocketPort)
        CrossServerManager.loadSocket(bcSocketPort)
    }

    override fun onEnable() {
        INSTANCE = this
        loadConfig()
        CommandHandler.init()
        CrossServerManager.init()
    }

}
