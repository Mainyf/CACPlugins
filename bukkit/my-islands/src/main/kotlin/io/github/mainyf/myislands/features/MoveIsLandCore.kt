package io.github.mainyf.myislands.features

import com.comphenix.protocol.events.PacketEvent
import com.plotsquared.core.plot.Plot
import dev.lone.itemsadder.api.CustomStack
import io.github.mainyf.myislands.IslandsManager
import io.github.mainyf.myislands.MyIslands
import io.github.mainyf.myislands.asPlotPlayer
import io.github.mainyf.myislands.config.ConfigMI
import io.github.mainyf.myislands.config.sendLang
import io.github.mainyf.myislands.storage.PlayerIsland
import io.github.mainyf.myislands.storage.StorageMI
import io.github.mainyf.newmclib.exts.onlinePlayers
import io.github.mainyf.newmclib.exts.runTaskLaterBR
import io.github.mainyf.newmclib.exts.submitTask
import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.newmclib.nms.addLeftClickListener
import io.github.mainyf.newmclib.nms.sendEntityDestroy
import io.github.mainyf.newmclib.nms.sendEntityMeta
import io.github.mainyf.newmclib.nms.sendSpawnArmorStand
import io.github.mainyf.newmclib.utils.Cooldown
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.util.Vector
import java.util.*

object MoveIsLandCore : Listener {

    private val moveingCooldown = Cooldown()
    private val interactCooldown = Cooldown()
    private val moveingCorePlayer = mutableMapOf<UUID, PlayerIsland>()
    private val playerToPlot = mutableMapOf<UUID, Plot>()
    private val playerTempCoreLoc = mutableMapOf<UUID, Pair<Location, Int>>()

    private val playerSetLater20tick = mutableSetOf<UUID>()

    val playerCoreRemove = mutableSetOf<UUID>()


    fun init() {
        MyIslands.INSTANCE.addLeftClickListener { player, packetEvent ->
            interactCooldown.invoke(player.uuid, 200L, {
                handlePlayerInteract(player, packetEvent)
            }, {})
        }
        MyIslands.INSTANCE.submitTask(
            delay = 0,
            period = 20
        ) {
            onlinePlayers().forEach {
                if (moveingCorePlayer.containsKey(it.uuid)) {
                    ConfigMI.moveCoreAction?.execute(it)
                }
            }
        }
    }

    fun hasMoveingCore(uuid: UUID): Boolean {
        return moveingCorePlayer.containsKey(uuid)
    }

    fun hasInPlaceLater20tick(uuid: UUID): Boolean {
        return playerSetLater20tick.contains(uuid)
    }

    fun tryStartMoveCore(player: Player, plot: Plot) {
        if (playerToPlot.values.any { it == plot }) {
            player.sendLang("alreadyMoveingCore")
            return
        }

//        val plot = MyIslands.plotUtils.getPlotByPLoc(player)
//        if (hasMoveingCore(player.uniqueId)) {
//            player.sendLang("alreadyMoveingCore")
////            player.errorMsg("你正在移动你的核心")
//            return
//        }
//        if (plot == null) {
//            player.sendLang("playerLocNotPlot")
////            player.errorMsg("你的脚下没有地皮")
//            return
//        }
//        val owner = plot.owner
//        if (owner != player.uniqueId) {
//            player.sendLang("noOwnerMoveCore")
////            player.errorMsg("你不是脚下地皮的主人")
//            return
//        }
        val islandData = IslandsManager.getIslandData(plot.owner!!)
        if (islandData == null) {
            player.sendLang("tryMoveCoreButPlayerNotHaveIsland")
//            player.errorMsg("意外的错误: 0xMI1")
            return
        }
        startMoveCore(player, islandData, plot)
        player.sendLang("startMoveCore")
//        player.successMsg("开始移动核心水晶")
    }

    fun startMoveCore(player: Player, islandData: PlayerIsland, plot: Plot) {
        moveingCorePlayer[player.uuid] = islandData
        playerToPlot[player.uuid] = plot
        playerSetLater20tick.add(player.uuid)
    }

    fun endMoveCore(player: Player) {
        moveingCorePlayer.remove(player.uniqueId)
        playerToPlot.remove(player.uniqueId)
        playerTempCoreLoc.remove(player.uniqueId)
    }

    fun removePlayerLater20Tick(player: Player) {
        playerSetLater20tick.remove(player.uuid)
    }

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        val player = event.player
        if (!moveingCorePlayer.containsKey(player.uniqueId)) return
        moveingCooldown.invoke(player.uniqueId, 100L, {
            if (!checkPlayerLoc(player)) {
                return@invoke
            }
            val block = event.player.getTargetBlock(6)
            if (block != null && block.type != Material.AIR) {
                placeFakeBlock(player, block.location.add(0.5, 1.0, 0.5))
            }
        })
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        if (!moveingCorePlayer.containsKey(player.uniqueId)) return
//        moveingCorePlayer.remove(player.uuid)
//        playerToPlot.remove(player.uuid)
//        playerTempCoreLoc.remove(player.uuid)
        endMoveCore(player)
        removePlayerLater20Tick(player)
    }

    private fun checkPlayerLoc(player: Player): Boolean {
        val plotAbsOwner = player.asPlotPlayer()?.location?.plotAbs?.owner
        if (plotAbsOwner != playerToPlot[player.uuid]?.ownerAbs) {
            player.sendLang("moveCoreToOutIsland")
            endMoveCore(player)
            removePlayerLater20Tick(player)
            return false
        }
        return true
    }

    private fun handlePlayerInteract(player: Player, event: PacketEvent): Boolean {
        if (!moveingCorePlayer.containsKey(player.uniqueId)) return false
        if (!playerToPlot.containsKey(player.uniqueId)) return false
        val islandData = moveingCorePlayer[player.uuid] ?: return false
        if (player.isSneaking) {
            if (playerTempCoreLoc.containsKey(player.uuid)) {
                playerTempCoreLoc.remove(player.uuid)?.second?.let {
                    deleteEntity(player, it)
                }
            }
            endMoveCore(player)
            removePlayerLater20Tick(player)
            player.sendLang("endMoveCore")
            event.isCancelled = true
//            player.successMsg("结束移动核心水晶")
//            event.isCancelled = true
            return true
        }
        if (!playerTempCoreLoc.containsKey(player.uniqueId)) return false

        if (!checkPlayerLoc(player)) {
            return false
        }
        event.isCancelled = true

        val (newLoc, entityID) = playerTempCoreLoc[player.uniqueId]!!
        val homeLoc = IslandsManager.getHomeLoc(newLoc)
        if (MyIslands.plotUtils.hasDanger(homeLoc, -1.0)) {
            player.sendLang("setupIslandCoreDanger")
            return false
        }
        val entities = IslandsManager.getIslandCoreEntity(islandData)
        IslandsManager.markMoveingIslandCore(islandData.id.value)
        entities.forEach {
            IslandsManager.deleteIslandCore(player, it)
        }
        deleteEntity(player, entityID)
        StorageMI.updateCoreLoc(islandData.id.value, newLoc.toVector())
        IslandsManager.setupPlotCore(newLoc)
        MyIslands.INSTANCE.submitTask(delay = 40L) {
            IslandsManager.cleanMoveingIslandCore(islandData.id.value)
        }
        IslandsManager.setPlotHome(playerToPlot[player.uniqueId]!!, homeLoc)
        player.teleport(homeLoc)
        player.velocity = Vector(0, 0, 0)
        player.sendLang("moveCoreSuccess")
//        player.successMsg("设置完成，新的水晶已放置，出生点已刷新")
        endMoveCore(player)
        MyIslands.INSTANCE.runTaskLaterBR(20L) {
            removePlayerLater20Tick(player)
        }
        return true
    }

    fun markCoreRemove(player: Player) {
        playerCoreRemove.add(player.uuid)
    }

    fun unMarkCoreRemove(player: Player) {
        playerCoreRemove.remove(player.uuid)
    }

    private fun placeFakeBlock(player: Player, loc: Location) {
        if (playerTempCoreLoc.containsKey(player.uuid)) {
            val (prevLoc, _) = playerTempCoreLoc[player.uuid]!!
            if (loc.blockX == prevLoc.blockX && loc.blockY == prevLoc.blockY && loc.blockZ == prevLoc.blockZ) {
                return
            }
            playerTempCoreLoc.remove(player.uuid)?.second?.let {
                deleteEntity(player, it)
            }
        }
        val furniture = spawnFakeArmorStand(player, loc)
        playerTempCoreLoc[player.uuid] = loc to furniture
    }

    fun spawnFakeArmorStand(p: Player, loc: Location): Int {
        val cas = p.sendSpawnArmorStand(loc) {
            equipment.helmet = CustomStack.getInstance(ConfigMI.coreId)!!.itemStack
        }
        p.sendEquipmentChange(cas, EquipmentSlot.HEAD, CustomStack.getInstance(ConfigMI.coreId)!!.itemStack)
        p.sendEntityMeta(cas)
        return cas.entityId
    }

    fun deleteEntity(p: Player, id: Int) {
        p.sendEntityDestroy(id)
    }


}