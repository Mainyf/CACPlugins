package io.github.mainyf.soulbind.listeners

import io.github.mainyf.newmclib.exts.isEmpty
import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.soulbind.SBManager
import io.github.mainyf.soulbind.config.sendLang
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

object PlayerListeners : Listener {

    @EventHandler
    fun onInvClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        handleItemInteract(player, event.currentItem, event)
    }

    @EventHandler
    fun onPlckup(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        handleItemInteract(player, event.item.itemStack, event)
    }

    @EventHandler
    fun onDropItem(event: PlayerDropItemEvent) {
        val player = event.player
        handleItemInteract(player, event.itemDrop.itemStack, event)
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item ?: return
        handleItemInteract(player, item, event)
    }

    private fun handleItemInteract(player: Player, itemStack: ItemStack?, cancellable: Cancellable) {
        if (itemStack.isEmpty()) return
        val itemData = SBManager.getBindItemData(itemStack) ?: return
        if (itemData.ownerUUID != player.uuid) {
            cancellable.isCancelled = true
            player.sendLang("noBindItemOwnerInteract")
            return
        }
    }

}