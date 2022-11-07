package io.github.mainyf.itemenchantplus.menu

import io.github.mainyf.itemenchantplus.config.ConfigIEP
import io.github.mainyf.newmclib.config.IaIcon
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import io.github.mainyf.newmclib.offline_player_ext.OfflinePlayerData
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

class EnchantListMenu : AbstractMenuHandler() {

    private var pageIndex = 1
    private var pageSize = 0
    private var maxPageIndex = 0

    private val friends = mutableListOf<OfflinePlayerData>()

    private val currentFriends = mutableListOf<OfflinePlayerData>()

    override fun open(player: Player) {
        setup(ConfigIEP.enchantListMenuConfig.settings)
        val inv = createInv(player)

        updateInv(player, inv)
        player.openInventory(inv)
    }

    override fun updateTitle(player: Player): String {
        val elm = ConfigIEP.enchantListMenuConfig
        val icons = mutableListOf<IaIcon>()
        icons.addAll(elm.prevSlot.iaIcon())
        icons.addAll(elm.nextSlot.iaIcon())
        icons.addAll(elm.enchantSlot.iaIcon())
        icons.addAll(elm.backSlot.iaIcon())

        return applyTitle(player, icons)
    }

    private fun updateInv(player: Player, inv: Inventory) {
        val elm = ConfigIEP.enchantListMenuConfig
        inv.setIcon(elm.prevSlot)
        inv.setIcon(elm.nextSlot)


        inv.setIcon(elm.backSlot) {
            DashboardMenu().open(it)
        }
    }


}