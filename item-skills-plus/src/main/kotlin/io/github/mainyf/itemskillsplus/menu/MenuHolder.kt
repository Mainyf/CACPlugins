package io.github.mainyf.itemskillsplus.menu

import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

class MenuHolder(
    val player: Player,
    val menuHandler: AbstractMenuHandler
) : InventoryHolder {

    override fun getInventory(): Inventory = player.inventory

}