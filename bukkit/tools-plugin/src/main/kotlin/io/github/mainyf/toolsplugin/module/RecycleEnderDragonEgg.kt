package io.github.mainyf.toolsplugin.module

import io.github.mainyf.newmclib.exts.AIR_ITEM
import io.github.mainyf.toolsplugin.ToolsPlugin
import io.github.mainyf.toolsplugin.config.ConfigTP
import org.bukkit.Material
import org.bukkit.block.Chest
import org.bukkit.block.DoubleChest
import org.bukkit.block.ShulkerBox
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryOpenEvent

object RecycleEnderDragonEgg : Listener {

    @EventHandler
    fun onInventoryOpenEvent(event: InventoryOpenEvent) {
        if (!ConfigTP.recycleEnderDragonEgg) return
        val holder = event.inventory.holder
        val player = event.player as? Player ?: return
        if (holder is Chest || holder is DoubleChest || holder is ShulkerBox) {
            var flag = false
            var amount = 0
            event.inventory.forEachIndexed { index, itemStack ->
                if (itemStack?.type == Material.DRAGON_EGG) {
                    flag = true
                    amount += itemStack.amount
                    event.inventory.setItem(index, AIR_ITEM)
                }
            }
            if (flag) {
                val loc = when (holder) {
                    is Chest -> holder.location
                    is DoubleChest -> holder.location
                    is ShulkerBox -> holder.location
                    else -> null
                }!!
                //                val loc = (holder as? Chest)?.location ?: (holder as? DoubleChest)?.location ?: (holder as ShulkerBox).location
                ToolsPlugin.LOGGER.info("玩家: ${player.name}，龙蛋已删除(x${amount})，位置: ${loc.world?.name} ${loc.x} ${loc.y} ${loc.z}")
                player.updateInventory()
            }
        }
    }

}