package io.github.mainyf.myislands

import com.Zrips.CMI.events.CMIPlayerTeleportEvent
import com.plotsquared.core.PlotAPI
import com.plotsquared.core.PlotSquared
import com.plotsquared.core.plot.Plot
import io.github.mainyf.myislands.config.ConfigManager
import io.github.mainyf.myislands.features.MoveIslandCore
import io.github.mainyf.myislands.menu.IslandsChooseMenu
import io.github.mainyf.myislands.storage.StorageManager
import io.github.mainyf.newmclib.dsl.event
import io.github.mainyf.newmclib.dsl.events
import io.github.mainyf.newmclib.exts.*
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class MyIslands : JavaPlugin() {

    companion object {

        lateinit var INSTANCE: MyIslands

        lateinit var plotAPI: PlotAPI

        lateinit var plotUtils: PlotUtils

    }

    private val joinPlayers = mutableMapOf<UUID, Plot>()

    override fun onEnable() {
        INSTANCE = this
        plotAPI = PlotAPI()
        ConfigManager.load()
        StorageManager.init()
        val injector = PlotSquared.platform().injector()
        val commandHandler = injector.getInstance(CommandHandler::class.java)
        plotUtils = injector.getInstance(PlotUtils::class.java)

        Bukkit.getServer().pluginManager.registerEvents(MoveIslandCore, this)
        registerCommand("myislands", commandHandler)
        events {
            event<PlayerJoinEvent>(priority = EventPriority.LOWEST) {
                runTaskLaterBR(20L) {
                    val plotPlayer = plotAPI.wrapPlayer(player.uuid)
                    if (plotPlayer == null) {
                        player.errorMsg("未知错误 MI0x1")
                        return@runTaskLaterBR
                    }
                    val plots = plotAPI.getPlayerPlots(plotPlayer)
                    if (plots.isNotEmpty()) {
                        joinPlayers[player.uuid] = plots.first()
                        runTaskLaterBR(10L) {
                            plots.first().getHome {
                                plotPlayer.teleport(it)
                            }
                        }
                        return@runTaskLaterBR
                    }
                    runTaskLaterBR(40L) {
                        IslandsChooseMenu().open(player)
                    }
                }
            }
            event<CMIPlayerTeleportEvent> {
                if (joinPlayers.containsKey(player.uuid)) {
                    joinPlayers.remove(player.uuid)
                    isCancelled = true
//                    val plotPlayer = plotAPI.wrapPlayer(player.uuid)!!
//                    plot.getHome {
//                        plotPlayer.teleport(it)
//                    }
                }
            }
        }
    }

    override fun onDisable() {
        StorageManager.close()
    }

}