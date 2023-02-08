package io.github.mainyf.soulbind.menu

import io.github.mainyf.newmclib.config.IaIcon
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.hooks.money
import io.github.mainyf.newmclib.hooks.takeMoney
import io.github.mainyf.newmclib.menu.AbstractMenuHandler
import io.github.mainyf.soulbind.RecallSBManager
import io.github.mainyf.soulbind.config.ConfigSB
import io.github.mainyf.soulbind.config.sendLang
import io.github.mainyf.soulbind.storage.StorageSB
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.UUID
import kotlin.math.ceil

class RecallItemMenu(val uuid: UUID) : AbstractMenuHandler() {

    private var pageIndex = 1
    private var pageSize = 0
    private var maxPageIndex = 0

    private val items = mutableListOf<Pair<Long, ItemStack>>()

    private val currentItems = mutableListOf<Pair<Long, ItemStack>>()

    override fun open(player: Player) {
        this.pageSize = ConfigSB.recallItemMenuConfig.recallItemSlot.slot.size
        updateItems(player)
        updateCurrentItems()
        setup(ConfigSB.recallItemMenuConfig.settings)
        val inv = createInv(player)

        updateInv(player, inv)
        player.openInventory(inv)
    }

    private fun updateItems(player: Player) {
        this.items.clear()
        this.items.addAll(StorageSB.getPlayerRecallItems(uuid).map { it.key to it.value })
        this.maxPageIndex = ceil(
            items.size.toDouble() / pageSize.toDouble()
        ).toInt()
    }

    private fun updateCurrentItems() {
        currentItems.clear()
        currentItems.addAll(items.pagination(pageIndex, pageSize))
    }

    override fun updateTitle(player: Player): String {
        val rim = ConfigSB.recallItemMenuConfig
        val icons = mutableListOf<IaIcon>()
        icons.addAll(rim.recallItemSlot.iaIcon())
        icons.addAll(rim.prevSlot.iaIcon())
        icons.addAll(rim.nextSlot.iaIcon())
        icons.addAll(rim.infoSlot.iaIcon())
        icons.addAll(rim.backSlot.iaIcon())

        return applyTitle(player, icons)
    }

    private fun updateInv(player: Player, inv: Inventory) {
        val rim = ConfigSB.recallItemMenuConfig
        inv.setIcon(rim.infoSlot)
        inv.setIcon(rim.prevSlot) {
            if (pageIndex > 1) {
                pageIndex--
                updateCurrentItems()
                updateRecallItemsSlot(player, inv)
            }
        }
        inv.setIcon(rim.nextSlot) {
            if (pageIndex < maxPageIndex) {
                pageIndex++
                updateCurrentItems()
                updateRecallItemsSlot(player, inv)
            }
        }

        updateRecallItemsSlot(player, inv)

        inv.setIcon(rim.backSlot) {

        }
    }

    private fun updateRecallItemsSlot(player: Player, inv: Inventory) {
        val rim = ConfigSB.recallItemMenuConfig
        val recallItemSlot = rim.recallItemSlot.slot
        inv.setIcon(rim.recallItemSlot.slot, AIR_ITEM)
        val itemDisplay = rim.recallItemSlot.default()!!.itemDisplay
        currentItems.forEachIndexed { index, (itemID, itemStack) ->
            inv.setIcon(recallItemSlot[index], itemStack.toEquipItemSlot().apply {
                withMeta(
                    {
                        itemDisplay.name?.tvar("itemName", itemStack.itemMeta.displayName()!!.serialize())
                            ?.deserialize()
                            ?: itemStack.displayName()
                    },
                    { lore ->
                        if (lore.isNullOrEmpty()) return@withMeta lore
                        itemDisplay.lore?.tvarList("desc", itemStack.lore()?.mapToSerialize() ?: emptyList())
                            ?.mapToDeserialize() ?: lore
                    }
                )
            }, leftClickBlock = {
                rim.recallItemSlot.default()?.execAction(it)
                if (player.money() < ConfigSB.recallCost) {
                    player.sendLang("recallCostDeficiency", "{money}", ConfigSB.recallCost)
                    return@setIcon
                }
                player.takeMoney(ConfigSB.recallCost.toDouble())
                val newCount = StorageSB.addRecallCount(itemID)
                val giveToPlayerItemStack = itemStack.clone()
                RecallSBManager.setRecallCount(giveToPlayerItemStack, newCount)
                RecallSBManager.tryUpdateRecallCount(giveToPlayerItemStack)
                val itemIndex = items.indexOfFirst { (id, _) ->
                    id == itemID
                }
                if (itemIndex != -1) {
                    items[itemIndex] = itemID to itemStack
                }
                updateCurrentItems()
                player.inventory.forEachIndexed { index, itemStack ->
                    if (RecallSBManager.hasInvalidSBItem(itemStack)) {
                        player.inventory.setItem(index, AIR_ITEM)
                    }
                }
                player.giveItem(giveToPlayerItemStack)
                player.updateInventory()
                player.sendLang("recallSuccess")
            }, rightClickBlock = {
                if (!it.isOp) return@setIcon
                rim.recallItemSlot.default()?.execAction(it)
                AbandonConfirmMenu(
                    itemStack,
                    { p ->
                        if (player.money() < ConfigSB.abandonCost) {
                            player.sendLang("abandonCostDeficiency", "{money}", ConfigSB.abandonCost)
                            return@AbandonConfirmMenu
                        }
                        player.takeMoney(ConfigSB.abandonCost.toDouble())
                        RecallSBManager.markItemAbandon(itemStack)
                        player.inventory.forEachIndexed { index, itemStack ->
                            if (RecallSBManager.hasInvalidSBItem(itemStack)) {
                                player.inventory.setItem(index, AIR_ITEM)
                            }
                            if (RecallSBManager.hasAbandonItem(itemStack)) {
                                player.inventory.setItem(index, AIR_ITEM)
                            }
                        }
                        player.updateInventory()
                        RecallItemMenu(uuid).open(p)
                    },
                    { p ->
                        RecallItemMenu(uuid).open(p)
                    },

                    ).open(it)
            })
        }

    }


}