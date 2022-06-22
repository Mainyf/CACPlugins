package io.github.mainyf.worldsettings.listeners

import io.github.mainyf.newmclib.exts.errorMsg
import io.github.mainyf.newmclib.exts.getShooterPlayer
import io.github.mainyf.newmclib.offline_player_ext.asOfflineData
import io.github.mainyf.worldsettings.PlayerDropItemStorage
import io.github.mainyf.worldsettings.config.CommandMatchType.LIKE
import io.github.mainyf.worldsettings.config.CommandMatchType.START
import io.github.mainyf.worldsettings.config.ConfigManager
import io.github.mainyf.worldsettings.getWorldSettings
import io.github.mainyf.worldsettings.ignorePermAndGetWorldSettings
import org.bukkit.Material
import org.bukkit.entity.*
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.player.*
import org.bukkit.event.server.TabCompleteEvent


object PlayerListener : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        getWorldSettings(event.player) { settings ->
            settings.joinServerAction?.execute(event.player)
        }
    }

    @EventHandler
    fun onRespawn(event: PlayerRespawnEvent) {
        getWorldSettings(event.player) { settings ->
            settings.respawnAction?.execute(event.player)
        }
    }

    @EventHandler
    fun onCommand(event: PlayerCommandPreprocessEvent) {
        val player = event.player
        ignorePermAndGetWorldSettings(player) { settings ->
            val command = event.message
            if (settings.commandWhite.isEmpty()) return@ignorePermAndGetWorldSettings
            var cancel = true
            settings.commandWhite.forEach { (type, cmd) ->
                when (type) {
                    LIKE -> {
                        if (command.contains(cmd)) {
                            cancel = false
                            return@ignorePermAndGetWorldSettings
                        }
                    }
                    START -> {
                        if (command.startsWith(cmd)) {
                            cancel = false
                            return@ignorePermAndGetWorldSettings
                        }
                    }
                }
            }
            if (cancel) {
                settings.commandBlockAction?.execute(player)
                event.isCancelled = true
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun handlePlayerGameMode(event: PlayerTeleportEvent) {
        val player = event.player
        val world = event.to.world
        ignorePermAndGetWorldSettings(player, world) { settings ->
            if (player.gameMode != settings.gameMode) {
                player.gameMode = settings.gameMode
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerModeChange(event: PlayerGameModeChangeEvent) {
        val player = event.player
        ignorePermAndGetWorldSettings(player) { settings ->
            if (event.newGameMode != settings.gameMode) {
                player.errorMsg("当前世界不允许更改游戏模式")
                event.isCancelled = true
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onToggleFlight(event: PlayerToggleFlightEvent) {
        val player = event.player

        ignorePermAndGetWorldSettings(player) { settings ->
            if (settings.antiFly) {
                player.allowFlight = false
                player.isFlying = false
                settings.flyBlockAction?.execute(player)
                event.isCancelled = true
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    fun onDamage(event: EntityDamageByEntityEvent) {
        val damager = event.damager.getShooterPlayer() ?: return
        ignorePermAndGetWorldSettings(damager) { settings ->
            if (event.entity !is Monster && settings.antiDamageFriendEntityLiving) {
                event.isCancelled = true
            }
            if (event.entity is Player && !settings.pvp) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        getWorldSettings(player) { settings ->
            if (event.cause == EntityDamageEvent.DamageCause.VOID && settings.antiVoidDamage) {
                event.isCancelled = true
            }
        }
    }

//    @EventHandler
//    fun onRespawn(event: PlayerRespawnEvent) {
//
//    }

    @EventHandler
    fun onTabComplete(event: TabCompleteEvent) {
        val player = event.sender as? Player ?: return
        ignorePermAndGetWorldSettings(player) { settings ->
            if (!settings.tabComplete) {
                event.completions.clear()
            }
        }
    }

    @EventHandler
    fun onPlayerCommandSend(event: PlayerCommandSendEvent) {
        val player = event.player
        ignorePermAndGetWorldSettings(player) { settings ->
            if (!settings.tabComplete) {
                event.commands.clear()
            }
        }
    }

//    @EventHandler(ignoreCancelled = true)
//    fun onTeleport(event: PlayerTeleportEvent) {
//        val settings = ConfigManager.getSetting(event.to?.world) ?: return
//        if (settings.autoDeOp && event.player.isOp) {
//            event.player.isOp = false
//        }
//    }

    @EventHandler
    fun noBreakFarm(event: PlayerInteractEvent) {
        getWorldSettings(event.player) { settings ->
            if (settings.antiTramplingFarmland && event.action == Action.PHYSICAL && event.clickedBlock?.blockData?.material == Material.FARMLAND) {
                event.isCancelled = true
                return@getWorldSettings
            }
        }
        ignorePermAndGetWorldSettings(event.player) { settings ->
            if (settings.antiItemUse.contains(event.item?.type)) {
                settings.itemBlockAction?.execute(event.player)
                event.isCancelled = true
                return@ignorePermAndGetWorldSettings
            }
            if (event.action == Action.RIGHT_CLICK_BLOCK && event.hasBlock() && settings.blockInteractWhite.isNotEmpty() && !settings.blockInteractWhite.contains(
                    event.clickedBlock?.blockData?.material
                )
            ) {
                event.setUseInteractedBlock(Event.Result.DENY)
//            event.isCancelled = true
                return@ignorePermAndGetWorldSettings
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onArmorStand(event: PlayerArmorStandManipulateEvent) {
        ignorePermAndGetWorldSettings(event.player) { settings ->
            if (settings.antiInteractArmorStand) {
                event.player.errorMsg("此世界禁止与盔甲架交互")
                event.isCancelled = true
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    fun onDrop(event: PlayerDropItemEvent) {
        val item = event.itemDrop
        val stack = item.itemStack
        if (stack.type == Material.AIR) return
        PlayerDropItemStorage.addData(event.player, event.itemDrop)
    }

    @EventHandler
    fun onPickup(event: EntityPickupItemEvent) {
        val entity = event.entity
        if (entity is Player) {
            val player = event.entity as? Player ?: return
            val pickupItem = event.item
            getWorldSettings(player) { settings ->
                val item = player.itemOnCursor
                if (item.type != Material.AIR && settings.antiCursorNoEmptyPickupItem) {
                    event.isCancelled = true
                    return@getWorldSettings
                }
            }
            if (!event.isCancelled) {
                ignorePermAndGetWorldSettings(player) { settings ->
                    val dropData = PlayerDropItemStorage.get(player, pickupItem)
                    if (settings.antiPlayerPickupOtherPlayerDropOfItem && dropData != null) {
                        event.isCancelled = true
                        val offlinePlayer = dropData.pUUID.asOfflineData()
                        if (offlinePlayer != null) {
                            settings.pickupItemAction?.execute(player)
//                        player.msg("该世界不允许你拾取其他玩家(${offlinePlayer.name})掉落的物品")
                        }
                    }
                }
            }
            if (!event.isCancelled) {
                PlayerDropItemStorage.removeData(pickupItem)
            }
        } else {
            val settings = ConfigManager.getSetting(entity.world) ?: return
            if (settings.antiNoPlayerPickupItem) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onBreakEntity(event: HangingBreakByEntityEvent) {
        val remover = event.remover as? Player ?: return

        ignorePermAndGetWorldSettings(remover) { settings ->
            if (event.entity is Painting && settings.antiInteractDisplayFrameAndPaint) {
                event.isCancelled = true
                return@ignorePermAndGetWorldSettings
            }
            if (event.entity is ItemFrame && settings.antiInteractDisplayFrameAndPaint) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onInteractAtItemFrame(event: PlayerInteractEntityEvent) {
        val player = event.player
        if (player.hasPermission(ConfigManager.ignorePermission)) {
            val e = event.rightClicked
            if (e is ItemFrame) {
                e.isFixed = false
            }
            return
        }
        val settings = ConfigManager.getSetting(player.world) ?: return
        val e = event.rightClicked
        if (e is ItemFrame && settings.antiInteractDisplayFrameAndPaint) {
            e.isFixed = true
            event.isCancelled = true
        }
    }

}