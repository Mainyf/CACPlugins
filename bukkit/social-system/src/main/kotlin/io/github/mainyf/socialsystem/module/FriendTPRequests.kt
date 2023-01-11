package io.github.mainyf.socialsystem.module

import io.github.mainyf.bungeesettingsbukkit.CrossServerManager
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.offline_player_ext.OfflinePlayerData
import io.github.mainyf.newmclib.offline_player_ext.asOfflineData
import io.github.mainyf.socialsystem.SocialSystem
import io.github.mainyf.socialsystem.config.ConfigSS
import io.github.mainyf.socialsystem.config.sendLang
import io.netty.buffer.ByteBuf
import org.bukkit.entity.Player
import java.util.*

object FriendTPRequests {

    private val tpRequests = mutableMapOf<String, Pair<UUID, Long>>()

    fun sendTPRequest(player: Player, offlineData: OfflinePlayerData) {
        val target = offlineData.uuid.asPlayer()
        player.sendLang("sendTPRequestToSender")
        if (target != null) {
            target.sendLang("sendTPRequestToReceiver", "{player}", player.name)
            tpRequests[player.name] = offlineData.uuid to currentTime()
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
            tpRequests[sender] = player.uuid to currentTime()
            player.sendLang("sendTPRequestToReceiver", "{player}", sender)
        }
    }

    fun handleTPReqAgree(player: Player, target: String) {
        if (!tpRequests.containsKey(target)) {
            player.sendLang("tpRequestInvalid")
            return
        }
        val pair = tpRequests.remove(target)!!
        if(currentTime() - pair.second >= ConfigSS.tpReqExpired * 1000L) {
            player.sendLang("tpRequestExpired")
            return
        }
        player.sendLang("tpRequestSuccessToReceiver", "{friend}", target.asOfflineData()!!.name)
        val targetPlayer = target.asPlayer()
        if (targetPlayer != null) {
            targetPlayer.teleport(player.location)
            targetPlayer.sendLang("tpRequestSuccessToSender", "{friend}", player.name)
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
            SocialSystem.INSTANCE.submitTask(delay = 10L) {
                CrossServerManager.stp(player, target)
            }
        }
    }

    fun handleTPReqRefuse(player: Player, target: String) {
        if (!tpRequests.containsKey(target)) return
        val offlineData = target.asOfflineData() ?: return
        val pair = tpRequests.remove(target)!!
        if(currentTime() - pair.second >= ConfigSS.tpReqExpired * 1000L) {
            player.sendLang("tpRequestExpired")
            return
        }
        val targetPlayer = target.asPlayer()
        player.sendLang("refuseTPRequestToReceiver", "{friend}", target)

        if (targetPlayer != null) {
            targetPlayer.sendLang("refuseTPRequestToSender", "{friend}", player.name)
        } else {
            CrossServerManager.sendData(SocialSystem.SOCIAL_LANG) {
                writeUUID(offlineData.uuid)
                writeString("refuseTPRequestToSender")
                writeString("{friend}")
                writeString(player.name)
            }
        }
    }
    
}