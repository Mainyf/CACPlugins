package io.github.mainyf.shopmanager.menu

import io.github.mainyf.newmclib.config.IaIcon
import io.github.mainyf.newmclib.exts.giveItem
import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.newmclib.hooks.giveMoney
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import io.github.mainyf.shopmanager.ShopManager
import io.github.mainyf.shopmanager.config.ConfigManager
import io.github.mainyf.shopmanager.config.sendLang
import io.github.mainyf.shopmanager.storage.StorageManager
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class SellMenu : AbstractMenuHandler() {

    private val itemMap = mutableMapOf<Int, ItemStack>()

    override fun open(player: Player) {
        setup(ConfigManager.sellMenuConfig.settings)

        val inv = createInv(player)

        updateInv(player, inv)
        player.openInventory(inv)
    }

    override fun updateTitle(player: Player): String {
        val sellMenuConfig = ConfigManager.sellMenuConfig
        val icons = mutableListOf<IaIcon>()
        icons.addAll(sellMenuConfig.placeholderSlot.iaIcon())
        icons.addAll(sellMenuConfig.sellSlot.iaIcon())

        return applyTitle(player, icons)
    }

    private fun updateInv(player: Player, inv: Inventory) {
        val pSlot = ConfigManager.sellMenuConfig.placeholderSlot
        inv.setIcon(pSlot)
        val sellSlot = ConfigManager.sellMenuConfig.sellSlot
        inv.setIcon(sellSlot) {
            sell(player, inv)
        }
    }

    private fun sell(player: Player, inv: Inventory) {
        val noSells = mutableListOf<ItemStack>()
        val itemMaterialGroupMap = mutableMapOf<Material, Int>()
        val itemStackGroupMap = mutableMapOf<Material, MutableList<ItemStack>>()
        itemMap.forEach { (slot, itemStack) ->
            val itemType = itemStack.type
            if (!ConfigManager.hasSellable(itemType)) {
                noSells.add(itemStack)
                return@forEach
            }
            if (itemMaterialGroupMap.containsKey(itemType)) {
                itemMaterialGroupMap[itemType] = itemMaterialGroupMap[itemType]!! + itemStack.amount
            } else {
                itemMaterialGroupMap[itemType] = itemStack.amount
            }
            itemStackGroupMap.getOrPut(itemType) { mutableListOf() }.add(itemStack)
        }
        var flag = false
        var allTotalPrice = 0.0
        itemMaterialGroupMap.forEach { (type, amount) ->
            val sellShop = ConfigManager.getSellShop(type)!!
            var totalPrice = sellShop.price * amount

            val sellCount = ShopManager.INSTANCE.getSellItemCount(player, type, sellShop)
            val langArr = sellShop.getLangArr(player, type, sellCount)
            if (amount > sellCount) {
                if (sellCount <= 0) {
                    player.sendLang(
                        "maxHarvest", *langArr
                    )
                } else {
                    flag = true
                    totalPrice = sellShop.price * sellCount
//                    player.sendLang(
//                        "sellSuccess",
//                        "{count}", sellCount,
//                        "{itemName}", Component.translatable(type),
//                        "{totalPrice}", totalPrice
//                    )
                    player.giveMoney(totalPrice)
                    allTotalPrice += totalPrice
                    StorageManager.updateHarvest(player.uuid, totalPrice, type)
                    var removedItemCount = sellCount
                    val itemList = itemStackGroupMap[type]!!
                    val iter = itemList.listIterator()
                    while (iter.hasNext()) {
                        val itemStack = iter.next()
                        if (itemStack.amount <= removedItemCount) {
                            iter.remove()
                            removedItemCount -= itemStack.amount
                        } else {
                            itemStack.amount -= removedItemCount
//                            removedItemCount = 0
                            break
                        }
                    }
                }
            } else {
                flag = true
//                player.sendLang(
//                    "sellSuccess",
//                    "{count}", amount,
//                    "{itemName}", Component.translatable(type),
//                    "{totalPrice}", totalPrice
//                )
                allTotalPrice += totalPrice
                player.giveMoney(totalPrice)
                itemStackGroupMap.remove(type)
                StorageManager.updateHarvest(player.uuid, totalPrice, type)
//                itemMaterialGroupMap[type] = 0
            }
        }
        itemMap.clear()
        inv.clear()
        noSells.forEach {
            itemMap[itemMap.size] = it
        }
        itemStackGroupMap.values.flatten().forEach {
            itemMap[itemMap.size] = it
        }
        itemMap.forEach {
            inv.setItem(it.key, it.value)
        }
        updateInv(player, inv)

        if (flag) {
            player.sendLang("fullItemSuccess", "{totalPrice}", allTotalPrice)
        }
        player.sendLang("fullItemSell")
    }

    override fun onClick(slot: Int, player: Player, inv: Inventory, event: InventoryClickEvent) {
        if (itemMap.containsKey(slot)) {
            inv.setItem(slot, null)
            val itemStack = itemMap.remove(slot)!!
            player.giveItem(itemStack)
        }
    }

    override fun onClose(player: Player, inv: Inventory, event: InventoryCloseEvent) {
        if (itemMap.isNotEmpty()) {
            itemMap.forEach { (slot, itemStack) ->
                inv.setItem(slot, null)
                player.giveItem(itemStack)
            }
        }
    }

    override fun onClickPlayerInv(slot: Int, player: Player, inv: Inventory, event: InventoryClickEvent) {
        super.onClickPlayerInv(slot, player, inv, event)
        val pInv = event.clickedInventory ?: return
        val itemStack = pInv.getItem(slot) ?: return
        if (!ConfigManager.hasSellable(itemStack.type)) {
            return
        }
        if (itemMap.size >= 45) return
        val freeSlot = inv.firstEmpty()
        if (freeSlot == -1) return
        inv.setItem(freeSlot, itemStack)
        pInv.setItem(slot, null)
        itemMap[freeSlot] = itemStack
    }

}