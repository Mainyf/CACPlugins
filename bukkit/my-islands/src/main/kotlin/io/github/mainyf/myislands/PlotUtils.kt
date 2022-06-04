package io.github.mainyf.myislands

import com.plotsquared.core.PlotSquared
import com.plotsquared.core.database.DBFunc
import com.plotsquared.core.player.PlayerMetaDataKeys
import com.plotsquared.core.player.PlotPlayer
import com.plotsquared.core.plot.Plot
import com.plotsquared.core.plot.PlotArea
import com.plotsquared.core.services.ServicePipeline
import com.plotsquared.core.services.plots.AutoService
import com.plotsquared.core.util.EventDispatcher
import com.plotsquared.core.util.SchematicHandler
import com.plotsquared.core.util.task.AutoClaimFinishTask
import com.plotsquared.core.util.task.RunnableVal
import com.plotsquared.core.util.task.TaskManager
import com.plotsquared.google.Inject
import com.shopify.promises.Promise
import io.github.mainyf.newmclib.exts.errorMsg
import io.github.mainyf.newmclib.exts.runTaskLaterBR
import org.bukkit.entity.Player

class PlotUtils @Inject constructor(
    val servicePipeline: ServicePipeline,
    val schematicHandler: SchematicHandler,
    val eventDispatcher: EventDispatcher
) {

    fun removeIsland(pp: PlotPlayer<*>, plot: Plot): Promise<Unit, Throwable> {
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

    fun autoClaimPlot(plotPlayer: PlotPlayer<*>, schematic: String? = null, block: () -> Unit) {
        val playerPlotArea = plotPlayer.applicablePlotArea
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

}