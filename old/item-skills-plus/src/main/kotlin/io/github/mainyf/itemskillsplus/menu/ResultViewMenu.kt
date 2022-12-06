package io.github.mainyf.itemskillsplus.menu

import io.github.mainyf.itemskillsplus.giveItem
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class ResultViewMenu(
    var itemStack: ItemStack
) : AbstractMenuHandler() {

    private var flag = false

    override fun open(player: Player) {
        val inv = Bukkit.createInventory(
            MenuHolder(
                player,
                this
            ),
            3 * 9,
            "结果展示，点击物品取回"
        )

        inv.setIcon(13, itemStack) {
            it.giveItem(itemStack)
            flag = true
            it.updateInventory()
            it.closeInventory()
        }

        player.openInventory(inv)
    }

    override fun onClose(player: Player, inv: Inventory, event: InventoryCloseEvent) {
        if (flag) return
        player.giveItem(itemStack)
//        player.world.dropItemNaturally(player.location, equipItemStack!!)
    }


}