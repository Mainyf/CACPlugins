package io.github.mainyf.myislands

import com.plotsquared.core.PlotAPI
import com.plotsquared.core.PlotSquared
import com.plotsquared.core.player.PlotPlayer
import io.github.mainyf.bungeesettingsbukkit.ServerPacket
import io.github.mainyf.myislands.config.ConfigMI
import io.github.mainyf.myislands.features.IsLandCreatureLimit
import io.github.mainyf.myislands.features.MoveIsLandCore
import io.github.mainyf.myislands.hook.QuestExtensionHooks
import io.github.mainyf.myislands.listeners.AuthListeners
import io.github.mainyf.myislands.listeners.NoAuthListeners
import io.github.mainyf.myislands.listeners.PlayerListeners
import io.github.mainyf.myislands.storage.StorageMI
import io.github.mainyf.newmclib.BasePlugin
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.hooks.addPlaceholderExpansion
import org.apache.logging.log4j.LogManager
import org.bukkit.entity.Player

class MyIslands : BasePlugin() {

    companion object {

        val LOGGER = LogManager.getLogger("MyIslands")

        lateinit var INSTANCE: MyIslands

        lateinit var plotAPI: PlotAPI

        lateinit var plotUtils: PlotUtils

        val ISLAND_TP_REQ = ServerPacket.registerPacket("broadcast_island_tp_req")

        val ISLAND_TP_RES = ServerPacket.registerPacket("broadcast_island_tp_res")

        val ISLAND_EMPTY_RES = ServerPacket.registerPacket("broadcast_island_empty_res")

        val ISLAND_NOT_ALLOWED_ACCESS = ServerPacket.registerPacket("broadcast_island_empty_not_allowed_access")

    }

    override fun enable() {
        INSTANCE = this
        plotAPI = PlotAPI()
        ConfigMI.load()
        StorageMI.init()
        val injector = PlotSquared.platform().injector()
        plotUtils = injector.getInstance(PlotUtils::class.java)

        MoveIsLandCore.init()
        IsLandCreatureLimit.init()
        QuestExtensionHooks.init()
        pluginManager().registerEvents(MoveIsLandCore, this)
        pluginManager().registerEvents(IsLandCreatureLimit, this)
        pluginManager().registerEvents(PlayerListeners, this)
        pluginManager().registerEvents(CrossServerHandler, this)
        CommandHandler.init()
        CommandHandler.register()
        if (isPlugin("AuthMe")) {
            LOGGER.info("检测到安装了登录插件，操作变更")
            server.pluginManager.registerEvents(AuthListeners, this)
        } else {
            server.pluginManager.registerEvents(NoAuthListeners, this)
        }
        submitTask(period = 20L) {
            onlinePlayers().forEach { player ->
                IslandsManager.checkPlayerPlotTrust(player)
                IslandsManager.fixIslandCore(player)
            }
        }
        submitTask(async = true, period = 20L) {
            IslandsManager.checkIslandHeatsAttenuation()
        }
        addPlaceholderExpansion("myislands") papi@{ _, params ->
//            val uuid = offlinePlayer?.uniqueId ?: return@papi null
//
//            val player = offlinePlayer.player ?: return@papi null
            when (params) {
                "cost_reset" -> ConfigMI.myislandCost.reset.toDisplayText()
                "cost_movecore" -> ConfigMI.myislandCost.moveCore.toDisplayText()
                "cost_switchvisibility" -> ConfigMI.myislandCost.switchVisibility.toDisplayText()
                "cost_addhelper" -> ConfigMI.myislandCost.addHelper.toDisplayText()
                else -> null
            }
        }
    }

    override fun onDisable() {
        StorageMI.close()
    }

}

fun Player.asPlotPlayer(): PlotPlayer<*>? {
    return MyIslands.plotAPI.wrapPlayer(uuid)
}