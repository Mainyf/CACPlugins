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

    private val recallCountKey = NamespacedKey(SoulBind.INSTANCE, "recallCount")

    private val recallSBItemPredicates = mutableMapOf<String, RecallSBItemPredicate>()
    private val recallSBItemIDProviders = mutableMapOf<String, RecallSBItemIDProvider>()

    fun addRecallSBItemPredicate(providerName: String, predicate: RecallSBItemPredicate) {
        recallSBItemPredicates[providerName] = predicate
    }

    fun addRecallSBItemIDProvider(providerName: String, provider: RecallSBItemIDProvider) {
        recallSBItemIDProviders[providerName] = provider
    }

    fun trySaveRecallItem(items: List<ItemStack>) {
        for (item in items) {
            tryUpdateRecallCount(item)
        }
    }

    private fun getItemID(providerName: String, itemID: Long): UUID {
        return "${providerName}-${itemID}".asUUIDFromByte()
    }

    fun getItemID(itemStack: ItemStack): UUID? {
        var rs: UUID? = null
        for ((providerName, provider) in recallSBItemIDProviders) {
            val itemID = provider.invoke(itemStack)
            if (itemID == -1L) continue
            rs = getItemID(providerName, itemID)
            break
        }
        return rs
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
        val itemID = getItemID(itemStack) ?: return itemStack
        for ((_, predicate) in recallSBItemPredicates) {
            if (predicate.invoke(itemStack)) {
                bindItem(itemStack, player, itemID)
            }
        }
        return itemStack
    }

    fun bindItem(itemStack: ItemStack, owner: Player, itemId: UUID) {
        val meta = itemStack.itemMeta
        val dataContainer = meta.persistentDataContainer
        val root = dataContainer.adapterContext.newPersistentDataContainer()
        val uuid = owner.uuid
        root.set(SBManager.ownerUUIDMostTag, PersistentDataType.LONG, uuid.mostSignificantBits)
        root.set(SBManager.ownerUUIDLeastTag, PersistentDataType.LONG, uuid.leastSignificantBits)

        root.set(SBManager.ownerNameTag, PersistentDataType.STRING, owner.name)
        root.set(recallCountKey, PersistentDataType.INTEGER, 0)

        dataContainer.set(
            SBManager.soulBindKey,
            PersistentDataType.TAG_CONTAINER,
            root
        )
        val lore = meta.lore() ?: mutableListOf()
        lore.addAll(ConfigSB.bindItemLore.map { it.tvar("player", owner.name) }.mapToDeserialize())
        meta.lore(lore)
        itemStack.itemMeta = meta
        StorageSB.updateRecallCount(owner.uuid, itemId, itemStack)
    }

    fun tryUpdateRecallCount(itemStack: ItemStack) {
        val bindData = SBManager.getBindItemData(itemStack) ?: return
        for ((providerName, provider) in recallSBItemIDProviders) {
            val itemID = provider.invoke(itemStack)
            if (itemID != -1L) {
                StorageSB.updateRecallCount(bindData.ownerUUID, getItemID(providerName, itemID), itemStack)
            }
        }
    }

    fun addRecallCount(itemStack: ItemStack) {
        val meta = itemStack.itemMeta
        val dataContainer = meta.persistentDataContainer
        val root = dataContainer.get(SBManager.soulBindKey, PersistentDataType.TAG_CONTAINER) ?: return
        val recallCount = root.get(recallCountKey, PersistentDataType.INTEGER) ?: return
        root.set(recallCountKey, PersistentDataType.INTEGER, recallCount + 1)
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