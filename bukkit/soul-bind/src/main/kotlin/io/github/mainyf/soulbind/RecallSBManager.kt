package io.github.mainyf.soulbind

import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.soulbind.config.ConfigSB
import io.github.mainyf.soulbind.storage.StorageSB
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

typealias RecallSBItemPredicate = (itemStack: ItemStack) -> Boolean

typealias RecallSBItemIDProvider = (itemStack: ItemStack) -> Long

object RecallSBManager {

    private val uidKey = NamespacedKey(SoulBind.INSTANCE, "uid")

    private val recallCountKey = NamespacedKey(SoulBind.INSTANCE, "recallCount")

    private val recallSBItemPredicates = mutableMapOf<String, RecallSBItemPredicate>()

    fun addRecallSBItemPredicate(providerName: String, predicate: RecallSBItemPredicate) {
        recallSBItemPredicates[providerName] = predicate
    }

    fun trySaveRecallItem(items: List<ItemStack>) {
        for (item in items) {
            tryUpdateRecallCount(item)
        }
    }

    fun getNextItemID(): Long {
        return StorageSB.nextItemLongID()
    }

    fun getItemID(itemStack: ItemStack): Long? {
        val meta = itemStack.itemMeta
        val dataContainer = meta.persistentDataContainer
        val root = dataContainer.get(SBManager.soulBindKey, PersistentDataType.TAG_CONTAINER) ?: return null
        return root.get(uidKey, PersistentDataType.LONG)
    }

    fun hasInvalidSBItem(itemStack: ItemStack?): Boolean {
        if (itemStack == null) return false
        val itemRecallCount = getBindItemRecallCount(itemStack)
        if (itemRecallCount == -1) return false
        val itemID = getItemID(itemStack) ?: return false
        val recallCount = StorageSB.getRecallCount(itemID)
        if (recallCount == -1) return false
        return itemRecallCount < recallCount
    }

    fun hasAbandonItem(itemStack: ItemStack?): Boolean {
        if (itemStack == null) return false
        val itemRecallCount = getBindItemRecallCount(itemStack)
        if (itemRecallCount == -1) return false
        val itemID = getItemID(itemStack) ?: return false
        return StorageSB.hasAbandonItem(itemID)
    }

    fun hasBindable(itemStack: ItemStack): Boolean {
        for ((_, predicate) in recallSBItemPredicates) {
            if (predicate.invoke(itemStack)) {
                return true
            }
        }
        return false
    }

    fun handleItemBind(player: Player, itemStack: ItemStack): ItemStack {
        //        val itemID = getItemID(itemStack) ?: return itemStack
        for ((_, predicate) in recallSBItemPredicates) {
            if (predicate.invoke(itemStack)) {
                bindItem(itemStack, player.uuid, player.name, getNextItemID())
                break
            }
        }
        return itemStack
    }

    fun bindItem(itemStack: ItemStack, ownerUUID: UUID, ownerName: String, itemId: Long) {
        val meta = itemStack.itemMeta
        val dataContainer = meta.persistentDataContainer
        val root = dataContainer.adapterContext.newPersistentDataContainer()
        root.set(SBManager.ownerUUIDMostTag, PersistentDataType.LONG, ownerUUID.mostSignificantBits)
        root.set(SBManager.ownerUUIDLeastTag, PersistentDataType.LONG, ownerUUID.leastSignificantBits)

        root.set(SBManager.ownerNameTag, PersistentDataType.STRING, ownerName)
        root.set(recallCountKey, PersistentDataType.INTEGER, 0)
        root.set(uidKey, PersistentDataType.LONG, itemId)

        dataContainer.set(
            SBManager.soulBindKey,
            PersistentDataType.TAG_CONTAINER,
            root
        )
        val lore = meta.lore() ?: mutableListOf()
        lore.addAll(ConfigSB.bindItemLore.map { it.tvar("player", ownerName) }.mapToDeserialize())
        meta.lore(lore)
        itemStack.itemMeta = meta
        StorageSB.updateRecallCount(ownerUUID, itemId, itemStack)
    }

    fun tryUpdateRecallCount(itemStack: ItemStack) {
        val bindData = SBManager.getBindItemData(itemStack) ?: return
        val itemID = getItemID(itemStack)
        if (itemID != null) {
            StorageSB.updateRecallCount(bindData.ownerUUID, itemID, itemStack)
        }
    }

    fun setRecallCount(itemStack: ItemStack, count: Int) {
        val meta = itemStack.itemMeta
        val dataContainer = meta.persistentDataContainer
        val root = dataContainer.get(SBManager.soulBindKey, PersistentDataType.TAG_CONTAINER) ?: return
        //        val recallCount = root.get(recallCountKey, PersistentDataType.INTEGER) ?: return
        root.set(recallCountKey, PersistentDataType.INTEGER, count)
        dataContainer.set(
            SBManager.soulBindKey,
            PersistentDataType.TAG_CONTAINER,
            root
        )
        itemStack.itemMeta = meta
    }

    fun markItemAbandon(itemStack: ItemStack) {
        val itemRecallCount = getBindItemRecallCount(itemStack)
        if (itemRecallCount == -1) return
        val itemID = getItemID(itemStack) ?: return
        StorageSB.markAbandonItem(itemID)
    }

    fun getBindItemRecallCount(itemStack: ItemStack): Int {
        val meta = itemStack.itemMeta
        val dataContainer =
            meta.persistentDataContainer.get(SBManager.soulBindKey, PersistentDataType.TAG_CONTAINER) ?: return -1
        return dataContainer.get(recallCountKey, PersistentDataType.INTEGER) ?: -1
    }

}