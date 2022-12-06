package io.github.mainyf.soulbind.listeners

import io.github.mainyf.newmclib.exts.AIR_ITEM
import io.github.mainyf.newmclib.exts.isEmpty
import io.github.mainyf.newmclib.exts.mapToSerialize
import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.newmclib.menu.MenuHolder
import io.github.mainyf.newmclib.utils.Cooldown
import io.github.mainyf.soulbind.RecallSBManager
import io.github.mainyf.soulbind.SBManager
import io.github.mainyf.soulbind.config.sendLang
import io.github.mainyf.soulbind.menu.RecallItemMenu
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

object PlayerListeners : Listener {

    private val interactCD = Cooldown()

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        val topInv = event.player.openInventory.topInventory
        val topInvHolder = topInv.holder
        val items = event.player.openInventory.topInventory.toMutableList()
        if (topInvHolder is MenuHolder && topInvHolder.menuHandler is RecallItemMenu) {
            items.clear()
        }
        items.addAll(event.player.openInventory.bottomInventory.toList())
        RecallSBManager.trySaveRecallItem(items.filterNotNull())
    }

    @EventHandler
    fun onInvClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        if (player.isOp) return
        var itemStack = event.currentItem ?: return
        if (itemStack.isEmpty()) return

        if (!SBManager.hasBindItem(itemStack)) {
            event.isCancelled = true
            if (!hasBindable(player, itemStack)) return
            itemStack = handleItemBind(player, itemStack)
            event.view.setItem(event.rawSlot, itemStack)
            player.sendLang("itemAutoBind")
        } else {
            handleItemInteract(player, itemStack, event)
            tryHandleInvalidItem(player, itemStack) {
                event.isCancelled = true
                event.currentItem = AIR_ITEM
            }
        }
    }

    @EventHandler
    fun onPlckup(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        if (player.isOp) return
        var itemStack = event.item.itemStack
        if (itemStack.isEmpty()) return

        if (!SBManager.hasBindItem(itemStack)) {
            if (!hasBindable(player, itemStack)) return
            itemStack = handleItemBind(player, itemStack)
            event.item.itemStack = itemStack
            player.sendLang("itemAutoBind")
        } else {
            handleItemInteract(player, event.item.itemStack, event)
            tryHandleInvalidItem(player, itemStack) {
                event.isCancelled = true
                event.item.remove()
            }
        }
    }

    @EventHandler
    fun onDropItem(event: PlayerDropItemEvent) {
        val player = event.player
        if (player.isOp) return
        var itemStack = event.itemDrop.itemStack
        if (itemStack.isEmpty()) return

        if (!SBManager.hasBindItem(itemStack)) {
            if (!hasBindable(player, itemStack)) return
            itemStack = handleItemBind(player, itemStack)
            event.itemDrop.itemStack = itemStack
            player.sendLang("itemAutoBind")
        } else {
            handleItemInteract(player, itemStack, event)
            tryHandleInvalidItem(player, itemStack) {
                event.isCancelled = true
                event.itemDrop.remove()
            }
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        val player = event.player
        if (player.isOp) return
        var itemStack = event.item ?: return
        if (itemStack.isEmpty()) return

        if (!SBManager.hasBindItem(itemStack)) {
            if (!hasBindable(player, itemStack)) return
            itemStack = handleItemBind(player, itemStack)
            player.inventory.setItemInMainHand(itemStack)
            player.sendLang("itemAutoBind")
        } else {
            handleItemInteract(player, itemStack, event)
            tryHandleInvalidItem(player, itemStack) {
                event.isCancelled = true
                player.inventory.setItemInMainHand(AIR_ITEM)
            }
        }
    }

    private fun tryHandleInvalidItem(player: Player, itemStack: ItemStack, block: () -> Unit) {
        var flag = false
        if (RecallSBManager.hasInvalidSBItem(itemStack)) {
            interactCD.invoke(player.uuid, 500L, {
                player.sendLang("itemInvalid")
            })
        }
        if (RecallSBManager.hasAbandonItem(itemStack)) {
            flag = true
            interactCD.invoke(player.uuid, 500L, {
                player.sendLang("itemAbandon")
            })
        }
        if (flag) {
            block.invoke()
        }
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

    private fun hasBindable(player: Player, itemStack: ItemStack): Boolean {
        return SBManager.hasBindable(itemStack) || RecallSBManager.hasBindable(itemStack)
    }

    private fun handleItemBind(player: Player, itemStack: ItemStack): ItemStack {
        val rs = SBManager.handleItemBind(player, itemStack)
        return RecallSBManager.handleItemBind(player, rs)
    }

}