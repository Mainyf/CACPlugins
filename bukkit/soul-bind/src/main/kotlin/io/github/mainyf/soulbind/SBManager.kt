package io.github.mainyf.soulbind

import io.github.mainyf.newmclib.exts.equalsByIaNamespaceID
import io.github.mainyf.newmclib.exts.mapToDeserialize
import io.github.mainyf.newmclib.exts.tvar
import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.soulbind.config.ConfigSB
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

object SBManager {

    val soulBindKey = NamespacedKey(SoulBind.INSTANCE, "soulBind")
    val ownerUUIDMostTag = NamespacedKey(SoulBind.INSTANCE, "ownerUUIDMost")
    val ownerUUIDLeastTag = NamespacedKey(SoulBind.INSTANCE, "ownerUUIDLeast")
    val ownerNameTag = NamespacedKey(SoulBind.INSTANCE, "soulBindOwnerName")
    val bindableTag = NamespacedKey(SoulBind.INSTANCE, "soulBindBindable")

    fun markBindableTag(itemStack: ItemStack) {
        val meta = itemStack.itemMeta
        val dataContainer = meta.persistentDataContainer
        dataContainer.set(bindableTag, PersistentDataType.INTEGER, 1)
        itemStack.itemMeta = meta
    }

    fun hasMarkBindableTag(itemStack: ItemStack): Boolean {
        val meta = itemStack.itemMeta ?: return false
        val dataContainer = meta.persistentDataContainer
        return dataContainer.has(bindableTag)
    }

    fun hasBindable(itemStack: ItemStack): Boolean {
        for (iaId in ConfigSB.autoBindIAList) {
            if (itemStack.equalsByIaNamespaceID(iaId)) {
                return true
            }
        }
        val meta = itemStack.itemMeta
        val dataContainer = meta.persistentDataContainer
        if (dataContainer.has(bindableTag)) {
            return true
        }
        return false
    }

    fun handleItemBind(player: Player, itemStack: ItemStack): ItemStack {
        if(hasBindable(itemStack)) {
            bindItem(itemStack, player.uuid, player.name)
        }
//        for (iaId in ConfigSB.autoBindIAList) {
//            if (itemStack.equalsByIaNamespaceID(iaId)) {
//                bindItem(itemStack, player.uuid, player.name)
//                break
//            }
//        }
        return itemStack
    }

    fun bindItem(itemStack: ItemStack, ownerUUID: UUID, ownerName: String) {
        val meta = itemStack.itemMeta
        val dataContainer = meta.persistentDataContainer
        val root = dataContainer.adapterContext.newPersistentDataContainer()
        root.set(ownerUUIDMostTag, PersistentDataType.LONG, ownerUUID.mostSignificantBits)
        root.set(ownerUUIDLeastTag, PersistentDataType.LONG, ownerUUID.leastSignificantBits)

        root.set(ownerNameTag, PersistentDataType.STRING, ownerName)

        dataContainer.set(
            soulBindKey,
            PersistentDataType.TAG_CONTAINER,
            root
        )
        val lore = meta.lore() ?: mutableListOf()
        lore.addAll(ConfigSB.bindItemLore.map { it.tvar("player", ownerName) }.mapToDeserialize())
        meta.lore(lore)
        itemStack.itemMeta = meta
    }

    fun hasBindItem(itemStack: ItemStack?): Boolean {
        val meta = itemStack?.itemMeta ?: return false
        val dataContainer = meta.persistentDataContainer
        return dataContainer.has(soulBindKey)
    }

    fun getBindItemData(itemStack: ItemStack?): BindItemData? {
        val meta = itemStack?.itemMeta ?: return null
        val dataContainer =
            meta.persistentDataContainer
        val rootTag = dataContainer.get(soulBindKey, PersistentDataType.TAG_CONTAINER) ?: return null
        val ownerUUIDMost = rootTag.get(ownerUUIDMostTag, PersistentDataType.LONG)!!
        val ownerUUIDLeast = rootTag.get(ownerUUIDLeastTag, PersistentDataType.LONG)!!
        val ownerName = rootTag.get(ownerNameTag, PersistentDataType.STRING)!!
        return BindItemData(
            UUID(ownerUUIDMost, ownerUUIDLeast),
            ownerName,
            RecallSBManager.getBindItemRecallCount(itemStack)
        )
    }

    data class BindItemData(
        val ownerUUID: UUID,
        val ownerName: String,
        val recallCount: Int
    )

}