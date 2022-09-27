package io.github.mainyf.worldsettings.listeners

import io.github.mainyf.worldsettings.config.ConfigManager
import io.github.mainyf.worldsettings.ignorePermAndGetWorldSettings
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerPortalEvent
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
    fun onAnvilsDamage(event: BlockDamageEvent) {
        val player = event.player
        if (event.block.type != Material.ANVIL) return
        val world = player.world
        val settings = ConfigManager.getSetting(world) ?: return
        if(settings.antiAnvilsDamage) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlace(event: BlockPlaceEvent) {
        val player = event.player
        ignorePermAndGetWorldSettings(player) { settings ->
            if (settings.antiPlaceBlock) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        val player = event.player
        ignorePermAndGetWorldSettings(player) { settings ->
            if (settings.antiBreakBlock) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onCreatePortal(event: PortalCreateEvent) {
        val entity = event.entity
        ignorePermAndGetWorldSettings(entity, event.world) { settings ->
            if (settings.antiCreatePortal) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onPortal(event: PlayerPortalEvent) {
        val player = event.player
        ignorePermAndGetWorldSettings(player) { settings ->
            if (settings.antiCreatePortal) {
                event.canCreatePortal = false
                event.isCancelled = true
            }
        }
    }

}