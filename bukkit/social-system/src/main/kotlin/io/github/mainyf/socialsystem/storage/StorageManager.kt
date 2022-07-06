package io.github.mainyf.socialsystem.storage

import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.newmclib.storage.AbstractStorageManager
import io.github.mainyf.newmclib.storage.newByID
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.*
import java.util.UUID

object StorageManager : AbstractStorageManager() {

    override fun init() {
        super.init()
        transaction {
            arrayOf(
                PlayerSocials,
                PlayerFriends,
                PlayerFriendRequests
            ).forEach {
                SchemaUtils.createMissingTablesAndColumns(it)
            }
        }
    }

    fun sendFriendRequest(player: Player, target: UUID) {
        transaction {
            val social = getPlayerSocial(player.uuid)
            val targetSocial = getPlayerSocial(target)
            PlayerFriendRequest.newByID {
                this.sender = social.id
                this.receiver = targetSocial.id
            }
        }
    }

    private fun getPlayerSocial(uuid: UUID): PlayerSocial {
        val rs = PlayerSocial.findById(uuid)
        if (rs == null) {
            return PlayerSocial.newByID(uuid) {}
        }
        return rs
    }

}