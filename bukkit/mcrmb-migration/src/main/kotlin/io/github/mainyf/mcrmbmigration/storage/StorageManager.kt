package io.github.mainyf.mcrmbmigration.storage

import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.newmclib.storage.AbstractStorageManager
import io.github.mainyf.newmclib.storage.newByID
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.SchemaUtils
import java.util.UUID

object StorageManager : AbstractStorageManager() {

    override fun init() {
        super.init()
        transaction {
            arrayOf(
                PlayerClaimOldPoints
            ).forEach {
                SchemaUtils.createMissingTablesAndColumns(it)
            }
        }
    }

    fun getClaimed(uuid: UUID): PlayerClaimOldPoint? {
        return transaction {
            PlayerClaimOldPoint.findById(uuid)
        }
    }

    fun onPlayerClaimOldPoint(player: Player, value: Double) {
        transaction {
            PlayerClaimOldPoint.newByID(player.uuid) {
                this.value = value
            }
        }
    }

}