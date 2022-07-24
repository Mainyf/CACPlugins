package io.github.mainyf.socialsystem.module

import io.github.mainyf.bungeesettingsbukkit.CrossServerManager
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.offline_player_ext.OfflinePlayerData
import io.github.mainyf.newmclib.offline_player_ext.asOfflineData
import io.github.mainyf.socialsystem.SocialSystem
import io.github.mainyf.socialsystem.config.ConfigManager
import io.github.mainyf.socialsystem.config.sendLang
import io.github.mainyf.socialsystem.storage.PlayerFriend
import io.github.mainyf.socialsystem.storage.PlayerSocial
import io.github.mainyf.socialsystem.storage.StorageManager
import io.netty.buffer.ByteBuf
import org.bukkit.entity.Player
import java.util.UUID

object FriendHandler {

    private val tpRequests = mutableMapOf<String, UUID>()
    private val invTpRequests = mutableMapOf<String, UUID>()

    fun allowRepair(player: Player, target: UUID): Boolean {
        return player.hasPermission(ConfigManager.repairPermission) && StorageManager.allowRepair(target)
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

//        when (val langKey = buf.readString()) {
//            "deleteFriendToRecevier" -> {
//                player?.sendLang(langKey, buf.readString(), buf.readString())
//            }
//            "refuseTPRequest" -> {
//                player?.sendLang(langKey, buf.readString(), buf.readString())
//            }
//        }
    }

    fun sendTPRequest(player: Player, offlineData: OfflinePlayerData) {
        val target = offlineData.uuid.asPlayer()
        player.sendLang("sendTPRequestToSender")
        if (target != null) {
            target.sendLang("sendTPRequestToReceiver", "{player}", player.name)
            tpRequests[player.name] = offlineData.uuid
        } else {
            CrossServerManager.sendData(SocialSystem.FRIEND_TP_REQUEST) {
                writeString(player.name)
                writeUUID(offlineData.uuid)
            }
        }
    }

    fun handleTPRequest(buf: ByteBuf) {
        val sender = buf.readString()
        val player = buf.readUUID().asPlayer()
        if (player != null) {
            tpRequests[sender] = player.uuid
            player.sendLang("sendTPRequestToReceiver", "{player}", sender)
        }
    }

    fun handleTPReqAgree(player: Player, target: String) {
        if (!tpRequests.containsKey(target)) {
            player.sendLang("tpRequestInvalid")
            return
        }
        player.sendLang("tpRequestSuccessToReceiver", "{friend}", target.asOfflineData()!!.name)
        val targetPlayer = target.asPlayer()
        if (targetPlayer != null) {
            targetPlayer.teleport(player.location)
            player.sendLang("tpRequestSuccessToSender", "{friend}", targetPlayer.name)
        } else {
            CrossServerManager.sendData(SocialSystem.FRIEND_TP_REQUEST_AGREE) {
                writeUUID(player.uuid)
                writeUUID(target.asOfflineData()!!.uuid)
            }
        }
    }

    fun handleTPReqAgreeSender(buf: ByteBuf) {
        val target = buf.readUUID()
        val player = buf.readUUID().asPlayer()
        if (player != null) {
            player.sendLang("tpRequestSuccessToSender", "{friend}", target.asOfflineData()!!.name)
            CrossServerManager.stp(player, target)
        }
    }

    fun handleTPReqRefuse(player: Player, target: String) {
        if (!tpRequests.containsKey(target)) return
        val offlineData = target.asOfflineData() ?: return
        tpRequests.remove(target)
        val targetPlayer = target.asPlayer()

        if (targetPlayer != null) {
            targetPlayer.sendLang("refuseTPRequest", "{friend}", player.name)
        } else {
            CrossServerManager.sendData(SocialSystem.SOCIAL_LANG) {
                writeUUID(offlineData.uuid)
                writeString("refuseTPRequest")
                writeString("{friend}")
                writeString(player.name)
            }
        }
    }

    fun sendInviteTP(player: Player, offlineData: OfflinePlayerData) {
        val target = offlineData.uuid.asPlayer()
        player.sendLang("inviteTPToSender")
        if (target != null) {
            target.sendLang("inviteTPToReceiver", "{player}", player.name)
            invTpRequests[player.name] = offlineData.uuid
        } else {
            CrossServerManager.sendData(SocialSystem.FRIEND_TP_INVITE) {
                writeString(player.name)
                writeUUID(offlineData.uuid)
            }
        }
    }

    fun handleInviteTP(buf: ByteBuf) {
        val sender = buf.readString()
        val pUUID = buf.readUUID()
        val player = pUUID.asPlayer()
        if (player != null) {
            invTpRequests[sender] = player.uuid
            player.sendLang("inviteTPToReceiver", "{player}", sender)
        }
    }

    fun handleInviteTPAgree(player: Player, target: String) {
        if (!invTpRequests.containsKey(target)) {
            player.sendLang("inviteTPInvalid")
            return
        }
        invTpRequests.remove(target)
        val offlineData = target.asOfflineData()!!

        player.sendLang("inviteTPSuccessToReceiver", "{friend}", offlineData.name)
        val targetPlayer = target.asPlayer()
        if (targetPlayer != null) {
            player.teleport(targetPlayer.location)
            targetPlayer.sendLang("inviteTPSuccessToSender", "{friend}", player.name)
        } else {
            CrossServerManager.stp(player, offlineData.uuid)
            CrossServerManager.sendData(SocialSystem.SOCIAL_LANG) {
                writeUUID(offlineData.uuid)
                writeString("inviteTPSuccessToSender")
                writeString("{friend}")
                writeString(player.name)
            }
        }
    }

//    fun handleInviteTPAgreeSender(buf: ByteBuf) {
//        val target = buf.readString()
//        val player = buf.readUUID().asPlayer()
//        if(player != null) {
//            CrossServerManager.stp(player, tpRequests.remove(target)!!)
//        }
//    }

    fun handleInviteTPRefuse(player: Player, target: String) {
        if (!invTpRequests.containsKey(target)) return
        val offlineData = target.asOfflineData() ?: return
        invTpRequests.remove(target)

        val targetPlayer = target.asPlayer()
        if (targetPlayer != null) {
            targetPlayer.sendLang("refuseTPInv", "{friend}", player.name)
        } else {
            CrossServerManager.sendData(SocialSystem.SOCIAL_LANG) {
                writeUUID(offlineData.uuid)
                writeString("refuseTPInv")
                writeString("{friend}")
                writeString(player.name)
            }
        }
    }

}