package io.github.mainyf.worldsettings.listeners

import io.github.mainyf.newmclib.exts.errorMsg
import io.github.mainyf.newmclib.exts.msg
import io.github.mainyf.newmclib.offline_player_ext.asOfflineData
import io.github.mainyf.worldsettings.PlayerDropItemStorage
import io.github.mainyf.worldsettings.config.CommandMatchType.LIKE
import io.github.mainyf.worldsettings.config.CommandMatchType.START
import io.github.mainyf.worldsettings.config.ConfigManager
import org.bukkit.Material
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.player.*
import org.bukkit.event.server.TabCompleteEvent


object PlayerListener : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onCommand(event: PlayerCommandPreprocessEvent) {
        val player = event.player
        if (player.hasPermission(ConfigManager.ignorePermission)) return
        val settings = ConfigManager.getSetting(player.world) ?: return
        val command = event.message
        var cancel = true
        settings.commandWhite.forEach { (type, cmd) ->
            when (type) {
                LIKE -> {
                    if (command.contains(cmd)) {
                        cancel = false
                        return
                    }
                }
                START -> {
                    if (command.startsWith(cmd)) {
                        cancel = false
                        return
                    }
                }
            }
        }
        if (cancel) {
            settings.commandBlockAction?.execute(player)
            event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun handlePlayerGameMode(event: PlayerTeleportEvent) {
        val player = event.player
        val world = event.to?.world
        if (player.hasPermission(ConfigManager.ignorePermission)) return
        val settings = ConfigManager.getSetting(world) ?: return
        if (player.gameMode != settings.gameMode) {
            player.gameMode = settings.gameMode
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerModeChange(event: PlayerGameModeChangeEvent) {
        val player = event.player
        if (player.hasPermission(ConfigManager.ignorePermission)) return
        val settings = ConfigManager.getSetting(player.world) ?: return
        if (event.newGameMode != settings.gameMode) {
            player.errorMsg("当前世界不允许更改游戏模式")
            event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onToggleFlight(event: PlayerToggleFlightEvent) {
        val player = event.player
        if (player.hasPermission(ConfigManager.ignorePermission)) return
        val settings = ConfigManager.getSetting(player.world) ?: return
        if (settings.antiFly) {
            player.allowFlight = false
            player.isFlying = false
            settings.flyBlockAction?.execute(player)
            event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    fun onDamage(event: EntityDamageByEntityEvent) {
        val damager = ((event.damager as? Projectile)?.shooter as? Player) ?: (event.damager as? Player) ?: return
        if (damager.hasPermission(ConfigManager.ignorePermission)) return
        val settings = ConfigManager.getSetting(damager.world) ?: return
        if (event.entity !is Monster && settings.antiDamageFriendEntityLiving) {
            event.isCancelled = true
        }
        if (event.entity is Player && !settings.pvp) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onTabComplete(event: TabCompleteEvent) {
        val player = event.sender as? Player ?: return
        if (player.hasPermission(ConfigManager.ignorePermission)) return
        val settings = ConfigManager.getSetting(player.world) ?: return
        if (!settings.tabComplete) {
            event.completions.clear()
        }
    }

    @EventHandler
    fun onPlayerCommandSend(event: PlayerCommandSendEvent) {
        val player = event.player
        if (player.hasPermission(ConfigManager.ignorePermission)) return
        val settings = ConfigManager.getSetting(player.world) ?: return
        if (!settings.tabComplete) {
            event.commands.clear()
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
        if (event.player.hasPermission(ConfigManager.ignorePermission)) return

        val settings = ConfigManager.getSetting(event.player.world) ?: return
        if (settings.antiTramplingFarmland && event.action == Action.PHYSICAL && event.clickedBlock?.blockData?.material == Material.FARMLAND) {
            event.isCancelled = true
            return
        }
        if (settings.antiItemUse.contains(event.item?.type)) {
            settings.itemBlockAction?.execute(event.player)
            event.isCancelled = true
            return
        }
        if (event.action == Action.RIGHT_CLICK_BLOCK && event.hasBlock() && !settings.blockInteractWhite.contains(event.clickedBlock?.blockData?.material)) {
            event.isCancelled = true
            return
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onArmorStand(event: PlayerArmorStandManipulateEvent) {
        if (event.player.hasPermission(ConfigManager.ignorePermission)) return
        val settings = ConfigManager.getSetting(event.player.world) ?: return
        if (settings.antiInteractArmorStand) {
            event.player.errorMsg("此世界禁止与盔甲架交互")
            event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    fun onDrop(event: PlayerDropItemEvent) {
        val item = event.itemDrop
        val stack = item.itemStack
        if (stack.type == Material.AIR) return
        PlayerDropItemStorage.addData(event.player, event.itemDrop)
    }

    @EventHandler(ignoreCancelled = true)
    fun onPickup(event: EntityPickupItemEvent) {
        val entity = event.entity
        if (entity is Player) {
            val player = event.entity as? Player ?: return
            val pickupItem = event.item
            kotlin.run {
                if (player.hasPermission(ConfigManager.ignorePermission)) return@run
                val settings = ConfigManager.getSetting(player.world) ?: return@run
                val item = player.itemOnCursor
                if (item.type != Material.AIR && settings.antiCursorNoEmptyPickupItem) {
                    event.isCancelled = true
                    return@run
                }
                val dropData = PlayerDropItemStorage.get(player, pickupItem)
                if (settings.antiPlayerPickupOtherPlayerDropOfItem && dropData != null) {
                    event.isCancelled = true
                    val offlinePlayer = dropData.pUUID.asOfflineData()
                    if (offlinePlayer != null) {
                        player.msg("该世界不允许你拾取其他玩家(${offlinePlayer.name})掉落的物品")
                    }
                }
            }
            PlayerDropItemStorage.removeData(pickupItem)
        } else {
            val settings = ConfigManager.getSetting(entity.world) ?: return
            if (settings.antiNoPlayerPickupItem) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onBreakEntity(event: HangingBreakByEntityEvent) {
        val remover = event.remover
        if (remover is Player && remover.hasPermission(ConfigManager.ignorePermission)) return
        val settings = ConfigManager.getSetting(event.entity.world) ?: return
        if (event.entity is Painting && settings.antiInteractDisplayFrameAndPaint) {
            event.isCancelled = true
            return
        }
        if (event.entity is ItemFrame && settings.antiInteractDisplayFrameAndPaint) {
            event.isCancelled = true
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

//    @EventHandler(ignoreCancelled = true)
//    fun onDead(event: PlayerDeathEvent) {
//        val player = event.entity
//        val settings = ConfigManager.getSetting(player.world) ?: return
//        if (settings.removeDeadMessage) {
//            event.deathMessage = null
//        }
//    }

//    @EventHandler(ignoreCancelled = true)
//    fun on1(event: PlayerDropItemEvent) {
//
//    }
//
//    @EventHandler(ignoreCancelled = true)
//    fun on2(event: EntityPickupItemEvent) {
//
//    }

}