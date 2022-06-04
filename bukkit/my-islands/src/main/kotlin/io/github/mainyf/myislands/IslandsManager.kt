package io.github.mainyf.myislands

import com.plotsquared.bukkit.paperlib.PaperLib
import com.plotsquared.bukkit.player.BukkitPlayer
import com.plotsquared.core.location.BlockLoc
import com.plotsquared.core.player.PlotPlayer
import com.plotsquared.core.plot.Plot
import com.shopify.promises.Promise
import com.shopify.promises.then
import dev.lone.itemsadder.api.CustomBlock
import io.github.mainyf.myislands.config.ConfigManager
import io.github.mainyf.myislands.config.Lang
import io.github.mainyf.myislands.config.send
import io.github.mainyf.myislands.menu.IslandsChooseMenu
import io.github.mainyf.myislands.storage.IslandVisibility
import io.github.mainyf.myislands.storage.PlayerIsland
import io.github.mainyf.myislands.storage.StorageManager
import io.github.mainyf.newmclib.exts.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.util.Vector
import java.util.*
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

object IslandsManager {

    private val joinPlayers = mutableMapOf<UUID, Plot>()

    fun addHelpers(player: Player, helperUUID: UUID) {
        StorageManager.transaction {
            val island = StorageManager.getPlayerIsland(player.uuid)
            if (island != null) {
                if ((island.helpers.toList().size ?: 0) >= 6) {
                    player.msg("授权者数量不能超过 6")
                    return@transaction
                }
                if (island.helpers.any { it.helperUUID == helperUUID }) {
                    player.msg("已经添加此授权者")
                    return@transaction
                }
                StorageManager.addHelpers(island, helperUUID)
            }
        }
    }

    fun getIslandHelpers(uuid: UUID): List<UUID> {
        return StorageManager.transaction {
            val data = StorageManager.getPlayerIsland(uuid)
            if (data == null) return@transaction emptyList()
            data.helpers.map { it.helperUUID }
        }
    }

    fun setIslandVisibility(/*player: Player, */island: PlayerIsland, visibility: IslandVisibility) {
        StorageManager.setVisibility(island, visibility)
    }

    fun addKudoToIsland(island: PlayerIsland, player: Player) {
        if (!StorageManager.addKudos(player.uuid, island)) {
            Lang.kudoRepeat.send(player)
        } else {
            Lang.kudoSuccess.send(player)
        }
    }

    fun resetIsland(pp: PlotPlayer<*>, plot: Plot) {
        removeIsland(pp, plot).whenComplete {
            tryOpenPlayerIslandMenu((pp as BukkitPlayer).player, false)
        }
    }

    fun removeIsland(pp: PlotPlayer<*>, plot: Plot): Promise<Unit, java.lang.Exception> {
        val data = StorageManager.getPlayerIsland(pp.uuid)
        return Promise {
            if (data != null) {
                val loc =
                    Location(
                        plot.worldName!!.asWorld(),
                        data.coreX.toDouble(),
                        data.coreY.toDouble(),
                        data.coreZ.toDouble()
                    )
                PaperLib.getChunkAtAsync(loc, true).thenAccept {
                    MyIslands.INSTANCE.runTaskBR {
                        loc.block.type = Material.AIR
                        MyIslands.plotUtils.removeIsland(pp, plot).whenComplete {
                            StorageManager.removePlayerIsland(data)
                            resolve(Unit)
                        }
                    }
                }
            } else {
                MyIslands.plotUtils.removeIsland(pp, plot)
                resolve(Unit)
            }
        }
    }

    fun getIslandAbs(player: Player): PlayerIsland? {
        val plot = MyIslands.plotUtils.getPlotByPLoc(player)
        if (plot == null || plot.owner == null) return null
        return StorageManager.getPlayerIsland(plot.owner!!)
    }

    fun hasPermission(player: Player): Boolean {
        val plot = MyIslands.plotUtils.getPlotByPLoc(player)
        if (plot == null || plot.owner == null) return false
        if (plot.owner == player.uuid) return true
        return StorageManager.hasPermission(player, plot.owner!!)
    }

    fun tryOpenPlayerIslandMenu(player: Player, join: Boolean = true) {
        MyIslands.INSTANCE.runTaskLaterBR(20L) {
            val plotPlayer = MyIslands.plotAPI.wrapPlayer(player.uuid)
            if (plotPlayer == null) {
                player.errorMsg("未知错误 MI0x1")
                return@runTaskLaterBR
            }
            val plots = MyIslands.plotAPI.getPlayerPlots(plotPlayer)
            if (plots.isNotEmpty()) {
                if (join) {
                    joinPlayers[player.uuid] = plots.first()
                }
                MyIslands.INSTANCE.runTaskLaterBR(10L) {
                    plots.first().getHome {
                        plotPlayer.teleport(it)
                    }
                }
                return@runTaskLaterBR
            }
            MyIslands.INSTANCE.runTaskLaterBR(10L) {
                IslandsChooseMenu().open(player)
            }
        }
    }

    fun handleCMITP(player: Player, event: Cancellable) {
        if (joinPlayers.containsKey(player.uuid)) {
            joinPlayers.remove(player.uuid)
            event.isCancelled = true
        }
    }

    fun createPlayerIsland(player: Player, plot: Plot, plotSchematic: ConfigManager.PlotSchematicConfig) {
        val coreVector = plotSchematic.core

        val dLoc = plot.bottomAbs
        val x = dLoc.x - coreVector.blockX
        val y = dLoc.y - coreVector.blockY
        val z = dLoc.z - coreVector.blockZ
        val world = Bukkit.getWorld(dLoc.world.name)!!
        val loc = Location(
            world,
            x.toDouble(), y.toDouble(), z.toDouble()
        )
        PaperLib.getChunkAtAsync(loc, true).thenAccept {
            MyIslands.INSTANCE.runTaskBR {
                setupPlotCore(loc)
                val homeLoc = lookAtLoc(loc.clone().add(-3.0, 0.0, 0.5), loc.clone().add(0.0, -1.0, 0.5))
                setPlotHome(plot, homeLoc)
                player.teleport(homeLoc)
                StorageManager.createPlayerIsland(player.uniqueId, loc.let { Vector(it.x, it.y, it.z) })
                player.successMsg("成功领取你的私人岛屿")
            }
        }
    }

    private fun setupPlotCore(loc: Location) {
        MyIslands.INSTANCE.runTaskLaterBR(5 * 20L) {
            CustomBlock.place(ConfigManager.coreId, loc)
        }
    }

    fun setPlotHome(plot: Plot, loc: Location) {
        val bottomAbs = plot.bottomAbs
        plot.setHome(
            BlockLoc(
                loc.blockX - bottomAbs.x,
                loc.blockY,
                loc.blockZ - bottomAbs.z,
                loc.yaw,
                loc.pitch
            )
        )
    }

    fun lookAtLoc(locA: Location, locB: Location): Location {
        val xDiff = locB.x - locA.x
        val yDiff = locB.y - locA.y
        val zDiff = locB.z - locA.z

        val distanceXZ = sqrt(xDiff * xDiff + zDiff * zDiff)
        val distanceY = sqrt(distanceXZ * distanceXZ + yDiff * yDiff)
        // xDiff(邻边) / distanceXZ(斜边) = cos(theta)
        var yaw = Math.toDegrees(acos(xDiff / distanceXZ))
        // yDiff(邻边) / distanceY(斜边) = cos(theta)
        val pitch = Math.toDegrees(acos(yDiff / distanceY)) - 90
//                    println(pitch)
        if (zDiff < 0.0) {
            yaw += abs(180.0 - yaw) * 2.0
        }
        return Location(locA.world, locA.x, locA.y, locA.z, yaw.toFloat() - 90f, pitch.toFloat())
    }

}