package io.github.mainyf.itemenchantplus.listeners

import com.destroystokyo.paper.event.inventory.PrepareResultEvent
import io.github.mainyf.itemenchantplus.EnchantManager
import io.github.mainyf.itemenchantplus.config.sendLang
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.GrindstoneInventory

object PlayerListeners : Listener {

    @EventHandler
    fun onEnchant(event: EnchantItemEvent) {
        val itemStack = event.item
        if (EnchantManager.hasEnchantItem(itemStack)) {
            val data = EnchantManager.getItemEnchant(itemStack) ?: return
            val enchants = event.enchantsToAdd
            val conflictEnchants = data.enchantType.conflictEnchant()
            val conflictEnchantsToAdd = enchants.keys.filter {
                conflictEnchants.contains(it)
            }
            if (conflictEnchantsToAdd.isNotEmpty()) {
                conflictEnchantsToAdd.forEach {
                    event.enchanter.sendLang(
                        "conflictEnchantToEnchantPlus",
                        "{enchantName}", data.enchantType.displayName(),
                        "{enchant_text}", Component.translatable(it)
                    )
                }
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        if (event.inventory !is GrindstoneInventory) return
        val itemStack = event.currentItem ?: return
        if (!EnchantManager.hasEnchantItem(itemStack)) return
        val data = EnchantManager.getItemEnchant(itemStack) ?: return
        if (EnchantManager.hasExtraData(data.enchantType, itemStack, data.enchantType.plusExtraDataName())) {
            player.sendLang("antiUseGrindstone")
            event.isCancelled = true
        }
    }

}