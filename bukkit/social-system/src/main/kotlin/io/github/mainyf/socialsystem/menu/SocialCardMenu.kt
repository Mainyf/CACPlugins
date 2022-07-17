package io.github.mainyf.socialsystem.menu

import io.github.mainyf.newmclib.exts.AIR_ITEM
import io.github.mainyf.newmclib.exts.asPlayer
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import io.github.mainyf.newmclib.offline_player_ext.OfflinePlayerData
import io.github.mainyf.socialsystem.config.ConfigManager
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

class SocialCardMenu(offlineData: OfflinePlayerData) : AbstractMenuHandler() {

    private val player = offlineData.uuid.asPlayer()

    private val hasOnline get() = player != null

    override fun open(player: Player) {
        setup(ConfigManager.socialCardMenuConfig.settings)
        val inv = createInv(player)

        updateInv(player, inv)
        player.openInventory(inv)
    }

    private fun updateInv(player: Player, inv: Inventory) {
        val scmConfig = ConfigManager.socialCardMenuConfig
        inv.setIcon(scmConfig.requestSlot)
        inv.setIcon(scmConfig.repairSlot)
        inv.setIcon(scmConfig.headSlot)
        inv.setIcon(scmConfig.cardX1Slot)
        inv.setIcon(scmConfig.cardX2Slot)
        inv.setIcon(scmConfig.cardX3Slot)
        inv.setIcon(scmConfig.cardX4Slot)

        if (hasOnline) {
            inv.setIcon(scmConfig.onlineSlot.slot, scmConfig.onlineSlot.onlineItem.toItemStack())
        } else {
            inv.setIcon(scmConfig.onlineSlot.slot, scmConfig.onlineSlot.offlineItem.toItemStack())
        }
        if (hasOnline) {
            inv.setIcon(scmConfig.helmetSlot.slot, player.equipment.helmet ?: AIR_ITEM)
            inv.setIcon(scmConfig.chestplateSlot.slot, player.equipment.chestplate ?: AIR_ITEM)
            inv.setIcon(scmConfig.leggingsSlot.slot, player.equipment.leggings ?: AIR_ITEM)
            inv.setIcon(scmConfig.bootsSlot.slot, player.equipment.boots ?: AIR_ITEM)
        }
    }

}