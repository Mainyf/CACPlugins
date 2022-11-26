package io.github.mainyf.itemenchantplus.menu

import io.github.mainyf.itemenchantplus.config.ConfigIEP
import io.github.mainyf.newmclib.config.IaIcon
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

class DashboardMenu : AbstractMenuHandler() {

    override fun open(player: Player) {
        setup(ConfigIEP.dashboardMenuConfig.settings)
        val inv = createInv(player)

        updateInv(player, inv)
        player.openInventory(inv)
    }

    override fun updateTitle(player: Player): String {
        val dmc = ConfigIEP.dashboardMenuConfig
        val icons = mutableListOf<IaIcon>()
        icons.addAll(dmc.giveEnchantSlot.iaIcon())
        icons.addAll(dmc.intensifySlot.iaIcon())
        icons.addAll(dmc.upgradeSlot.iaIcon())

        return applyTitle(player, icons)
    }

    private fun updateInv(player: Player, inv: Inventory) {
        val dmc = ConfigIEP.dashboardMenuConfig
        inv.setIcon(dmc.giveEnchantSlot) {
            EnchantListMenu().open(it)
        }
        inv.setIcon(dmc.intensifySlot) {
            EnchantIntensifyMenuMenu().open(it)
        }
        inv.setIcon(dmc.upgradeSlot) {
            EnchantUpgradeMenu().open(it)
        }
    }

}