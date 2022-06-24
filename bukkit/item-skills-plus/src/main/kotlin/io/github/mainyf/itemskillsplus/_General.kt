package io.github.mainyf.itemskillsplus

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

fun ItemStack?.isEmpty() = this == null || this.type == Material.AIR

fun Player.giveItem(item: ItemStack) {
    val leftover = inventory.addItem(item)
    leftover.forEach {
        world.dropItemNaturally(location, it.value)
    }
}