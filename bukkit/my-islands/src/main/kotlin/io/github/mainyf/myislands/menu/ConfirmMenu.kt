package io.github.mainyf.myislands.menu

import io.github.mainyf.newmclib.exts.colored
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player

class ConfirmMenu(val block: (Player) -> Unit) : AbstractMenuHandler() {

    override fun open(player: Player) {
        val inv = Bukkit.createInventory(createHolder(player), 3 * 9, Component.text("&a确认".colored()))

        inv.setIcon(13, Material.RED_STAINED_GLASS_PANE, "${ChatColor.RED}确认选择", clickBlock = block)

        player.openInventory(inv)
    }

}