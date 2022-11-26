package io.github.mainyf.soulbind

import io.github.mainyf.newmclib.exts.mapToDeserialize
import io.github.mainyf.newmclib.exts.tvar
import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.soulbind.config.ConfigSB
import io.github.mainyf.soulbind.storage.StorageSB
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object RecallSBManager {

    private val recallCountKey = NamespacedKey(SoulBind.INSTANCE, "recallCount")

    fun bindItem(itemStack: ItemStack, owner: Player, itemId: Long) {
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
        StorageSB.createRecallCount(itemId)
    }

}