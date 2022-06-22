package io.github.mainyf.myislands.features

import com.comphenix.protocol.PacketType
import com.plotsquared.bukkit.util.BukkitUtil
import com.plotsquared.core.plot.Plot
import dev.lone.itemsadder.api.CustomFurniture
import dev.lone.itemsadder.api.CustomStack
import io.github.mainyf.myislands.IslandsManager
import io.github.mainyf.myislands.MyIslands
import io.github.mainyf.myislands.config.ConfigManager
import io.github.mainyf.myislands.config.sendLang
import io.github.mainyf.myislands.storage.PlayerIsland
import io.github.mainyf.myislands.storage.StorageManager
import io.github.mainyf.newmclib.exts.runTaskBR
import io.github.mainyf.newmclib.exts.runTaskLaterBR
import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.newmclib.hooks.registerPacketListener
import io.github.mainyf.newmclib.nms.sendEntityDestroy
import io.github.mainyf.newmclib.nms.sendEntityMeta
import io.github.mainyf.newmclib.nms.sendSpawnArmorStand
import io.github.mainyf.newmclib.utils.Cooldown
import net.minecraft.world.EnumHand
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.util.Vector
import java.util.*

object MoveIslandCore : Listener {

    private val cooldown = Cooldown()
    private val moveingCorePlayer = mutableMapOf<UUID, PlayerIsland>()
    private val playerToPlot = mutableMapOf<UUID, Plot>()
    private val playerTempCoreLoc = mutableMapOf<UUID, Pair<Location, Int>>()

    private val playerSetLater20tick = mutableSetOf<UUID>()

    fun init() {
        MyIslands.INSTANCE.registerPacketListener(PacketType.Play.Client.ARM_ANIMATION) { event ->
            val enumHand = event.packet.modifier.read(0) as? EnumHand
            if (enumHand == EnumHand.a) {
                MyIslands.INSTANCE.runTaskBR {
                    handlePlayerInteract(event.player)
                }
            }
        }
//        protocolManager().addPacketListener(object :
//            PacketAdapter(MyIslands.INSTANCE, PacketType.Play.Client.ARM_ANIMATION) {
//
//            override fun onPacketReceiving(event: PacketEvent) {
//                val enumHand = event.packet.modifier.read(0) as? EnumHand
//                if (enumHand == EnumHand.a) {
//                    MyIslands.INSTANCE.runTaskBR {
//                        handlePlayerInteract(event.player)
//                    }
//                }
//            }
//
//        })
    }

    fun hasMoveingCore(uuid: UUID): Boolean {
        return moveingCorePlayer.containsKey(uuid)
    }

    fun hasInPlaceLater20tick(uuid: UUID): Boolean {
        return playerSetLater20tick.contains(uuid)
    }

    fun tryStartMoveCore(player: Player) {
        val plot = MyIslands.plotUtils.getPlotByPLoc(player)
        if (hasMoveingCore(player.uniqueId)) {
            player.sendLang("alreadyMoveingCore")
//            player.errorMsg("你正在移动你的核心")
            return
        }
        if (plot == null) {
            player.sendLang("playerLocNotPlot")
//            player.errorMsg("你的脚下没有地皮")
            return
        }
        val owner = plot.owner
        if (owner != player.uniqueId) {
            player.sendLang("playerNotPlotOwner")
//            player.errorMsg("你不是脚下地皮的主人")
            return
        }
        val islandData = StorageManager.getPlayerIsland(player.uniqueId)
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
        cooldown.invoke(player.uniqueId, 100L, {
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

    private fun handlePlayerInteract(player: Player): Boolean {
        if (!moveingCorePlayer.containsKey(player.uniqueId)) return false
        if (!playerToPlot.containsKey(player.uniqueId)) return false
        val islandData = StorageManager.getPlayerIsland(player.uniqueId) ?: return false
        if (player.isSneaking) {
            if (playerTempCoreLoc.containsKey(player.uuid)) {
                playerTempCoreLoc.remove(player.uuid)?.second?.let {
                    deleteEntity(player, it)
                }
            }
            endMoveCore(player)
            removePlayerLater20Tick(player)
            player.sendLang("endMoveCore")
//            player.successMsg("结束移动核心水晶")
//            event.isCancelled = true
            return true
        }
        if (!playerTempCoreLoc.containsKey(player.uniqueId)) return false
        val (newLoc, entityID) = playerTempCoreLoc[player.uniqueId]!!
        val world = Bukkit.getWorld("plotworld")!!
        val oldLoc = Location(
            world,
            islandData.coreX.toDouble(),
            islandData.coreY.toDouble(),
            islandData.coreZ.toDouble()
        )
        world.entities.filter {
            val eLoc = it.location
            eLoc.blockX == oldLoc.blockX && eLoc.blockY == oldLoc.blockY && eLoc.blockZ == oldLoc.blockZ
        }.forEach {
            if (it is ArmorStand) {
                CustomFurniture.remove(it, false)
            }
        }
        deleteEntity(player, entityID)
//            oldLoc.block.type = Material.AIR
        StorageManager.updateCoreLoc(player.uniqueId, newLoc.toVector())
        IslandsManager.setupPlotCore(newLoc)
//            CustomBlock.place(ConfigManager.coreId, newLoc)
        val homeLoc = IslandsManager.getHomeLoc(newLoc)
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

//    @EventHandler(ignoreCancelled = false)
//    fun onInteract(event: PlayerInteractEvent) {
//        val player = event.player
//    }

    private fun placeFakeBlock(player: Player, loc: Location) {
//        arrayOf(
//            loc.clone().add(-1.0, 0.0, 0.0),
//            loc.clone().add(0.0, 0.0, 0.0)
//        )

        val plot = playerToPlot[player.uniqueId]!!
        if (!plot.area!!.contains(BukkitUtil.adaptComplete(loc))) {
            player.sendLang("coreNotPlaceOutIsland")
//            player.errorMsg("你不能把水晶设置在岛屿外部")
            return
        }

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
            equipment.helmet = CustomStack.getInstance(ConfigManager.coreId)!!.itemStack
        }
        p.sendEquipmentChange(cas, EquipmentSlot.HEAD, CustomStack.getInstance(ConfigManager.coreId)!!.itemStack)
        p.sendEntityMeta(cas)
        return cas.entityId
    }

    fun deleteEntity(p: Player, id: Int) {
        p.sendEntityDestroy(id)
    }


}