package io.github.mainyf.soulbind.listeners

import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.menu.MenuHolder
import io.github.mainyf.newmclib.nms.asNmsPlayer
import io.github.mainyf.newmclib.utils.Cooldown
import io.github.mainyf.soulbind.RecallSBManager
import io.github.mainyf.soulbind.SBManager
import io.github.mainyf.soulbind.config.sendLang
import io.github.mainyf.soulbind.menu.RecallItemMenu
import org.bukkit.Material
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Painting
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerArmorStandManipulateEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
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
        val itemStack = event.currentItem ?: return
        if (itemStack.isEmpty()) return

        if (!SBManager.hasBindItem(itemStack)) {
            tryAutoBind(player, itemStack, event) {
                event.view.setItem(event.rawSlot, it)
            }
        } else {
            if (handleItemInteract(player, itemStack, event)) {
                event.currentItem = AIR_ITEM
                player.asNmsPlayer().dropItemNaturally(itemStack)
            }
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
        val itemStack = event.item.itemStack
        if (itemStack.isEmpty()) return

        if (!SBManager.hasBindItem(itemStack)) {
            if (RecallSBManager.hasBindable(itemStack)) {
                player.sendLang("antiPickupBindableItem")
                event.isCancelled = true
            } else {
                tryAutoBind(player, itemStack) {
                    event.item.itemStack = it
                }
            }
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
        val itemStack = event.itemDrop.itemStack
        if (itemStack.isEmpty()) return

        if (!SBManager.hasBindItem(itemStack)) {
            tryAutoBind(player, itemStack) {
                event.itemDrop.itemStack = it
            }
        } else {
            //            handleItemInteract(player, itemStack, event)
            tryHandleInvalidItem(player, itemStack) {
                event.isCancelled = true
                event.itemDrop.remove()
            }
            if(!event.isCancelled) {
                val data = SBManager.getBindItemData(itemStack)
                if(data != null && data.ownerUUID == player.uuid) {
                    player.sendLang("antiDropBindItem")
                    event.isCancelled = true
                }
            }
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        val player = event.player
        if (player.isOp) return
        val itemStack = event.item ?: return
        if (itemStack.isEmpty()) return

        if (!SBManager.hasBindItem(itemStack)) {
            tryAutoBind(player, itemStack) {
                player.inventory.setItemInMainHand(it)
            }
        } else {
            handleItemInteract(player, itemStack, event)
            tryHandleInvalidItem(player, itemStack) {
                event.isCancelled = true
                player.inventory.setItemInMainHand(AIR_ITEM)
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onInteractAtItemFrame(event: PlayerInteractEntityEvent) {
        val player = event.player
        if (player.isOp) return
        val e = event.rightClicked
        if (e is ItemFrame) {
            val itemStack = player.inventory.getItem(event.hand)
            if (itemStack.isEmpty()) return
            if (!SBManager.hasBindItem(itemStack)) {
                tryAutoBind(player, itemStack, event) {
                    player.inventory.setItem(event.hand, it)
                }
            } else {
                handleItemInteract(player, itemStack, event)
                tryHandleInvalidItem(player, itemStack) {
                    event.isCancelled = true
                    player.inventory.setItem(event.hand, AIR_ITEM)
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onArmorStand(event: PlayerArmorStandManipulateEvent) {
        val player = event.player
        if (player.isOp) return
        val itemStack = event.playerItem
        if (itemStack.isEmpty()) return
        if (!SBManager.hasBindItem(itemStack)) {
            tryAutoBind(player, itemStack, event) {
                player.inventory.setItemInMainHand(it)
            }
        } else {
            handleItemInteract(player, itemStack, event)
            tryHandleInvalidItem(player, itemStack) {
                event.isCancelled = true
                player.inventory.setItemInMainHand(AIR_ITEM)
            }
        }
    }

    private fun tryAutoBind(
        player: Player,
        itemStack: ItemStack,
        event: Cancellable? = null,
        block: (ItemStack) -> Unit
    ) {
        var rs = itemStack
        if (!hasBindable(rs)) return
        event?.isCancelled = true
        rs = handleItemBind(player, rs)
        block(rs)
        player.sendLang("itemAutoBind")
    }

    private fun tryHandleInvalidItem(player: Player, itemStack: ItemStack, block: () -> Unit) {
        var flag = false
        if (RecallSBManager.hasInvalidSBItem(itemStack)) {
            flag = true
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

    private fun handleItemInteract(player: Player, itemStack: ItemStack?, cancellable: Cancellable): Boolean {
        if (itemStack.isEmpty()) return false
        val itemData = SBManager.getBindItemData(itemStack) ?: return false
        if (itemData.ownerUUID != player.uuid) {
            cancellable.isCancelled = true
            player.sendLang("noBindItemOwnerInteract")
            return true
        }
        return false
    }

    private fun hasBindable(itemStack: ItemStack): Boolean {
        return SBManager.hasBindable(itemStack) || RecallSBManager.hasBindable(itemStack)
    }

    private fun handleItemBind(player: Player, itemStack: ItemStack): ItemStack {
        val rs = SBManager.handleItemBind(player, itemStack)
        return RecallSBManager.handleItemBind(player, rs)
    }

}