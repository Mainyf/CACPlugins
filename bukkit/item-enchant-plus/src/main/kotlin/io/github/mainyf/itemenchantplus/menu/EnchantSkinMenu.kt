package io.github.mainyf.itemenchantplus.menu

import io.github.mainyf.itemenchantplus.EnchantData
import io.github.mainyf.itemenchantplus.EnchantManager
import io.github.mainyf.itemenchantplus.EnchantSkin
import io.github.mainyf.itemenchantplus.config.ConfigIEP
import io.github.mainyf.itemenchantplus.storage.StorageIEP
import io.github.mainyf.newmclib.config.IaIcon
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import kotlin.math.ceil

class EnchantSkinMenu : AbstractMenuHandler() {

    private var pageIndex = 1
    private var pageSize = 0
    private var maxPageIndex = 0

    private val enchantSkins = mutableListOf<EnchantSkin>()

    private val currentEnchantSkins = mutableListOf<EnchantSkin>()

    private var currentEnchantItem: ItemStack? = null
    private var enchantData: EnchantData? = null
    private var currentSkin: EnchantSkin? = null

    override fun open(player: Player) {
        this.pageSize = ConfigIEP.enchantSkinMenuConfig.enchantSkinSlot.slot.size
        setup(ConfigIEP.enchantSkinMenuConfig.settings)
        val inv = createInv(player)

        updateInv(player, inv)
        player.openInventory(inv)
    }

    private fun updateEnchantSkins(player: Player) {
        if (enchantData == null) return
        this.enchantSkins.clear()
        this.enchantSkins.addAll(StorageIEP.getAllEnchantSkin(player.uuid, enchantData!!.enchantType))
        this.maxPageIndex = ceil(
            enchantSkins.size.toDouble() / pageSize.toDouble()
        ).toInt()
    }

    private fun updateCurrentEnchantSkins() {
        currentEnchantSkins.clear()
        currentEnchantSkins.addAll(enchantSkins.pagination(pageIndex, pageSize))
    }

    override fun updateTitle(player: Player): String {
        val ekm = ConfigIEP.enchantSkinMenuConfig
        val icons = mutableListOf<IaIcon>()
        icons.addAll(ekm.largeSkinSlot.iaIcon())
        icons.addAll(ekm.prevSlot.iaIcon())
        icons.addAll(ekm.nextSlot.iaIcon())
        icons.addAll(ekm.enchantSkinSlot.iaIcon())

        return applyTitle(player, icons)
    }

    private fun updateInv(player: Player, inv: Inventory) {
        val ekm = ConfigIEP.enchantSkinMenuConfig
        inv.setIcon(ekm.prevSlot) {
            if (pageIndex > 1) {
                pageIndex--
                updateCurrentEnchantSkins()
                updateEnchantSkinSlot(player, inv)
            }
        }
        inv.setIcon(ekm.nextSlot) {
            if (pageIndex < maxPageIndex) {
                pageIndex++
                updateCurrentEnchantSkins()
                updateEnchantSkinSlot(player, inv)
            }
        }

        updateEnchantSkinSlot(player, inv)

        inv.setIcon(ekm.backSlot)
    }

    private fun updateEnchantSkinSlot(player: Player, inv: Inventory) {
        val ekm = ConfigIEP.enchantSkinMenuConfig
        val enchantSkinSlot = ekm.enchantSkinSlot.slot
        inv.setIcon(enchantSkinSlot, AIR_ITEM)
        inv.unSetIcon(ekm.largeSkinSlot.slot)
        if (currentEnchantItem.isEmpty()) return
        inv.setIcon(ekm.largeSkinSlot.slot, itemStack = currentEnchantItem!!.clone().apply {
            val skinEffect = currentSkin!!.skinConfig.skinEffect[currentSkin!!.stage - 1]
            val meta = itemMeta
            meta.setCustomModelData(skinEffect.customModelData)
            itemMeta = meta
            setDisplayName {
                skinEffect.menuItemName.deserialize()
            }
            lore(skinEffect.menuItemLore.mapToDeserialize())
        })
        currentEnchantSkins.forEachIndexed { index, enchantSkin ->
            val skinEffect = enchantSkin.skinConfig.skinEffect[enchantSkin.stage - 1]
            inv.setIcon(enchantSkinSlot[index], currentEnchantItem!!.clone().apply {
                val meta = itemMeta
                meta.setCustomModelData(skinEffect.customModelData)
                itemMeta = meta
                setDisplayName {
                    skinEffect.menuItemName.deserialize()
                }
                lore(skinEffect.menuItemLore.mapToDeserialize())
            }) {
                ekm.enchantSkinSlot.default()?.execAction(it)
                currentSkin = enchantSkin
                updateEnchantSkins(player)
                updateCurrentEnchantSkins()
                updateEnchantSkinSlot(player, inv)
            }
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

        enchantData = data
        currentSkin = data.enchantSkin
        if (currentEnchantItem.isEmpty()) {
            currentEnchantItem = itemStack
            pInv.setItem(slot, null)
        } else {
            pInv.setItem(slot, currentEnchantItem)
            currentEnchantItem = itemStack
        }
        updateEnchantSkins(player)
        updateCurrentEnchantSkins()
        updateEnchantSkinSlot(player, inv)
    }

    override fun onClose(player: Player, inv: Inventory, event: InventoryCloseEvent) {
        super.onClose(player, inv, event)
        if (!currentEnchantItem.isEmpty()) {
            player.giveItem(currentEnchantItem!!)
            player.updateInventory()
        }
    }

}