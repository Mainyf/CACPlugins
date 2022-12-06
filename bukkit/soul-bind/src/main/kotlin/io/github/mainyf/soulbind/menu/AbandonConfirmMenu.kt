package io.github.mainyf.soulbind.menu

import io.github.mainyf.newmclib.config.IaIcon
import io.github.mainyf.newmclib.exts.toEquipItemSlot
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import io.github.mainyf.soulbind.config.ConfigSB
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class AbandonConfirmMenu(
    val itemStack: ItemStack,
    val successBlock: (Player) -> Unit,
    val backBlock: (Player) -> Unit
) : AbstractMenuHandler() {

    override fun open(player: Player) {
        setup(ConfigSB.abandonConfirmMenu.settings)
        val inv = createInv(player)

        val acm = ConfigSB.abandonConfirmMenu
        inv.setIcon(acm.itemSlot.slot, itemStack = itemStack.toEquipItemSlot())
        inv.setIcon(acm.confirmSlot, leftClickBlock = successBlock)
        inv.setIcon(acm.backSlot, leftClickBlock = backBlock)

        player.openInventory(inv)
    }

    override fun updateTitle(player: Player): String {
        val acm = ConfigSB.abandonConfirmMenu
        val icons = mutableListOf<IaIcon>()
        icons.addAll(acm.itemSlot.iaIcon())
        icons.addAll(acm.confirmSlot.iaIcon())
        icons.addAll(acm.backSlot.iaIcon())
        return applyTitle(player, icons)
    }

}