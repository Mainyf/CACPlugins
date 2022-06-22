package io.github.mainyf.myislands

import com.plotsquared.core.PlotAPI
import com.plotsquared.core.PlotSquared
import com.plotsquared.core.player.PlotPlayer
import io.github.mainyf.myislands.config.ConfigManager
import io.github.mainyf.myislands.features.MoveIslandCore
import io.github.mainyf.myislands.listeners.AuthListeners
import io.github.mainyf.myislands.listeners.NoAuthListeners
import io.github.mainyf.myislands.listeners.PlayerListeners
import io.github.mainyf.myislands.storage.StorageManager
import io.github.mainyf.newmclib.exts.isPluginEnabled
import io.github.mainyf.newmclib.exts.pluginManager
import io.github.mainyf.newmclib.exts.registerCommand
import io.github.mainyf.newmclib.exts.uuid
import org.apache.logging.log4j.LogManager
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class MyIslands : JavaPlugin() {

    companion object {

        val LOGGER = LogManager.getLogger("MyIslands")

        lateinit var INSTANCE: MyIslands

        lateinit var plotAPI: PlotAPI

        lateinit var plotUtils: PlotUtils
    }


    override fun onEnable() {
        INSTANCE = this
        plotAPI = PlotAPI()
        ConfigManager.load()
        StorageManager.init()
        val injector = PlotSquared.platform().injector()
        val commandHandler = injector.getInstance(CommandHandler::class.java)
        plotUtils = injector.getInstance(PlotUtils::class.java)

        MoveIslandCore.init()
        pluginManager().registerEvents(MoveIslandCore, this)
        pluginManager().registerEvents(PlayerListeners, this)
        registerCommand("myislands", commandHandler)
        if (isPluginEnabled("AuthMe")) {
            LOGGER.info("检测到安装了登录插件，操作变更")
            server.pluginManager.registerEvents(AuthListeners, this)
        } else {
            server.pluginManager.registerEvents(NoAuthListeners, this)
        }
    }

    override fun onDisable() {
        StorageManager.close()
    }

}

fun Player.asPlotPlayer(): PlotPlayer<*>? {
    return MyIslands.plotAPI.wrapPlayer(uuid)
}