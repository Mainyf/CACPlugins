package io.github.mainyf.toolsplugin.module

import dev.lone.itemsadder.api.CustomStack
import io.github.mainyf.newmclib.exts.msg
import io.github.mainyf.newmclib.exts.serialize
import io.github.mainyf.newmclib.exts.toComp
import io.github.mainyf.toolsplugin.config.ConfigTP
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.PlayerInventory

object IaItemAutoUpdate : Listener {

    fun init() {

    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val inv = event.clickedInventory
        if (inv !is PlayerInventory) return
        val item = event.currentItem ?: return
        val iaStack = CustomStack.byItemStack(item) ?: return
        if (!ConfigTP.iaItemAutoUpdateItems.contains(iaStack.namespacedID)) return
        val iaOriginalStack = CustomStack.getInstance(iaStack.namespacedID) ?: return
        val originalStack = iaOriginalStack.itemStack
        val oldMeta = item.itemMeta
        if (!originalStack.itemMeta.equals(oldMeta)) {
            item.itemMeta = originalStack.itemMeta
            inv.setItem(event.slot, item)
            event.isCancelled = true
            ConfigTP.iaItemAutoUpdateInfo?.execute(player, "{itemName}", oldMeta.displayName()?.serialize() ?: "")
        }
    }

}