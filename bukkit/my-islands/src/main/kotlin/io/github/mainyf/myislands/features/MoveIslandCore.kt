package io.github.mainyf.myislands.features

import com.plotsquared.bukkit.util.BukkitUtil
import com.plotsquared.core.plot.Plot
import dev.lone.itemsadder.api.CustomBlock
import io.github.mainyf.myislands.IslandsManager
import io.github.mainyf.myislands.config.ConfigManager
import io.github.mainyf.myislands.storage.PlayerIslandData
import io.github.mainyf.myislands.storage.StorageManager
import io.github.mainyf.newmclib.exts.errorMsg
import io.github.mainyf.newmclib.exts.successMsg
import io.github.mainyf.newmclib.utils.Cooldown
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.util.Vector
import java.util.*

object MoveIslandCore : Listener {

    private val cooldown = Cooldown()
    private val moveingCorePlayer = mutableMapOf<UUID, PlayerIslandData>()
    private val playerToPlot = mutableMapOf<UUID, Plot>()
    private val playerTempCoreLoc = mutableMapOf<UUID, Location>()

    fun hasMoveingCore(uuid: UUID): Boolean {
        return moveingCorePlayer.containsKey(uuid)
    }

    fun startMoveCore(player: Player, islandData: PlayerIslandData, plot: Plot) {
        moveingCorePlayer[player.uniqueId] = islandData
        playerToPlot[player.uniqueId] = plot
    }

    fun endMoveCore(player: Player) {
        moveingCorePlayer.remove(player.uniqueId)
        playerToPlot.remove(player.uniqueId)
        playerTempCoreLoc.remove(player.uniqueId)
    }

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        val player = event.player
        if (!moveingCorePlayer.containsKey(player.uniqueId)) return
        cooldown.invoke(player.uniqueId, 30L, {
            val block = event.player.getTargetBlock(20)
            if (block != null) {
                placeFakeBlock(player, block.location.add(0.0, 1.0, 0.0))
            }
        })
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!moveingCorePlayer.containsKey(player.uniqueId)) return
        if (!playerToPlot.containsKey(player.uniqueId)) return
        if (!playerTempCoreLoc.containsKey(player.uniqueId)) return
        val islandData = StorageManager.getPlayerIsland(player.uniqueId) ?: return
        if (event.action == Action.LEFT_CLICK_AIR || event.action == Action.LEFT_CLICK_BLOCK) {
            val newLoc = playerTempCoreLoc[player.uniqueId]!!
            val oldLoc = Location(
                Bukkit.getWorld("plotworld"),
                islandData.coreX.toDouble(),
                islandData.coreY.toDouble(),
                islandData.coreZ.toDouble()
            )
            oldLoc.block.type = Material.AIR
            StorageManager.updateCoreLoc(player.uniqueId, newLoc.toVector())
            CustomBlock.place(ConfigManager.coreId, newLoc)
            val homeLoc =
                IslandsManager.lookAtLoc(newLoc.clone().add(-3.0, 0.0, 0.5), newLoc.clone().add(0.0, -1.0, 0.5))
            IslandsManager.setPlotHome(playerToPlot[player.uniqueId]!!, homeLoc)
            player.teleport(homeLoc)
            player.velocity = Vector(0, 0, 0)
            player.successMsg("设置完成，新的水晶已放置，出生点已刷新")
            endMoveCore(player)
            event.isCancelled = true
        }
    }

    private fun placeFakeBlock(player: Player, loc: Location) {
        val cBlock = CustomBlock.getInstance(ConfigManager.coreId)!!
        val plot = playerToPlot[player.uniqueId]!!
        if (!plot.area!!.contains(BukkitUtil.adaptComplete(loc))) {
            player.errorMsg("你不能把水晶设置在岛屿外部")
            return
        }
        if (playerTempCoreLoc.containsKey(player.uniqueId)) {
            player.sendBlockChange(playerTempCoreLoc[player.uniqueId]!!, Bukkit.createBlockData(Material.AIR))
            playerTempCoreLoc.remove(player.uniqueId)
        }
        player.sendBlockChange(loc, cBlock.baseBlockData!!)
        playerTempCoreLoc[player.uniqueId] = loc
    }


}