package io.github.mainyf.worldsettings.listeners

import io.github.mainyf.worldsettings.config.ConfigManager
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.world.PortalCreateEvent

object BlockListener : Listener {

//    @EventHandler
//    fun onBlockIgnite(event: BlockIgniteEvent) {
//        if (event.cause != IgniteCause.FLINT_AND_STEEL) {
//            val settings = ConfigManager.getSetting(event.block.world)
//            if (settings != null && settings.antiFireSpread) {
//                event.isCancelled = true
//            }
//        }
//    }
//
//    @EventHandler
//    fun onBurn(event: BlockBurnEvent) {
//        val settings = ConfigManager.getSetting(event.block.world)
//        if (settings != null && settings.antiFireSpread) {
//            event.isCancelled = true
//        }
//    }

    @EventHandler
    fun onPlace(event: BlockPlaceEvent) {
        if (event.player.hasPermission(ConfigManager.ignorePermission)) return
        val settings = ConfigManager.getSetting(event.block.world)
        if (settings != null && settings.antiPlaceBlock) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        if (event.player.hasPermission(ConfigManager.ignorePermission)) return
        val settings = ConfigManager.getSetting(event.block.world)
        if (settings != null && settings.antiBreakBlock) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onCreatePortal(event: PortalCreateEvent) {
        val entity = event.entity
        if (entity is Player) {
            if (entity.hasPermission(ConfigManager.ignorePermission)) return
        }
        val settings = ConfigManager.getSetting(event.world)
        if (settings != null && settings.antiCreatePortal) {
            event.isCancelled = true
        }
    }

}