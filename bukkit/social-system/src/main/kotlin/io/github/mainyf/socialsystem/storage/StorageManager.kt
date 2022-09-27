package io.github.mainyf.socialsystem.storage

import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.newmclib.storage.AbstractStorageManager
import io.github.mainyf.newmclib.storage.newByID
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import java.util.*

object StorageManager : AbstractStorageManager() {

    override fun init() {
        super.init()
        transaction {
            arrayOf(
                PlayerSocials,
                PlayerFriends,
                PlayerFriendRequests,
                PlayerLinkQQs
            ).forEach {
                SchemaUtils.createMissingTablesAndColumns(it)
            }
        }
    }

    fun addFriend(playerA: UUID, playerB: UUID): PlayerFriend {
        return transaction {
            val playerASocial = getPlayerSocial(playerA)
            val playerBSocial = getPlayerSocial(playerB)
            PlayerFriend.newByID {

                this.social = playerBSocial.id
                this.friend = playerA
            }

            PlayerFriend.newByID {
                this.social = playerASocial.id
                this.friend = playerB
            }
        }
    }

    fun removeFriend(playerA: UUID, playerB: UUID) {
        transaction {
            val playerASocial = getPlayerSocial(playerA)
            val playerBSocial = getPlayerSocial(playerB)
            PlayerFriends.deleteWhere {
                (PlayerFriends.social eq playerASocial.id) and (PlayerFriends.friend eq playerB)
            }
            PlayerFriends.deleteWhere {
                (PlayerFriends.social eq playerBSocial.id) and (PlayerFriends.friend eq playerA)
            }
        }
    }

    fun getFriends(pUUID: UUID): List<PlayerFriend> {
        return transaction {
            val social = getPlayerSocial(pUUID)
            social.friends.toList()
        }
    }

    fun addFriendRequest(sender: UUID, receiver: UUID) {
        transaction {
            val social = getPlayerSocial(sender)
            val targetSocial = getPlayerSocial(receiver)
            PlayerFriendRequest.newByID {
                this.sender = social.id
                this.receiver = targetSocial.id
            }
        }
    }

    fun removeFriendRequest(sender: UUID, receiver: UUID) {
        transaction {
            val social = getPlayerSocial(sender)
            val targetSocial = getPlayerSocial(receiver)
            PlayerFriendRequests.deleteWhere {
                (PlayerFriendRequests.sender eq social.id) and (PlayerFriendRequests.receiver eq targetSocial.id)
            }
        }
    }

    fun getPlayerFriendList(pUUID: UUID): List<UUID> {
        return transaction {
            val social = getPlayerSocial(pUUID)
            social.friends.map { it.friend }
        }
    }

    fun inFriendRequests(sender: UUID, receiver: UUID): Boolean {
        return transaction {
            PlayerFriendRequest.find {
                (PlayerFriendRequests.receiver eq receiver) and (PlayerFriendRequests.sender eq sender)
            }.firstOrNull() != null
        }
    }

    fun getPlayerReceiveFriendRequests(receiver: UUID): List<UUID> {
        return transaction {
            val social = getPlayerSocial(receiver)
            social.receiveRequests.map { it.sender.value }
        }
    }

    fun getPlayerSendFriendRequests(sender: UUID): List<UUID> {
        return transaction {
            val social = getPlayerSocial(sender)
            social.sendRequests.map { it.sender.value }
        }
    }

    fun allowRepair(uuid: UUID): Boolean {
        return transaction {
            getPlayerSocial(uuid).allowRepair
        }
    }

    fun getPlayerSocial(uuid: UUID): PlayerSocial {
        return transaction {
            PlayerSocial.findById(uuid) ?: PlayerSocial.newByID(uuid) {}
        }
    }

    fun setAllowRepair(social: PlayerSocial, value: Boolean) {
        transaction {
            social.allowRepair = value
        }
    }

    fun getPlayerQQNum(uuid: UUID): Long? {
        return transaction {
            PlayerLinkQQs
                .select { PlayerLinkQQs.id eq uuid }
                .firstOrNull()
                ?.let {
                    it[PlayerLinkQQs.qqNum]
                }
        }
    }

}