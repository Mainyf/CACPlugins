package io.github.mainyf.itemenchantplus.menu

import io.github.mainyf.itemenchantplus.config.ConfigIEP
import io.github.mainyf.itemenchantplus.config.ItemEnchantType
import io.github.mainyf.newmclib.config.IaIcon
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import kotlin.math.ceil

class EnchantListMenu : AbstractMenuHandler() {

    private var pageIndex = 1
    private var pageSize = 0
    private var maxPageIndex = 0

    private val enchants = mutableListOf<ItemEnchantType>()

    private val currentEnchants = mutableListOf<ItemEnchantType>()

    override fun open(player: Player) {
        this.pageSize = ConfigIEP.enchantListMenuConfig.enchantSlot.slot.size
        updateEnchants()
        updateCurrentEnchants()
        setup(ConfigIEP.enchantListMenuConfig.settings)
        val inv = createInv(player)

        updateInv(player, inv)
        player.openInventory(inv)
    }

    private fun updateEnchants() {
        this.enchants.clear()
        this.enchants.addAll(ItemEnchantType.values())
        this.maxPageIndex = ceil(
            enchants.size.toDouble() / pageSize.toDouble()
        ).toInt()
    }

    private fun updateCurrentEnchants() {
        currentEnchants.clear()
        currentEnchants.addAll(enchants.pagination(pageIndex, pageSize))
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
        inv.setIcon(elm.prevSlot) {
            if (pageIndex > 1) {
                pageIndex--
                updateCurrentEnchants()
                updateEnchantSlot(player, inv)
            }
        }
        inv.setIcon(elm.nextSlot) {
            if (pageIndex < maxPageIndex) {
                pageIndex++
                updateCurrentEnchants()
                updateEnchantSlot(player, inv)
            }
        }

        updateEnchantSlot(player, inv)

        inv.setIcon(elm.backSlot) {
            DashboardMenu().open(it)
        }
    }

    private fun updateEnchantSlot(player: Player, inv: Inventory) {
        val elm = ConfigIEP.enchantListMenuConfig
        val enchantSlot = elm.enchantSlot.slot
        inv.setIcon(elm.enchantSlot.slot, AIR_ITEM)
        currentEnchants.forEachIndexed { index, enchant ->
            inv.setIcon(enchantSlot[index], elm.enchantSlot.default()!!.toItemStack {
                withMeta(
                    {
                        it?.serialize()?.tvar("enchantName", enchant.displayName())?.deserialize()
                    },
                    { lore ->
                        if (lore.isNullOrEmpty()) return@withMeta lore
                        lore.mapToSerialize().tvarList("desc", enchant.menuItemInListMenu()).mapToDeserialize()
                    }
                )
            }) {
                elm.enchantSlot.default()?.execAction(it)
                GiveEnchantMenu(enchant).open(it)
            }
        }

    }


}