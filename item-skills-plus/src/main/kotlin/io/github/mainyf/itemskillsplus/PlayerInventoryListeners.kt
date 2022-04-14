package io.github.mainyf.itemskillsplus

import io.github.mainyf.itemskillsplus.menu.MenuHolder
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent

object PlayerInventoryListeners : Listener {

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val topInv = event.whoClicked.openInventory.topInventory
        if (topInv.holder !is MenuHolder) return
        if (event.action == InventoryAction.COLLECT_TO_CURSOR) {
            event.isCancelled = true
        }
        val inv = event.clickedInventory
//        val invHolder = event.inventory.holder

//        if (invHolder is MenuHolder && inv?.holder is Player && !invHolder.hasClickPlayerInv) {
//            event.isCancelled = true
//        }

        val holder = topInv.holder as MenuHolder

        if (inv?.holder !is MenuHolder) {
            kotlin.runCatching {
                holder.menuHandler.onClickPlayerInv(event.slot, holder.player, topInv, event)
            }.onFailure {
                it.printStackTrace()
            }
            return
        }
        kotlin.runCatching {
            holder.menuHandler.triggerClick(event.slot, holder.player, inv, event)
        }.onFailure {
            it.printStackTrace()
        }
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        val inv = event.inventory
        val player = event.player
        if (player !is Player) return
        val holder = inv.holder
        if (holder !is MenuHolder) return
        holder.menuHandler.onClose(player, inv, event)
    }

}