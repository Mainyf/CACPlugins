package io.github.mainyf.myislands.listeners

import dev.lone.itemsadder.api.CustomFurniture
import io.github.mainyf.myislands.IslandsManager
import io.github.mainyf.myislands.MyIslands
import io.github.mainyf.myislands.config.sendLang
import io.github.mainyf.myislands.features.MoveIslandCore
import io.github.mainyf.myislands.menu.IslandsMainMenu
import io.github.mainyf.newmclib.exts.getShooterPlayer
import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.newmclib.utils.Cooldown
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent

object PlayerListeners : Listener {

    private val cooldown = Cooldown()

    @EventHandler(ignoreCancelled = true)
    fun onArmorStand(event: EntityDamageByEntityEvent) {
        if (event.entity is ArmorStand) {
            val furniture = CustomFurniture.byAlreadySpawned(event.entity as ArmorStand)
            if (furniture != null) {
                val damager = event.damager
                if (damager is Player && MoveIslandCore.playerCoreRemove.contains(damager.uuid)) {
                    val plot = MyIslands.plotUtils.getPlotByPLoc(damager)
                    if (plot != null && plot.owner == damager.uuid) {
                        MoveIslandCore.playerCoreRemove.remove(damager.uuid)
//                        return
                    }
                }

                event.damager.getShooterPlayer()?.sendLang("antiPlayerBreakCore")
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onInteractAE(event: PlayerInteractAtEntityEvent) {
        val entity = event.rightClicked
        if (entity is ArmorStand) {
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
        if (plot != null && !IslandsManager.hasPermission(player, plot)) {
            player.sendLang("dropItemByNonHelperIsland")
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPickup(event: EntityPickupItemEvent) {
        val entity = event.entity
        if (entity is Player) {
            val plot = MyIslands.plotUtils.getPlotByPLoc(entity)
            if (plot != null && !IslandsManager.hasPermission(entity, plot)) {
                cooldown.invoke(entity.uuid, 1000L, {
                    entity.sendLang("pickupByNonHelperIsland")
                })
                event.isCancelled = true
            }
        }
    }

}