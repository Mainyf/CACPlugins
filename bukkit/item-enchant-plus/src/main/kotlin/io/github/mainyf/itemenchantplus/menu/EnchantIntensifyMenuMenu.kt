package io.github.mainyf.itemenchantplus.menu

import io.github.mainyf.itemenchantplus.EnchantData
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
import org.bukkit.inventory.ItemStack

class EnchantIntensifyMenuMenu : AbstractMenuHandler() {

    private var enchantType: ItemEnchantType? = null
    private var enchantItem = AIR_ITEM
    private val materials = mutableListOf<ItemStack>()

    override fun open(player: Player) {
        setup(ConfigIEP.enchantIntensifyMenuConfig.settings)
        val inv = createInv(player)

        updateInv(player, inv)
        clearMaterials()
        player.openInventory(inv)
    }

    fun clearMaterials() {
        materials.clear()
        repeat(10) {
            materials.add(AIR_ITEM)
        }
    }

    override fun updateTitle(player: Player): String {
        val eim = ConfigIEP.enchantIntensifyMenuConfig
        val icons = mutableListOf<IaIcon>()
        icons.addAll(eim.materialsSlot.iaIcon())
        icons.addAll(eim.equipSlot.iaIcon())
        icons.addAll(eim.backSlot.iaIcon())
        icons.addAll(eim.finishSlot.iaIcon())

        return applyTitle(player, icons)
    }

    private fun updateInv(player: Player, inv: Inventory) {
        val eim = ConfigIEP.enchantIntensifyMenuConfig
        updateEquip(inv)
        updateMaterials(inv)
        inv.setIcon(eim.backSlot) {
            DashboardMenu().open(it)
        }
        inv.setIcon(eim.finishSlot) {
            finish(it, inv)
        }
    }

    private fun finish(player: Player, inv: Inventory) {
        if (enchantItem.isEmpty()) {
            player.sendLang("giveItemEmpty")
            return
        }
        if (materials.all { it -> it.isEmpty() }) {
            return
        }
        var data = EnchantManager.getItemEnchant(enchantItem) ?: return

        val exp = materials.sumOf { materialItemStack ->
            (ConfigIEP.enchantIntensifyMaterials.find { it.key.equalsItem(materialItemStack) }
                ?: 0.0) * materialItemStack.amount
        }
        val needExp = EnchantManager.getToNextStageNeedExp(data)
        if (needExp <= 0.0) return
        if (exp > needExp) {
            val consumedItems = mutableListOf<Int>()
            var cExp = 0.0

            for (materialItemStack in materials) {
                val materialExp =
                    ConfigIEP.getMaterialExp(materialItemStack)
                var flag = true
                for (i in 1 .. materialItemStack.amount) {
                    cExp += materialExp
                    if (cExp >= needExp) {
                        flag = false
                        consumedItems.add(i)
                        break
                    }
                }
                if (flag) {
                    consumedItems.add(materialItemStack.amount)
                } else {
                    break
                }
            }
            consumedItems.forEachIndexed { index, amount ->
                val materialItemStack = materials[index]
                if (materialItemStack.amount == amount) {
                    materials[index] = AIR_ITEM
                } else {
                    materialItemStack.amount -= amount
                }
            }
            EnchantManager.addExpToItem(data.enchantType, enchantItem, needExp)
        } else {
            EnchantManager.addExpToItem(data.enchantType, enchantItem, exp)
            clearMaterials()
        }

        data = EnchantManager.getItemEnchant(enchantItem)!!
        EnchantManager.updateItemMeta(enchantItem, data)
        //        putBack(player, inv)

        //        player.giveItem(enchantItem)
        //        enchantItem = AIR_ITEM
        //
        updateEquip(inv)
        updateMaterials(inv)
        player.updateInventory()
        player.sendLang("enchantIntensifySuccess", "{enchantName}", enchantType!!.displayName())
    }

    private fun updateEquip(inv: Inventory) {
        val eim = ConfigIEP.enchantIntensifyMenuConfig
        inv.setIcon(eim.equipSlot.slot, itemStack = enchantItem.toEquipItemSlot()) { player ->
            if (enchantItem.isEmpty()) return@setIcon
            putBack(player, inv)
        }
    }

    private fun updateMaterials(inv: Inventory) {
        val eim = ConfigIEP.enchantIntensifyMenuConfig
        val mSlot = eim.materialsSlot.slot
        materials.forEachIndexed { index, itemStack ->
            inv.setIcon(mSlot[index], itemStack) {
                if (itemStack.isEmpty()) return@setIcon
                it.giveItem(itemStack)
                materials[index] = AIR_ITEM
                updateMaterials(inv)
            }
        }
    }

    override fun onClickPlayerInv(slot: Int, player: Player, inv: Inventory, event: InventoryClickEvent) {
        super.onClickPlayerInv(slot, player, inv, event)
        val pInv = event.clickedInventory ?: return
        val itemStack = pInv.getItem(slot) ?: return
        if (itemStack.isEmpty()) return
        val data = EnchantManager.getItemEnchant(itemStack)
        if (data != null) {
            handlePutEnchantItem(data, itemStack, slot, player, inv, pInv)
        } else {
            handlePutEnchantMaterials(itemStack, slot, player, inv, pInv)
        }
    }

    private fun handlePutEnchantMaterials(
        itemStack: ItemStack,
        slot: Int,
        player: Player,
        inv: Inventory,
        pInv: Inventory
    ) {
        if (enchantItem.isEmpty()) {
            player.sendLang("enchantItemEmptyPutMaterials")
            return
        }
        if (!ConfigIEP.enchantIntensifyMaterials.keys.any { it.equalsItem(itemStack) }) {
            player.sendLang("enchantMaterialsUnqualified")
            return
        }

        if (materials.any { it -> it.isSimilar(itemStack) }) {
            materials.forEach {
                if (!it.isSimilar(itemStack)) return@forEach
                if(itemStack.amount <= 0) {
                    return@forEach
                }
                if (it.amount >= it.maxStackSize) {
                    return@forEach
                }
                val rsAmount = itemStack.amount + it.amount
                if (rsAmount > it.maxStackSize) {
                    it.amount = it.maxStackSize
                    itemStack.amount = rsAmount - it.maxStackSize
                    pInv.setItem(slot, itemStack)
                } else {
                    pInv.setItem(slot, null)
                    it.amount = rsAmount
                    itemStack.amount = 0
                }
            }
            if (itemStack.amount > 0) {
                val freeIndex = materials.findIndex { it.isEmpty() }
                if (freeIndex != -1) {
                    materials[freeIndex] = itemStack
                    pInv.setItem(slot, null)
                }
            }
        } else {
            val freeIndex = materials.findIndex { it.isEmpty() }
            if (freeIndex == -1) {
                player.sendLang("enchantMaterialsNoFreeSlot")
                return
            }
            materials[freeIndex] = itemStack
            pInv.setItem(slot, null)
        }
        updateMaterials(inv)
    }

    private fun handlePutEnchantItem(
        data: EnchantData,
        itemStack: ItemStack,
        slot: Int,
        player: Player,
        inv: Inventory,
        pInv: Inventory
    ) {
        if (data.hasMaxLevel) {
            player.sendLang("enchantMaxLevel")
            return
        }
        if (data.hasMaxExp) {
            player.sendLang("enchantMaxExp")
            return
        }

        enchantType = data.enchantType
        if (enchantItem.isEmpty()) {
            enchantItem = itemStack
            pInv.setItem(slot, null)
        } else {
            putBack(player, inv)
            enchantItem = itemStack
            pInv.setItem(slot, AIR_ITEM)
        }
        updateTitle(player)
        updateEquip(inv)
        //        updateMaterials(inv)
        player.updateInventory()
    }

    override fun onClose(player: Player, inv: Inventory, event: InventoryCloseEvent) {
        super.onClose(player, inv, event)
        if (!enchantItem.isEmpty()) {
            putBack(player, inv)
        }
    }

    private fun putBack(player: Player, inv: Inventory) {
        player.giveItem(enchantItem)
        materials.forEach {
            if (!it.isEmpty()) {
                player.giveItem(it)
            }
        }
        enchantItem = AIR_ITEM
        updateMenu(player, inv)
    }

    private fun updateMenu(player: Player, inv: Inventory) {
        clearMaterials()
        player.updateInventory()
        updateEquip(inv)
        updateMaterials(inv)
        updateTitle(player)
    }

}