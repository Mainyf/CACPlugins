package io.github.mainyf.worldsettings

import io.github.mainyf.newmclib.exts.ROOT_UUID
import io.github.mainyf.newmclib.storage.BaseModel
import io.github.mainyf.newmclib.storage.GeneralStorage
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

object PlayerDropItemStorage {

    private lateinit var storage: GeneralStorage<DropItemData>

    fun init(plugin: JavaPlugin) {
        storage = GeneralStorage.of(plugin.dataFolder.toPath().resolve("drop-item"))
    }

    fun close() {
        storage.close()
    }

    fun addData(player: Player, item: Item) {
        storage.add(DropItemData(item.world.name, player.uniqueId, item.uniqueId))
    }

    fun get(player: Player, item: Item): DropItemData? {
        return storage.find { it.world == item.world.name && it.pUUID != player.uniqueId && it.itemUUID == item.uniqueId }
    }

    fun removeData(item: Item) {
        val data = storage.find { it.world == item.world.name && it.itemUUID == item.uniqueId }
        if (data != null) {
            storage.remove(data.id)
        }
    }

    data class DropItemData(
        val world: String = "",
        val pUUID: UUID = ROOT_UUID,
        val itemUUID: UUID = ROOT_UUID
    ) : BaseModel()

}