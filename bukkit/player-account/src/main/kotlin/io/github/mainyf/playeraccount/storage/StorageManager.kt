package io.github.mainyf.playeraccount.storage

import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.newmclib.storage.AbstractStorageManager
import io.github.mainyf.newmclib.storage.newByID
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.SchemaUtils

object StorageManager : AbstractStorageManager() {

    override fun init() {
        super.init()
        transaction {
            arrayOf(
                PlayerAccountDatas
            ).forEach {
                SchemaUtils.createMissingTablesAndColumns(it)
            }
        }
    }

    fun getPlayerAccount(player: Player): PlayerAccountData? {
        return transaction {
            PlayerAccountData.findById(player.uuid)
        }
    }

    fun updatePlayerAccount(player: Player, phoneNumbers: String) {
        transaction {
            PlayerAccountData.newByID(player.uuid) {
                this.phoneNumber = phoneNumbers
            }
        }
    }

}