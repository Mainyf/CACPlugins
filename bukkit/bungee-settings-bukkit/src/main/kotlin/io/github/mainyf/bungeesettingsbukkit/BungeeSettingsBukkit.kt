package io.github.mainyf.bungeesettingsbukkit

import io.github.mainyf.newmclib.BasePlugin
import io.github.mainyf.newmclib.getLogger
import org.bukkit.event.Listener

class BungeeSettingsBukkit : BasePlugin(), Listener {

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

    override fun enable() {
        INSTANCE = this
        loadConfig()
        CommandHandler.init()
        CrossServerManager.init()
    }

}
