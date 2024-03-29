package io.github.mainyf.socialsystem.module

import io.github.mainyf.bungeesettingsbukkit.CrossServerManager
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.offline_player_ext.OfflinePlayerData
import io.github.mainyf.newmclib.offline_player_ext.asOfflineData
import io.github.mainyf.socialsystem.SocialSystem
import io.github.mainyf.socialsystem.config.sendLang
import io.github.mainyf.socialsystem.storage.PlayerFriend
import io.github.mainyf.socialsystem.storage.PlayerSocial
import io.github.mainyf.socialsystem.storage.StorageSS
import io.netty.buffer.ByteBuf
import org.bukkit.entity.Player
import java.util.UUID

object FriendHandler {


    fun allowRepair(target: UUID): Boolean {
        return StorageSS.allowRepair(target)
    }

    fun getFriends(player: Player): List<OfflinePlayerData> {
        return StorageSS.getFriends(player.uuid).map { it to it.friend.asOfflineData() }
            .sortedWith { (a, offlineDataA), (b, offlineDataB) ->
                val aOnline = isOnline(a.friend)
                val bOnline = isOnline(b.friend)
                when {
                    aOnline && !bOnline -> -1
                    !aOnline && bOnline -> 1
                    else -> comparator(a, offlineDataA, b, offlineDataB)
                }
            }.mapNotNull { it.second }
    }

    private fun comparator(
        a: PlayerFriend,
        offlineDataA: OfflinePlayerData?,
        b: PlayerFriend,
        offlineDataB: OfflinePlayerData?
    ): Int {
        return when {
            a.intimacy > b.intimacy -> -1
            a.intimacy < b.intimacy -> 1
            a.intimacy == b.intimacy -> {
                (offlineDataA?.name ?: "").compareTo(offlineDataB?.name ?: "")
            }

            else -> -1
        }
    }

    private fun isOnline(uuid: UUID): Boolean {
        return CrossServerManager.isOnline(uuid)
    }

    fun removeFriend(player: Player, target: UUID) {
        StorageSS.removeFriend(player.uuid, target)
    }

    fun isFriend(player: Player, target: UUID): Boolean {
        return StorageSS.getPlayerFriendList(player.uuid).contains(target)
    }

    fun getSendFriendRequests(player: Player): List<String> {
        return StorageSS.getPlayerSendFriendRequests(player.uuid).map { it.asOfflineData()?.name!! }
    }

    fun getReceiveFriendRequests(player: Player): List<String> {
        return StorageSS.getPlayerReceiveFriendRequests(player.uuid).map { it.asOfflineData()?.name!! }
    }

    fun sendFriendRequest(player: Player, target: UUID) {
        StorageSS.addFriendRequest(player.uuid, target)
    }

    fun addFriend(pUUID: UUID, target: UUID): PlayerFriend {
        return StorageSS.addFriend(pUUID, target)
    }

    fun agressFriendRequest(player: Player, uuid: UUID) {
        if (!StorageSS.inFriendRequests(uuid, player.uuid)) {
            player.sendLang("friendRequestsNoExists")
            return
        }
        StorageSS.removeFriendRequest(uuid, player.uuid)
        addFriend(player.uuid, uuid)
        player.sendLang("agressFriendRequestToReceiver", "{sender}", uuid.asOfflineData()?.name ?: "")
        uuid.asPlayer()?.sendLang("agressFriendRequestToSender", "{receiver}", player.name)
    }

    fun refuseFriendRequest(player: Player, uuid: UUID) {
        if (!StorageSS.inFriendRequests(uuid, player.uuid)) {
            player.sendLang("friendRequestsNoExists")
            return
        }
        StorageSS.removeFriendRequest(uuid, player.uuid)
        player.sendLang("refuseFriendRequestToReceiver", "{sender}", uuid.asOfflineData()?.name ?: "")
        uuid.asPlayer()?.sendLang("refuseFriendRequestToSender", "{receiver}", player.name)
    }

    fun getPlayerSocial(uuid: UUID): PlayerSocial {
        return StorageSS.getPlayerSocial(uuid)
    }

    fun setAllowRepair(social: PlayerSocial, value: Boolean) {
        StorageSS.setAllowRepair(social, value)
    }

    fun deleteFriend(player: Player, offlineData: OfflinePlayerData) {
        removeFriend(player, offlineData.uuid)
        player.sendLang("deleteFriendToSender", "{friend}", offlineData.name)
        val target = offlineData.uuid.asPlayer()
        if (target == null) {
            CrossServerManager.sendData(SocialSystem.SOCIAL_LANG) {
                writeUUID(offlineData.uuid)
                writeString("deleteFriendToRecevier")
                writeString("{friend}")
                writeString(player.name)
            }
        } else {
            target.sendLang("deleteFriendToRecevier", "{friend}", player.name)
        }
    }

    fun handleCrossServerLang(buf: ByteBuf) {
        val player = buf.readUUID().asPlayer()
        val langKey = buf.readString()
        player?.sendLang(langKey, buf.readString(), buf.readString())
    }

}