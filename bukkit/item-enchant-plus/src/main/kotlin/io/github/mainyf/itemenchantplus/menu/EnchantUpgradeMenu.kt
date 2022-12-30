package io.github.mainyf.itemenchantplus.menu

import io.github.mainyf.itemenchantplus.EnchantData
import io.github.mainyf.itemenchantplus.EnchantManager
import io.github.mainyf.itemenchantplus.config.ConfigIEP
import io.github.mainyf.itemenchantplus.config.sendLang
import io.github.mainyf.itemenchantplus.storage.StorageIEP
import io.github.mainyf.newmclib.config.IaIcon
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import kotlin.math.min

class EnchantUpgradeMenu : AbstractMenuHandler() {

    private var enchantData: EnchantData? = null
    private var enchantItem = AIR_ITEM

    override fun open(player: Player) {
        setup(ConfigIEP.enchantUpgradeMenuConfig.settings)
        val inv = createInv(player)

        updateInv(player, inv)
        player.openInventory(inv)
    }

    override fun updateTitle(player: Player): String {
        val eum = ConfigIEP.enchantUpgradeMenuConfig
        val icons = mutableListOf<IaIcon>()
        icons.addAll(eum.materialsSlot.iaIcon())
        icons.addAll(eum.equipSlot.iaIcon())
        icons.addAll(eum.upgradeResultSlot.iaIcon())
        icons.addAll(eum.backSlot.iaIcon())
        icons.addAll(eum.finishSlot.iaIcon())

        return applyTitle(player, icons)
    }

    private fun updateInv(player: Player, inv: Inventory) {
        val eum = ConfigIEP.enchantUpgradeMenuConfig
        updateMaterials(inv)
        inv.setIcon(eum.backSlot) {
            DashboardMenu().open(it)
        }
        inv.setIcon(eum.finishSlot) {
            finish(it, inv)
        }
    }

    private fun finish(player: Player, inv: Inventory) {
        if (enchantItem.isEmpty()) {
            player.sendLang("giveItemEmpty")
            return
        }
        val enchantData = enchantData ?: return
        if (!enchantData.hasMaxLevel) {
            player.sendLang("enchantUpgradeLevelUnqualified")
            return
        }
        if (enchantData.stage >= ConfigIEP.stageLevel.size) {
            player.sendLang("enchantMaxStage")
            return
        }
        val upgradeMaterials = enchantData.enchantType.upgradeMaterials()[enchantData.stage + 1]

        if (upgradeMaterials.any { material ->
                val count = player.countByItem {
                    material.item.equalsItem(it)
                }
                count < material.amount
            }) {
            player.sendLang("enchantUpgradeMaterialsDeficiency")
            return
        }

        upgradeMaterials.forEach { material ->
            player.takeItem(material.amount) {
                material.item.equalsItem(it)
            }
        }
        enchantData.stage++
        EnchantManager.setItemEnchantData(enchantData.enchantType, enchantItem, enchantData)
        val data = EnchantManager.getItemEnchant(enchantItem)!!
        EnchantManager.updateItemMeta(enchantItem, data)
        player.giveItem(enchantItem)
        player.updateInventory()
        enchantItem = AIR_ITEM
        setup(ConfigIEP.enchantUpgradeMenuConfig.settings)
        updateMaterials(inv)
        player.sendLang("enchantUpgradeSuccess", "{enchantName}", enchantData.enchantType.displayName())
        updateTitle(player)
    }

    private fun updateMaterials(inv: Inventory) {
        val gum = ConfigIEP.enchantUpgradeMenuConfig
        inv.setIcon(gum.equipSlot.slot, itemStack = enchantItem.toEquipItemSlot()) {
            if (enchantItem.isEmpty()) return@setIcon
            it.giveItem(enchantItem)
            it.updateInventory()
            enchantItem = AIR_ITEM
            setup(ConfigIEP.enchantUpgradeMenuConfig.settings)
            updateMaterials(inv)
            updateTitle(it)
        }
        val mSlot = gum.materialsSlot.slot
        if (!enchantItem.isEmpty()) {
            inv.setIcon(gum.infoSlot, itemBlock = {
                withMeta(
                    {
                        it?.serialize()?.tvar("enchantName", enchantData!!.enchantType.displayName())?.deserialize()
                    },
                    { lore ->
                        if (lore.isNullOrEmpty()) return@withMeta lore
                        lore.mapToSerialize().tvarList("desc", enchantData!!.enchantType.menuItemInUpgradeMenu()).mapToDeserialize()
                    }
                )
            })
            val enchantType = enchantData!!.enchantType
            val enchantItemUpgradeResult = enchantItem.clone()
            EnchantManager.updateItemMeta(
                enchantItemUpgradeResult,
                enchantData!!.copy(stage = min(ConfigIEP.stageLevel.size, enchantData!!.stage + 1))
            )
            inv.setIcon(gum.upgradeResultSlot.slot, itemStack = enchantItemUpgradeResult)
            val upgradeMaterials = enchantType.upgradeMaterials()[enchantData!!.stage + 1]
            upgradeMaterials.forEachIndexed { index, enchantMaterial ->
                val materialItem = enchantMaterial.item.toItemStack()
                val materialDName = materialItem.getItemName()!!
                val materialLore = materialItem.lore() ?: emptyList()
                inv.setIcon(mSlot[index], gum.materialsSlot.default()!!.toItemStack(materialItem.clone()) {
                    withMeta(
                        {
                            it?.tvar("itemName", materialDName, "count", enchantMaterial.amount.toString().deserialize())
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
            inv.unSetIcon(gum.upgradeResultSlot.slot)
            inv.unSetIcon(gum.infoSlot.slot)
        }
    }

    override fun onClickPlayerInv(slot: Int, player: Player, inv: Inventory, event: InventoryClickEvent) {
        super.onClickPlayerInv(slot, player, inv, event)
        handlePutEnchantItem(slot, player, inv, event)
    }

    private fun handlePutEnchantItem(slot: Int, player: Player, inv: Inventory, event: InventoryClickEvent) {
        val pInv = event.clickedInventory ?: return
        val itemStack = pInv.getItem(slot) ?: return
        if (itemStack.isEmpty()) return
        val data = EnchantManager.getItemEnchant(itemStack) ?: return

        if (!data.hasMaxLevel) {
            player.sendLang("enchantUpgradeLevelUnqualified")
            return
        }
        if (data.stage >= ConfigIEP.stageLevel.size) {
            player.sendLang("enchantMaxStage")
            return
        }

        enchantData = data
        if (enchantItem.isEmpty()) {
            enchantItem = itemStack
            pInv.setItem(slot, null)
        } else {
            pInv.setItem(slot, enchantItem)
            enchantItem = itemStack
        }
        setup(ConfigIEP.enchantUpgradeMenuConfig.settings.copy(background = ConfigIEP.enchantUpgradeMenuConfig.backgroundEquipNoEmpty))
        updateMaterials(inv)
        updateTitle(player)
    }

    override fun onClose(player: Player, inv: Inventory, event: InventoryCloseEvent) {
        super.onClose(player, inv, event)
        if (!enchantItem.isEmpty()) {
            player.giveItem(enchantItem)
            player.updateInventory()
        }
    }

}