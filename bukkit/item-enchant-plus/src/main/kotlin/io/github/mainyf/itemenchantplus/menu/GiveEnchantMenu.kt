package io.github.mainyf.itemenchantplus.menu

import io.github.mainyf.itemenchantplus.EnchantManager
import io.github.mainyf.itemenchantplus.config.ConfigIEP
import io.github.mainyf.itemenchantplus.config.ItemEnchantType
import io.github.mainyf.itemenchantplus.config.sendLang
import io.github.mainyf.newmclib.config.IaIcon
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory

class GiveEnchantMenu(val enchantType: ItemEnchantType) : AbstractMenuHandler() {

    private var enchantItem = AIR_ITEM

    override fun open(player: Player) {
        setup(ConfigIEP.giveEnchantMenuConfig.settings)
        val inv = createInv(player)

        updateInv(player, inv)
        player.openInventory(inv)
    }

    override fun updateTitle(player: Player): String {
        val gem = ConfigIEP.giveEnchantMenuConfig
        val icons = mutableListOf<IaIcon>()
        icons.addAll(gem.materialsSlot.iaIcon())
        icons.addAll(gem.infoSlot.iaIcon())
        icons.addAll(gem.equipSlot.iaIcon())
        icons.addAll(gem.backSlot.iaIcon())
        icons.addAll(gem.finishSlot.iaIcon())

        return applyTitle(player, icons)
    }

    private fun updateInv(player: Player, inv: Inventory) {
        val gem = ConfigIEP.giveEnchantMenuConfig
        updateMaterials(inv)
        inv.setIcon(gem.infoSlot, itemBlock = {
            withMeta(
                {
                    it?.serialize()?.tvar("enchantName", enchantType.displayName())?.deserialize()
                },
                { lore ->
                    if (lore.isNullOrEmpty()) return@withMeta lore
                    lore.mapToSerialize().tvarList("desc", enchantType.menuItemInGiveMenu()).mapToDeserialize()
                }
            )
        })
        inv.setIcon(gem.backSlot) {
            EnchantListMenu().open(it)
        }
        inv.setIcon(gem.finishSlot) {
            finish(it, inv)
        }
    }

    private fun finish(player: Player, inv: Inventory) {
        if (enchantItem.isEmpty()) {
            player.sendLang("giveItemEmpty")
            return
        }
        val giveMaterials = enchantType.upgradeMaterials()[0]

        if (giveMaterials.any { material ->
                val count = player.countByItem {
                    material.item.equalsItem(it)
                }
                count < material.amount
            }) {
            player.sendLang("giveItemEnchantMaterialsDeficiency")
            return
        }

        giveMaterials.forEach { material ->
            player.takeItem(material.amount) {
                material.item.equalsItem(it)
            }
        }
        EnchantManager.initItemEnchant(player, enchantType, enchantItem)
        val data = EnchantManager.getItemEnchant(enchantItem)!!
        EnchantManager.updateItemMeta(enchantItem, data)
        player.giveItem(enchantItem)
        player.updateInventory()
        enchantItem = AIR_ITEM
        updateMaterials(inv)
        player.sendLang("giveItemEnchantSuccess", "{enchantName}", enchantType.displayName())
    }

    private fun updateMaterials(inv: Inventory) {
        val gem = ConfigIEP.giveEnchantMenuConfig
        inv.setIcon(gem.equipSlot.slot, itemStack = enchantItem) {
            if (enchantItem.isEmpty()) return@setIcon
            it.giveItem(enchantItem)
            it.updateInventory()
            enchantItem = AIR_ITEM
            updateMaterials(inv)
        }
        val mSlot = gem.materialsSlot.slot
        if (!enchantItem.isEmpty()) {
            val giveMaterials = enchantType.upgradeMaterials()[0]
            giveMaterials.forEachIndexed { index, enchantMaterial ->
                val materialItem = enchantMaterial.item.toItemStack()
                val materialDName = materialItem.getItemName()!!
                val materialLore = materialItem.lore() ?: emptyList()
                inv.setIcon(mSlot[index], gem.materialsSlot.default()!!.toItemStack(materialItem.clone()) {
                    withMeta(
                        {
                            it?.tvar("itemName", materialDName, "count", enchantMaterial.amount.toString().toComp())
                        },
                        { lore ->
                            if (lore.isNullOrEmpty()) return@withMeta lore
                            lore.mapToSerialize().tvarList("lore", materialLore.map { it.serialize() }).mapToDeserialize()
                        }
                    )
                })
            }
        } else {
            inv.unSetIcon(mSlot)
        }
    }

    //    override fun onClick(slot: Int, player: Player, inv: Inventory, event: InventoryClickEvent) {
    //        super.onClick(slot, player, inv, event)
    //        val gem = ConfigIEP.giveEnchantMenuConfig
    //        event.isCancelled = true
    //        if (gem.equipSlot.slot.first() == slot && !enchantItem.isEmpty()) {
    //            player.giveItem(enchantItem)
    //            player.updateInventory()
    //            enchantItem = AIR_ITEM
    //            updateMaterials(inv)
    //        }
    //    }

    override fun onClickPlayerInv(slot: Int, player: Player, inv: Inventory, event: InventoryClickEvent) {
        super.onClickPlayerInv(slot, player, inv, event)
        handlePutEnchantItem(slot, player, inv, event)
    }

    private fun handlePutEnchantItem(slot: Int, player: Player, inv: Inventory, event: InventoryClickEvent) {
        val pInv = event.clickedInventory ?: return
        val itemStack = pInv.getItem(slot) ?: return
        if (itemStack.isEmpty()) return
        if (!enchantType.allowGiveItem().contains(itemStack.type)) {
            player.sendLang("giveItemUnqualified")
            return
        }
        if (EnchantManager.hasEnchantItem(itemStack)) {
            player.sendLang("enchantItemPutGiveEnchantMenu")
            return
        }

        if (enchantItem.isEmpty()) {
            enchantItem = itemStack
            pInv.setItem(slot, null)
        } else {
            pInv.setItem(slot, enchantItem)
            enchantItem = itemStack
        }
        updateMaterials(inv)
    }

    override fun onClose(player: Player, inv: Inventory, event: InventoryCloseEvent) {
        super.onClose(player, inv, event)
        if (!enchantItem.isEmpty()) {
            player.giveItem(enchantItem)
            player.updateInventory()
        }
    }

}