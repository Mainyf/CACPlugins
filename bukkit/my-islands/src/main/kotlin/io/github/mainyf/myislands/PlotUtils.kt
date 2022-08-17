package io.github.mainyf.myislands

import com.plotsquared.bukkit.util.BukkitUtil
import com.plotsquared.core.PlotSquared
import com.plotsquared.core.database.DBFunc
import com.plotsquared.core.permissions.PermissionHandler.PermissionHandlerCapability
import com.plotsquared.core.player.PlayerMetaDataKeys
import com.plotsquared.core.player.PlotPlayer
import com.plotsquared.core.plot.Plot
import com.plotsquared.core.plot.PlotArea
import com.plotsquared.core.plot.world.PlotAreaManager
import com.plotsquared.core.services.ServicePipeline
import com.plotsquared.core.services.plots.AutoService
import com.plotsquared.core.util.EventDispatcher
import com.plotsquared.core.util.SchematicHandler
import com.plotsquared.core.util.query.PlotQuery
import com.plotsquared.core.util.task.AutoClaimFinishTask
import com.plotsquared.core.util.task.RunnableVal
import com.plotsquared.core.util.task.TaskManager
import com.plotsquared.google.Inject
import com.shopify.promises.Promise
import io.github.mainyf.myislands.config.sendLang
import io.github.mainyf.newmclib.exts.errorMsg
import io.github.mainyf.newmclib.exts.runTaskLaterBR
import io.github.mainyf.newmclib.exts.submitTask
import io.github.mainyf.newmclib.exts.uuid
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.UUID

class PlotUtils @Inject constructor(
    val servicePipeline: ServicePipeline,
    val schematicHandler: SchematicHandler,
    val eventDispatcher: EventDispatcher,
    val plotAreaManager: PlotAreaManager
) {

    fun removePlot(pp: PlotPlayer<*>, plot: Plot): Promise<Unit, Throwable> {
        return Promise {
            kotlin.runCatching {
                plot.plotModificationManager.deletePlot(pp) {
                    plot.removeRunning()
                    this@PlotUtils.eventDispatcher.callPostDelete(plot)
                    resolve(Unit)
                }
            }.onFailure {
                it.printStackTrace()
                reject(it)
            }
        }
    }

    fun getPlotByPLoc(player: Player): Plot? {
        val plotPlayer = player.asPlotPlayer()!!
        return plotPlayer.location.plotAbs
    }

    fun paste(player: Player, plot: Plot, schematiceName: String, block: (Boolean) -> Unit) {
        TaskManager.runTaskAsync {
            schematicHandler.schematicNames
            val schematice = schematicHandler.getSchematic(schematiceName)
            if (schematice == null) {
                player.errorMsg("$schematiceName 不存在")
                return@runTaskAsync
            }

            kotlin.runCatching {
                schematicHandler.paste(
                    schematice,
                    plot,
                    0,
                    plot.area!!.minBuildHeight,
                    0,
                    false,
                    MyIslands.plotAPI.wrapPlayer(player.uniqueId),
                    object : RunnableVal<Boolean>() {
                        override fun run(status: Boolean) {
                            block(status)
                        }
                    })
            }.onFailure {
                it.printStackTrace()
                MyIslands.INSTANCE.runTaskLaterBR(2 * 20L) {
                    block(true)
                }
            }
        }
    }

    fun autoClaimPlot(player: Player, plotPlayer: PlotPlayer<*>, schematic: String? = null, block: () -> Unit) {
        var playerPlotArea = plotPlayer.applicablePlotArea
//        var playerPlotArea: PlotArea? = null
        if (playerPlotArea == null) {
            val permissionHandler = PlotSquared.platform().permissionHandler()
            if (permissionHandler.hasCapability(PermissionHandlerCapability.PER_WORLD_PERMISSIONS)) {
                val allPlotAreas = this.plotAreaManager.allPlotAreas
                for (plotArea in allPlotAreas) {
                    if (plotPlayer.hasPermission(plotArea.worldName, "plots.auto")) {
                        if (playerPlotArea != null) {
                            playerPlotArea = null
                            break
                        }
                        playerPlotArea = plotArea
                    }
                }
            }

            if (this.plotAreaManager.allPlotAreas.size == 1) {
                playerPlotArea = this.plotAreaManager.allPlotAreas[0]
            }

            if (playerPlotArea == null) {
                player.errorMsg("出现意外错误: MI9，请报告给管理员")
                return
            }
        }
        val plots = servicePipeline.pump(
            AutoService.AutoQuery(
                plotPlayer,
                null,
                1,
                1,
                playerPlotArea
            )
        ).through(AutoService::class.java).result

        if (plots.size == 1) {
            claimSingle(plotPlayer, plots[0], playerPlotArea, schematic, block)
        }
    }

    private fun claimSingle(
        plotPlayer: PlotPlayer<*>,
        plot: Plot,
        plotArea: PlotArea,
        schematic: String?,
        block: () -> Unit
    ) {
        val metaDataAccess = plotPlayer.accessTemporaryMetaData(PlayerMetaDataKeys.TEMPORARY_AUTO)
        try {
            metaDataAccess.set(true)
        } catch (ex: Throwable) {
            try {
                metaDataAccess.close()
            } catch (var8: Throwable) {
                ex.addSuppressed(var8)
            }
            throw ex
        }
        metaDataAccess.close()
        plot.ownerAbs = plotPlayer.uuid
        DBFunc.createPlotSafe(plot, object : RunnableVal<Plot>(plot) {
            override fun run(plot: Plot) {
                try {
                    TaskManager.getPlatformImplementation()!!
                        .sync(
                            AutoClaimFinishTask(
                                plotPlayer,
                                plot,
                                plotArea,
                                null,
                                PlotSquared.get().eventDispatcher
                            )
                        )
                    block()
                } catch (var3x: Exception) {
                    var3x.printStackTrace()
                }
            }
        }) {
            this.claimSingle(plotPlayer, plot, plotArea, schematic, block)
        }
    }

    fun teleportHomePlot(player: Player) {
        val islandData = IslandsManager.getIslandData(player.uuid)
        if (islandData == null) {
            player.sendLang("playerNoPlot")
            return
        }
        val coreLoc = IslandsManager.getIslandCoreLoc(islandData)
        val homeLoc = IslandsManager.getHomeLoc(coreLoc)
        teleportPlayerToLoc(player, homeLoc, coreLoc)
    }

    fun findSafeLoc(sender: CommandSender?, loc: Location, coreLoc: Location, msg: Boolean = true): Location {
        if (hasDanger(loc, -1.0)) {
            val z = 0.0
            repeat(2) {
                for (i in 1..4) {
                    val newLoc = coreLoc.clone().add(0.0, 0.0, if (it == 0) z else -z)
                    if (hasDanger(newLoc)) {
                        continue
                    }
                    if (msg) {
                        sender?.sendLang("dangerHomeLoc")
                    }
                    return loc
                }
            }
            val x = 0.0
            repeat(2) {
                for (i in 1..4) {
                    val newLoc = coreLoc.clone().add(if (it == 0) x else -x, 0.0, 0.0)
                    if (hasDanger(newLoc)) {
                        continue
                    }
                    if (msg) {
                        sender?.sendLang("dangerHomeLoc")
                    }
                    return loc
                }
            }
            return if (hasDanger(coreLoc)) {
                coreLoc.clone().add(0.0, -1.0, 0.0).block.type = Material.STONE
                coreLoc
            } else {
                if (msg) {
                    sender?.sendLang("dangerHomeLoc")
                }
                coreLoc
            }
        } else {
            return loc
        }
    }

    fun teleportPlayerToLoc(player: Player, loc: Location, coreLoc: Location) {
        player.teleport(IslandsManager.fixIslandHomeLoc(findSafeLoc(player, loc, coreLoc)))
    }

    fun hasDanger(loc: Location, yOffset: Double = 0.0): Boolean {
        return loc.clone().add(0.0, yOffset, 0.0).block.isEmpty
    }

    fun findPlot(uuid: UUID): Plot? {
        return PlotQuery
            .newQuery()
            .ownedBy(uuid)
            .asSet()
            .firstOrNull()
    }

}