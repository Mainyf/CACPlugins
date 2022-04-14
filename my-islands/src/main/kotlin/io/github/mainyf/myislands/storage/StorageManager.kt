package io.github.mainyf.myislands.storage

import io.github.mainyf.myislands.MyIslands
import io.github.mainyf.newmclib.storage.BaseModel
import io.github.mainyf.newmclib.storage.GeneralStorage
import org.bukkit.util.Vector
import java.util.UUID

object StorageManager {

    private lateinit var storage: GeneralStorage<PlayerIslandData>

    fun init() {
        storage = GeneralStorage.of(MyIslands.INSTANCE.dataFolder.toPath().resolve("island-data"))
    }

    fun close() {
        storage.close()
    }

    fun createPlayerIsLand(uuid: UUID, coreLoc: Vector) {
        storage.add(
            PlayerIslandData(
                coreLoc,
                false,
                0
            ).apply {
                this.id = uuid
            }
        )
    }

    fun setVisibility(uuid: UUID, visibility: Boolean) {
        storage.update(uuid) {
            this.visibility = visibility
        }
    }

    fun addKudos(uuid: UUID, count: Int = 1) {
        storage.update(uuid) {
            this.kudos += count
        }
    }

    fun getIsLandsOrderByKudos(): List<PlayerIslandData> {
        return storage.findAll().sortedByDescending { it.kudos }
    }

    data class PlayerIslandData(
        var coreLoc: Vector = Vector(0, 0, 0),
        var visibility: Boolean = false,
        var kudos: Int = 0
    ) : BaseModel()

}