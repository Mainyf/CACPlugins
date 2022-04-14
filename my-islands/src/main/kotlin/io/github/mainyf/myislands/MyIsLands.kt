package io.github.mainyf.myislands

import com.plotsquared.core.PlotAPI
import com.plotsquared.core.PlotSquared
import io.github.mainyf.newmclib.exts.registerCommand
import io.github.mainyf.myislands.config.ConfigManager
import io.github.mainyf.myislands.storage.StorageManager
import org.bukkit.plugin.java.JavaPlugin

class MyIslands : JavaPlugin() {

    companion object {

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
        registerCommand("myislands", commandHandler)
    }

    override fun onDisable() {
        StorageManager.close()
    }

}