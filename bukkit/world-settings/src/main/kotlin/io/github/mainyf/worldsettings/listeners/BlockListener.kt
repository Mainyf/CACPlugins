package io.github.mainyf.worldsettings.listeners

import io.github.mainyf.worldsettings.getWorldSetting
import io.github.mainyf.worldsettings.ignorePermAndGetWorldSettings
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerPortalEvent
import org.bukkit.event.world.PortalCreateEvent

object BlockListener : Listener {

    @EventHandler
    fun onAnvilsDamage(event: BlockDamageEvent) {
        val player = event.player
        if (event.block.type != Material.ANVIL) return
        val settings = getWorldSetting(event.block.location) ?: return
        if (settings.antiAnvilsDamage) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlace(event: BlockPlaceEvent) {
        val player = event.player
        ignorePermAndGetWorldSettings(player, location = event.block.location) { settings ->
            if (settings.antiPlaceBlock) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        val player = event.player
        ignorePermAndGetWorldSettings(player, location = event.block.location) { settings ->
            if (settings.antiBreakBlock) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onCreatePortal(event: PortalCreateEvent) {
        val entity = event.entity
        ignorePermAndGetWorldSettings(entity) { settings ->
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

    @EventHandler
    fun onExplode(event: EntityExplodeEvent) {
        ignorePermAndGetWorldSettings(event.entity, event.location) { settings ->
            if (settings.antiBlockExplode) {
                event.blockList().clear()
            }
        }
    }

    @EventHandler
    fun onBlockExplode(event: BlockExplodeEvent) {
        ignorePermAndGetWorldSettings(null, event.block.location) { settings ->
            if (settings.antiBlockExplode) {
                event.isCancelled = true
            }
        }
    }

}