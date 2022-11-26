package io.github.mainyf.soulbind

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

    fun bindItem(itemStack: ItemStack, owner: Player) {
        val meta = itemStack.itemMeta
        val dataContainer = meta.persistentDataContainer
        val root = dataContainer.adapterContext.newPersistentDataContainer()
        val uuid = owner.uuid
        root.set(ownerUUIDMostTag, PersistentDataType.LONG, uuid.mostSignificantBits)
        root.set(ownerUUIDLeastTag, PersistentDataType.LONG, uuid.leastSignificantBits)

        root.set(ownerNameTag, PersistentDataType.STRING, owner.name)

        dataContainer.set(
            soulBindKey,
            PersistentDataType.TAG_CONTAINER,
            root
        )
        val lore = meta.lore() ?: mutableListOf()
        lore.addAll(ConfigSB.bindItemLore.map { it.tvar("player", owner.name) }.mapToDeserialize())
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
        val dataContainer = meta.persistentDataContainer
        val rootTag = dataContainer.get(soulBindKey, PersistentDataType.TAG_CONTAINER) ?: return null
        val ownerUUIDMost = rootTag.get(ownerUUIDMostTag, PersistentDataType.LONG)!!
        val ownerUUIDLeast = rootTag.get(ownerUUIDLeastTag, PersistentDataType.LONG)!!
        val ownerName = rootTag.get(ownerNameTag, PersistentDataType.STRING)!!
        return BindItemData(UUID(ownerUUIDMost, ownerUUIDLeast), ownerName)
    }

    data class BindItemData(
        val ownerUUID: UUID,
        val ownerName: String
    )

}