package io.github.mainyf.itemenchantplus.menu

import io.github.mainyf.itemenchantplus.EnchantData
import io.github.mainyf.itemenchantplus.EnchantManager
import io.github.mainyf.itemenchantplus.EnchantSkin
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
    private var currentSkinIndex = 0
    private var currentSkin: EnchantSkin? = null

    override fun open(player: Player) {
        this.pageSize = 5
        setup(ConfigIEP.enchantSkinMenuConfig.settings)
        val inv = createInv(player)

        updateInv(player, inv)
        player.openInventory(inv)
    }

    private fun updateEnchantSkins(player: Player) {
        if (enchantData == null) return
        this.enchantSkins.clear()
        this.enchantSkins.addAll(StorageIEP.getAllEnchantSkin(player.uuid, enchantData!!.enchantType).sortedBy {
            it.skinConfig.priority
        })
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
        if (currentSkin != null) {
            icons.add(currentSkin!!.skinConfig.menuBackground)
        } else {
            icons.addAll(ekm.largeSkinSlot.iaIcon())
        }
        icons.addAll(ekm.prevSlot.iaIcon())
        icons.addAll(ekm.nextSlot.iaIcon())
        if (!currentEnchantItem.isEmpty()) {
            listOf(
                ekm.enchantSkinX1Slot,
                ekm.enchantSkinX2Slot,
                ekm.enchantSkinX3Slot,
                ekm.enchantSkinX4Slot,
                ekm.enchantSkinX5Slot
            ).forEachIndexed { index, defaultSlotConfig ->
                icons.addAll(defaultSlotConfig.iaIcon(if (currentSkinIndex == index) "default" else "unSelect"))
            }
        }
        icons.addAll(ekm.finishSlot.iaIcon(if (currentEnchantItem == null) "default" else if (currentSkin!!.hasOwn) "haveSkin" else "select"))

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
        val enchantSkinSlots = listOf(
            ekm.enchantSkinX1Slot,
            ekm.enchantSkinX2Slot,
            ekm.enchantSkinX3Slot,
            ekm.enchantSkinX4Slot,
            ekm.enchantSkinX5Slot
        )
        val enchantSkinSlot = enchantSkinSlots.map { it.slot }.flatten()
        inv.unSetIcon(enchantSkinSlot)
        inv.unSetIcon(ekm.largeSkinSlot.slot)
        inv.unSetIcon(ekm.finishSlot.slot)

        kotlin.run {
            val itemKey =
                if (currentEnchantItem == null) "default" else if (currentSkin!!.hasOwn) "haveSkin" else "select"
            inv.setIcon(ekm.finishSlot, itemKey) {
                val currentItem = currentEnchantItem ?: return@setIcon
                val skin = currentSkin ?: return@setIcon
                val data = enchantData ?: return@setIcon
                if (skin.hasOwn) {
                    EnchantManager.setItemSkin(data.enchantType, currentItem, skin.skinConfig)
                    EnchantManager.updateItemMeta(currentItem)
                    it.sendLang("enchantSkinSetSuccess")
                } else {
                    skin.skinConfig.menuActions?.execute(it)
                }
            }
        }

        if (currentEnchantItem.isEmpty()) return
        inv.setIcon(ekm.largeSkinSlot.slot, itemStack = currentEnchantItem!!.toEquipItemSlot().apply {
            val skinEffect = currentSkin!!.skinConfig.skinEffect[currentSkin!!.stage - 1]
            val meta = itemMeta
            meta.enchants.forEach {
                meta.removeEnchant(it.key)
            }
            meta.setCustomModelData(skinEffect.menuLarge.customModelData)
            itemMeta = meta
            withMeta(
                displayName = skinEffect.menuLarge.name.deserialize(),
                lore = skinEffect.menuLarge.lore.mapToDeserialize()
            )
            //            setDisplayName {
            //                skinEffect.menuItemName.deserialize()
            //            }
            //            lore(skinEffect.menuItemLore.mapToDeserialize())
        })

        currentEnchantSkins.forEachIndexed { index, enchantSkin ->
            val skinEffect = enchantSkin.skinConfig.skinEffect[enchantSkin.stage - 1]
            inv.setIcon(enchantSkinSlots[index], itemStack = currentEnchantItem!!.toEquipItemSlot().apply {
                val meta = itemMeta
                meta.enchants.forEach {
                    meta.removeEnchant(it.key)
                }
                meta.setCustomModelData(skinEffect.customModelData)
                itemMeta = meta
                setDisplayName {
                    skinEffect.menuItemName.deserialize()
                }
                lore(skinEffect.menuItemLore.mapToDeserialize())
            }) {
                if (currentSkinIndex == index) {
                    enchantSkinSlots[index].default()?.execAction(it)
                } else {
                    enchantSkinSlots[index]["unSelect"]?.execAction(it)
                }
                currentSkin = enchantSkin
                currentSkinIndex = index
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
        updateEnchantSkins(player)
        updateCurrentEnchantSkins()
        currentSkinIndex = currentEnchantSkins.indexOfFirst { it.skinConfig.name == currentSkin!!.skinConfig.name }
        if (currentEnchantItem.isEmpty()) {
            currentEnchantItem = itemStack
            pInv.setItem(slot, null)
        } else {
            pInv.setItem(slot, currentEnchantItem)
            currentEnchantItem = itemStack
        }
        updateEnchantSkinSlot(player, inv)
        updateTitle(player)
    }

    override fun onClose(player: Player, inv: Inventory, event: InventoryCloseEvent) {
        super.onClose(player, inv, event)
        if (!currentEnchantItem.isEmpty()) {
            player.giveItem(currentEnchantItem!!)
            player.updateInventory()
        }
    }

}