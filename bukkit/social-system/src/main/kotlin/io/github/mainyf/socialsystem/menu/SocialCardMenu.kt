package io.github.mainyf.socialsystem.menu

import io.github.mainyf.bungeesettingsbukkit.CrossServerManager
import io.github.mainyf.newmclib.config.IaIcon
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import io.github.mainyf.newmclib.offline_player_ext.OfflinePlayerData
import io.github.mainyf.newmclib.offline_player_ext.asOfflineData
import io.github.mainyf.newmclib.utils.Cooldown
import io.github.mainyf.newmclib.utils.Heads
import io.github.mainyf.socialsystem.config.ConfigManager
import io.github.mainyf.socialsystem.config.sendLang
import io.github.mainyf.socialsystem.module.FriendHandler
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

class SocialCardMenu(val offlineData: OfflinePlayerData) : AbstractMenuHandler() {

    companion object {

        val friendRequestCooldown = Cooldown()

        val repairCooldown = Cooldown()

    }

    private val target = offlineData.uuid.asPlayer()

    private val isTargetOnlineBC get() = CrossServerManager.isOnline(offlineData.uuid)

    private val isTargetOnline get() = offlineData.uuid.isOnline()

    private lateinit var player: Player

    private val offlinePlayer = Bukkit.getOfflinePlayer(offlineData.name)

    override fun open(player: Player) {
        this.player = player
        setup(ConfigManager.socialCardMenuConfig.settings)
        val inv = createInv(player)

        updateInv(player, inv)
        player.openInventory(inv)
    }

    override fun updateTitle(player: Player): String {
        val scmConfig = ConfigManager.socialCardMenuConfig
        val icons = mutableListOf<IaIcon>()
        icons.addAll(scmConfig.requestSlot.iaIcon())
        icons.addAll(scmConfig.repairSlot.iaIcon())
        icons.addAll(scmConfig.headSlot.iaIcon())
        icons.addAll(scmConfig.cardX1Slot.iaIcon())
        icons.addAll(scmConfig.cardX2Slot.iaIcon())
        icons.addAll(scmConfig.cardX3Slot.iaIcon())
        icons.addAll(scmConfig.cardX4Slot.iaIcon())

        if (isTargetOnlineBC) {
            icons.addAll(scmConfig.onlineSlot.iaIcon("online"))
        } else {
            icons.addAll(scmConfig.onlineSlot.iaIcon("offline"))
        }

        icons.addAll(scmConfig.helmetSlot.iaIcon())
        icons.addAll(scmConfig.chestplateSlot.iaIcon())
        icons.addAll(scmConfig.leggingsSlot.iaIcon())
        icons.addAll(scmConfig.bootsSlot.iaIcon())
        return applyTitle(player, icons)
    }

    private fun updateInv(player: Player, inv: Inventory) {
        val scmConfig = ConfigManager.socialCardMenuConfig
        if (offlineData.uuid != player.uuid) {
            inv.setIcon(scmConfig.requestSlot) {
                if (FriendHandler.isFriend(it, offlineData.uuid)) {
                    it.sendLang("alreadyFriend")
                    return@setIcon
                }
                if (!isTargetOnline) {
                    player.sendLang("targetOffline", "{player}", offlineData.name)
                    return@setIcon
                }
                friendRequestCooldown.invoke(it.uuid, ConfigManager.friendRequestCooldown * 1000L, {
                    FriendHandler.sendFriendRequest(it, offlineData.uuid)
                    player.sendLang("sendFriendRequestToSender")
                    target?.sendLang("sendFriendRequestToReceiver", "{player}", player.name)
                }, { eTime ->
                    it.sendLang("sendFriendRequestCooldown", "{eTime}", eTime.timestampConvertTime())
                })
            }
            inv.setIcon(scmConfig.repairSlot) {
                if (!FriendHandler.allowRepair(offlineData.uuid)) {
                    player.sendLang("targetNoAllowRepairHand", "{player}", offlineData.name)
                    return@setIcon
                }
                if (!it.hasPermission(ConfigManager.repairPermission)) {
                    player.sendLang("noPermissionRepairHand")
                    return@setIcon
                }
                if (!isTargetOnline) {
                    player.sendLang("targetOffline", "{player}", offlineData.name)
                    return@setIcon
                }
                repairCooldown.invoke(it.uuid, ConfigManager.repairCooldown * 1000L, {
                    execmd("cmi repair hand ${target!!.name}")
                    player.sendLang("sendRepairHandEquipmentToSender")
                    target.sendLang("sendRepairHandEquipmentToReceiver", "{player}", player.name)
                }, { eTime ->
                    it.sendLang("repairCooldown", "{eTime}", eTime.timestampConvertTime())
                })
            }
        }

        inv.setIcon(
            scmConfig.headSlot.slot,
            scmConfig.headSlot.default()!!
                .toItemStack(Heads.getPlayerHead(offlineData.name).clone()).tvar("player", offlineData.name)
        )

        arrayOf(
            scmConfig.cardX1Slot,
            scmConfig.cardX2Slot,
            scmConfig.cardX3Slot,
            scmConfig.cardX4Slot
        ).forEach {
            inv.setIcon(it, itemBlock = {
                setPlaceholder(offlinePlayer)
            })
        }

        if (isTargetOnlineBC) {
            inv.setIcon(scmConfig.onlineSlot, "online")
        } else {
            inv.setIcon(scmConfig.onlineSlot, "offline")
        }
        if (isTargetOnlineBC) {
            inv.setIcon(scmConfig.helmetSlot, itemStack = target!!.equipment.helmet ?: AIR_ITEM)
            inv.setIcon(scmConfig.chestplateSlot, itemStack = target.equipment.chestplate ?: AIR_ITEM)
            inv.setIcon(scmConfig.leggingsSlot, itemStack = target.equipment.leggings ?: AIR_ITEM)
            inv.setIcon(scmConfig.bootsSlot, itemStack = target.equipment.boots ?: AIR_ITEM)
        }
    }

}