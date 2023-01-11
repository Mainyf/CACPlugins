package io.github.mainyf.shopmanager.menu

import io.github.mainyf.newmclib.config.IaIcon
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.hooks.giveMoney
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import io.github.mainyf.newmclib.nms.asNmsPlayer
import io.github.mainyf.shopmanager.ShopManager
import io.github.mainyf.shopmanager.config.ConfigManager
import io.github.mainyf.shopmanager.config.sendLang
import io.github.mainyf.shopmanager.storage.StorageManager
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class SellMenu : AbstractMenuHandler() {

    private val items = newItemList(45)

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
        updateItems(inv)
        val pSlot = ConfigManager.sellMenuConfig.placeholderSlot
        inv.setIcon(pSlot)
        val sellSlot = ConfigManager.sellMenuConfig.sellSlot
        inv.setIcon(sellSlot) {
            sell(player, inv)
        }
    }

    private fun updateItems(inv: Inventory) {
        items.forEachIndexed { index, itemStack ->
            inv.setIcon(index, itemStack) {
                //                if (itemStack.isEmpty()) return@setIcon
                //                items[index] = AIR_ITEM
                //                if (it.giveItem(itemStack)) {
                //                    it.sendLang("unSellItemBack")
                //                }
                //                updateItems(inv)
            }
        }
    }

    private fun sell(player: Player, inv: Inventory) {
        val noSells = mutableListOf<ItemStack>()
        val itemMaterialGroupMap = mutableMapOf<Material, Int>()
        val itemStackGroupMap = mutableMapOf<Material, MutableList<ItemStack>>()
        items.forEach { itemStack ->
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
        items.clear()
        items.addAll(newItemList(45))
        inv.clear()
        noSells.forEach {
            mergeItems(it, items)
        }
        itemStackGroupMap.values.flatten().forEach {
            mergeItems(it, items)
        }
        items.forEachIndexed { index, itemStack ->
            inv.setItem(index, itemStack)
        }
        updateInv(player, inv)

        if (flag) {
            player.sendLang("fullItemSuccess", "{totalPrice}", allTotalPrice)
        }
        player.sendLang("fullItemSell")
    }

    override fun onClose(player: Player, inv: Inventory, event: InventoryCloseEvent) {
        if (items.isNotEmpty()) {
            var flag = false
            items.forEachIndexed { slot, itemStack ->
                inv.setItem(slot, null)
                flag = player.giveItem(itemStack)
            }
            if (flag) {
                player.sendLang("unSellItemBack")
            }
        }
    }

    override fun onClick(slot: Int, player: Player, inv: Inventory, event: InventoryClickEvent) {
        val itemStack = items.getOrNull(slot) ?: return
        if (itemStack.isEmpty()) return
        if (event.isLeftClick && event.isShiftClick) {
            for (i in 0 until items.size) {
                val toItem = items[i]
                if (!itemStack.isSimilar(toItem)) continue
                if (itemStack.isEmpty()) continue
                putItemToInventory(
                    player,
                    i,
                    toItem,
                    items,
                    player.inventory
                )
            }
        } else {
            items[slot] = AIR_ITEM
            if (player.giveItem(itemStack)) {
                player.sendLang("unSellItemBack")
            }
        }
        updateItems(inv)
    }

    override fun onClickPlayerInv(slot: Int, player: Player, inv: Inventory, event: InventoryClickEvent) {
        super.onClickPlayerInv(slot, player, inv, event)
        val pInv = event.clickedInventory ?: return
        val itemStack = pInv.getItem(slot) ?: return
        if (!ConfigManager.hasSellable(itemStack.type)) {
            return
        }
        val freeSlot = inv.firstEmpty()
        if (freeSlot == -1) return
        if (event.isLeftClick && event.isShiftClick) {
            for (i in 0 until pInv.size) {
                val toItem = pInv.getItem(i)
                if (!itemStack.isSimilar(toItem)) continue
                if (itemStack.isEmpty()) continue
                val response = putItemToInventory(
                    i,
                    toItem!!,
                    pInv,
                    items
                )
                if (response == PutItemResponse.FREE_SLOT_LACK) {
                    break
                }
            }
        } else {
            putItemToInventory(
                slot,
                itemStack,
                pInv,
                items
            )
        }
        updateItems(inv)
        //        inv.setItem(freeSlot, itemStack)
        //        pInv.setItem(slot, null)
        //        items[freeSlot] = itemStack
    }

    enum class PutItemResponse {
        FREE_SLOT_LACK,
        SUCCESS
    }

    fun newItemList(size: Int): MutableList<ItemStack> {
        val rs = mutableListOf<ItemStack>()
        repeat(size) {
            rs.add(AIR_ITEM)
        }
        return rs
    }

    fun putItemToInventory(
        slot: Int,
        itemStack: ItemStack,
        fromInv: Inventory,
        toInv: MutableList<ItemStack>
    ): PutItemResponse {
        if (toInv.any { it -> it.isSimilar(itemStack) }) {
            toInv.forEach {
                if (!it.isSimilar(itemStack)) return@forEach
                if (itemStack.amount <= 0) {
                    return@forEach
                }
                if (it.amount >= it.maxStackSize) {
                    return@forEach
                }
                val rsAmount = itemStack.amount + it.amount
                if (rsAmount > it.maxStackSize) {
                    it.amount = it.maxStackSize
                    itemStack.amount = rsAmount - it.maxStackSize
                    fromInv.setItem(slot, itemStack)
                } else {
                    fromInv.setItem(slot, null)
                    it.amount = rsAmount
                    itemStack.amount = 0
                }
            }
            if (itemStack.amount > 0) {
                val freeIndex = toInv.findIndex { it.isEmpty() }
                if (freeIndex == -1) {
                    return PutItemResponse.FREE_SLOT_LACK
                }
                toInv[freeIndex] = itemStack
                fromInv.setItem(slot, null)
            }
        } else {
            val freeIndex = toInv.findIndex { it.isEmpty() }
            if (freeIndex == -1) {
                return PutItemResponse.FREE_SLOT_LACK
            }
            toInv[freeIndex] = itemStack
            fromInv.setItem(slot, null)
        }
        return PutItemResponse.SUCCESS
    }

    fun putItemToInventory(
        player: Player,
        slot: Int,
        itemStack: ItemStack,
        fromInv: MutableList<ItemStack>,
        toInv: Inventory
    ) {
        if (toInv.any { it?.isSimilar(itemStack) == true }) {
            toInv.forEach {
                if (it.isEmpty()) return@forEach
                if (!it.isSimilar(itemStack)) return@forEach
                if (itemStack.amount <= 0) {
                    return@forEach
                }
                if (it.amount >= it.maxStackSize) {
                    return@forEach
                }
                val rsAmount = itemStack.amount + it.amount
                if (rsAmount > it.maxStackSize) {
                    it.amount = it.maxStackSize
                    itemStack.amount = rsAmount - it.maxStackSize
                    fromInv[slot] = itemStack
                } else {
                    fromInv[slot] = AIR_ITEM
                    it.amount = rsAmount
                    itemStack.amount = 0
                }
            }
            if (itemStack.amount > 0) {
                val leftover = toInv.addItem(itemStack)
                val nmsPlayer = player.asNmsPlayer()
                leftover.forEach {
                    nmsPlayer.dropItemNaturally(it.value)
                }
                fromInv[slot] = AIR_ITEM
            }
        } else {
            val leftover = toInv.addItem(itemStack)
            val nmsPlayer = player.asNmsPlayer()
            leftover.forEach {
                nmsPlayer.dropItemNaturally(it.value)
            }
            fromInv[slot] = AIR_ITEM
        }
    }

    private fun mergeItems(itemStack: ItemStack, itemList: MutableList<ItemStack>) {
        if (itemList.any { it -> it.isSimilar(itemStack) }) {
            itemList.forEach {
                if (!it.isSimilar(itemStack)) return@forEach
                if (itemStack.amount <= 0) {
                    return@forEach
                }
                if (it.amount >= it.maxStackSize) {
                    return@forEach
                }
                val rsAmount = itemStack.amount + it.amount
                if (rsAmount > it.maxStackSize) {
                    it.amount = it.maxStackSize
                    itemStack.amount = rsAmount - it.maxStackSize
                } else {
                    it.amount = rsAmount
                    itemStack.amount = 0
                }
            }
            if (itemStack.amount > 0) {
                val freeIndex = itemList.findIndex { it.isEmpty() }
                if (freeIndex != -1) {
                    itemList[freeIndex] = itemStack
                }
            }
        } else {
            val freeIndex = itemList.findIndex { it.isEmpty() }
            if (freeIndex != -1) {
                itemList[freeIndex] = itemStack
            }
        }
    }

}