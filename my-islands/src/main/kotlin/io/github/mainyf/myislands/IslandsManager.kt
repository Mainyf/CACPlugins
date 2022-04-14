package io.github.mainyf.myislands

import com.plotsquared.bukkit.paperlib.PaperLib
import com.plotsquared.core.location.BlockLoc
import com.plotsquared.core.plot.Plot
import dev.lone.itemsadder.api.CustomBlock
import io.github.mainyf.myislands.config.ConfigManager
import io.github.mainyf.myislands.storage.StorageManager
import io.github.mainyf.newmclib.exts.runTaskBR
import io.github.mainyf.newmclib.exts.successMsg
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.util.Vector

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
                val homeLoc = loc.clone().subtract(4.0, 0.0, 0.0)
                setPlotHome(plot, homeLoc)
                player.teleport(homeLoc)
                StorageManager.createPlayerIsLand(player.uniqueId, loc.let { Vector(it.x, it.y, it.z) })
                player.successMsg("成功领取你的私人岛屿")
            }
        }
    }

    private fun setupPlotCore(loc: Location) {
        val coreBlock = CustomBlock.getInstance("itemsadder:lit_campfire")!!
        coreBlock.place(loc)
//        world.getBlockAt(loc).type = Material.EMERALD_BLOCK
    }

    private fun setPlotHome(plot: Plot, loc: Location) {
        val bottomAbs = plot.bottomAbs
        plot.setHome(
            BlockLoc(
                (loc.blockX - 4) - bottomAbs.x,
                loc.blockY,
                loc.blockZ - bottomAbs.z,
                loc.yaw,
                loc.pitch
            )
        )
    }

}