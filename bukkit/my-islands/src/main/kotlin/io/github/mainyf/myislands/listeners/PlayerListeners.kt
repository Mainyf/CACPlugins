package io.github.mainyf.myislands.listeners

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import com.plotsquared.bukkit.util.BukkitUtil
import dev.lone.itemsadder.api.CustomFurniture
import io.github.mainyf.myislands.IslandsManager
import io.github.mainyf.myislands.MyIslands
import io.github.mainyf.myislands.config.sendLang
import io.github.mainyf.myislands.features.MoveIsLandCore
import io.github.mainyf.myislands.menu.IslandsMainMenu
import io.github.mainyf.newmclib.exts.getShooterPlayer
import io.github.mainyf.newmclib.exts.submitTask
import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.newmclib.utils.Cooldown
import org.bukkit.entity.Animals
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerRespawnEvent

object PlayerListeners : Listener {

    private val cooldown = Cooldown()

    @EventHandler(ignoreCancelled = true)
    fun onArmorStand(event: EntityDamageByEntityEvent) {
        if (event.entity is ArmorStand) {
            val furniture = CustomFurniture.byAlreadySpawned(event.entity as ArmorStand)
            if (furniture != null) {
                val damager = event.damager
                if (damager is Player && MoveIsLandCore.playerCoreRemove.contains(damager.uuid)) {
                    val plot = MyIslands.plotUtils.getPlotByPLoc(damager)
                    if (plot != null && plot.owner == damager.uuid) {
                        MoveIsLandCore.playerCoreRemove.remove(damager.uuid)
//                        return
                    }
                }

                event.damager.getShooterPlayer()?.sendLang("antiPlayerBreakCore")
                event.isCancelled = true
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onDamage(event: EntityDamageByEntityEvent) {
        val damager = event.damager.getShooterPlayer() ?: return
        val victims = event.entity
        val vLoc = BukkitUtil.adaptComplete(victims.location)
        val plot = vLoc.plot ?: return
        if (!IslandsManager.hasPermission(damager, plot)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onInteractAE(event: PlayerInteractAtEntityEvent) {
        val entity = event.rightClicked
        if (entity is ArmorStand && IslandsManager.isIslandCore(entity)) {
            val player = event.player
            val plot = MyIslands.plotUtils.getPlotByPLoc(player)
            if (plot?.owner != null) {
                val islandData = IslandsManager.getIslandData(plot.owner!!)
                if (islandData != null) {
                    val entities = IslandsManager.getIslandCoreEntity(islandData)
                    if (!entities.contains(entity)) {
                        IslandsManager.deleteIslandCore(player, entity)
                        MyIslands.LOGGER.info("玩家 ${event.player.name} 与错误的岛屿核心互动了，错误核心已被删除，位置: x: ${islandData.coreX} y: ${islandData.coreY} z: ${islandData.coreZ}")
                        return
                    }
                }
            }
            val furniture = CustomFurniture.byAlreadySpawned(entity)
            if (furniture != null) {
                IslandsMainMenu().open(event.player)
            }
        }
    }

    @EventHandler
    fun onDrop(event: PlayerDropItemEvent) {
        val player = event.player
        val plot = MyIslands.plotUtils.getPlotByPLoc(player)
        if (plot != null) {
            if (!IslandsManager.hasPermission(player, plot)) {
                player.sendLang("dropItemByNonHelperIsland")
                event.isCancelled = true
            }
        } else if (player.world.name == "plotworld") {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPickup(event: EntityPickupItemEvent) {
        val entity = event.entity
        if (entity is Player) {
            val plot = MyIslands.plotUtils.getPlotByPLoc(entity)
            if (plot != null) {
                if (!IslandsManager.hasPermission(entity, plot)) {
                    cooldown.invoke(entity.uuid, 1000L, {
                        entity.sendLang("pickupByNonHelperIsland")
                    })
                    event.isCancelled = true
                }
            }
        }
    }

    @EventHandler
    fun onRespawn(event: PlayerRespawnEvent) {
        val islandData = IslandsManager.getIslandData(event.player.uuid) ?: return
        val coreLoc = IslandsManager.getIslandCoreLoc(islandData)
        val homeLoc = IslandsManager.getHomeLoc(coreLoc)
        event.respawnLocation = MyIslands.plotUtils.findSafeLoc(event.player, homeLoc, coreLoc)
        MyIslands.INSTANCE.submitTask(delay = 1L) {
            MyIslands.plotUtils.teleportHomePlot(event.player)
        }
    }

}