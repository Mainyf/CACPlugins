package io.github.mainyf.myislands

import com.plotsquared.bukkit.paperlib.PaperLib
import com.plotsquared.core.location.BlockLoc
import com.plotsquared.core.plot.Plot
import dev.lone.itemsadder.api.CustomBlock
import io.github.mainyf.myislands.config.ConfigManager
import io.github.mainyf.myislands.storage.StorageManager
import io.github.mainyf.newmclib.exts.runTaskBR
import io.github.mainyf.newmclib.exts.runTaskLaterBR
import io.github.mainyf.newmclib.exts.successMsg
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

object IslandsManager {

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