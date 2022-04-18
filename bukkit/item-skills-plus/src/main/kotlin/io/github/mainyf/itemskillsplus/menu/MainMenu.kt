package io.github.mainyf.itemskillsplus.menu

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player

class MainMenu : AbstractMenuHandler() {

    override fun open(player: Player) {
        val inv = Bukkit.createInventory(
            MenuHolder(
                player,
                this
            ),
            3 * 9,
            "主界面"
        )

//        inv.maxStackSize = 128
//
//        inv.setItem(0, ItemStack(Material.STONE).apply {
//            amount = 65
//        })

        inv.setIcon(12, Material.RED_STAINED_GLASS_PANE, "赋予附魔界面") {
            InitItemSkillMenu().open(it)
        }
        inv.setIcon(14, Material.LIME_STAINED_GLASS_PANE, "附魔进阶界面") {
            ItemSkillUpgradeMenu().open(it)
        }

        player.openInventory(inv)
    }

//    override fun onClick(slot: Int, player: Player, inv: Inventory, event: InventoryClickEvent) {
//        println("[当前slot] $slot")
//    }

}