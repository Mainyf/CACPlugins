package io.github.mainyf.socialsystem.module

import io.github.mainyf.bungeesettingsbukkit.CrossServerManager
import io.github.mainyf.bungeesettingsbukkit.readLoc
import io.github.mainyf.bungeesettingsbukkit.readLocPair
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.offline_player_ext.OfflinePlayerData
import io.github.mainyf.newmclib.offline_player_ext.asOfflineData
import io.github.mainyf.socialsystem.SocialSystem
import io.github.mainyf.socialsystem.config.ConfigManager
import io.github.mainyf.socialsystem.config.sendLang
import io.github.mainyf.socialsystem.menu.SocialIslandTPMenu
import io.github.mainyf.socialsystem.storage.PlayerFriend
import io.github.mainyf.socialsystem.storage.PlayerSocial
import io.github.mainyf.socialsystem.storage.StorageManager
import io.netty.buffer.ByteBuf
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.UUID

object FriendHandler {


    fun allowRepair(target: UUID): Boolean {
        return StorageManager.allowRepair(target)
    }

    fun getFriends(player: Player): List<OfflinePlayerData> {
        return StorageManager.getFriends(player.uuid).map { it to it.friend.asOfflineData() }
            .sortedWith { (a, offlineDataA), (b, offlineDataB) ->
                when {
                    a.intimacy > b.intimacy -> -1
                    a.intimacy < b.intimacy -> 1
                    a.intimacy == b.intimacy -> {
                        (offlineDataA?.name ?: "").compareTo(offlineDataB?.name ?: "")
                    }
                    else -> -1
                }
            }.mapNotNull { it.second }
    }

    fun removeFriend(player: Player, target: UUID) {
        StorageManager.removeFriend(player.uuid, target)
    }

    fun isFriend(player: Player, target: UUID): Boolean {
        return StorageManager.getPlayerFriendList(player.uuid).contains(target)
    }

    fun getSendFriendRequests(player: Player): List<String> {
        return StorageManager.getPlayerSendFriendRequests(player.uuid).map { it.asOfflineData()?.name!! }
    }

    fun getReceiveFriendRequests(player: Player): List<String> {
        return StorageManager.getPlayerReceiveFriendRequests(player.uuid).map { it.asOfflineData()?.name!! }
    }

    fun sendFriendRequest(player: Player, target: UUID) {
        StorageManager.addFriendRequest(player.uuid, target)
    }

    fun addFriend(pUUID: UUID, target: UUID): PlayerFriend {
        return StorageManager.addFriend(pUUID, target)
    }

    fun agressFriendRequest(player: Player, uuid: UUID) {
        if (!StorageManager.inFriendRequests(uuid, player.uuid)) {
            player.sendLang("friendRequestsNoExists")
            return
        }
        StorageManager.removeFriendRequest(uuid, player.uuid)
        addFriend(player.uuid, uuid)
        player.sendLang("agressFriendRequestToReceiver", "{sender}", uuid.asOfflineData()?.name ?: "")
        uuid.asPlayer()?.sendLang("agressFriendRequestToSender", "{receiver}", player.name)
    }

    fun refuseFriendRequest(player: Player, uuid: UUID) {
        if (!StorageManager.inFriendRequests(uuid, player.uuid)) {
            player.sendLang("friendRequestsNoExists")
            return
        }
        StorageManager.removeFriendRequest(uuid, player.uuid)
        player.sendLang("refuseFriendRequestToReceiver", "{sender}", uuid.asOfflineData()?.name ?: "")
        uuid.asPlayer()?.sendLang("refuseFriendRequestToSender", "{receiver}", player.name)
    }

    fun getPlayerSocial(uuid: UUID): PlayerSocial {
        return StorageManager.getPlayerSocial(uuid)
    }

    fun setAllowRepair(social: PlayerSocial, value: Boolean) {
        StorageManager.setAllowRepair(social, value)
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