package io.github.mainyf.socialsystem.module

import io.github.mainyf.bungeesettingsbukkit.CrossServerManager
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.offline_player_ext.OfflinePlayerData
import io.github.mainyf.newmclib.offline_player_ext.asOfflineData
import io.github.mainyf.socialsystem.SocialSystem
import io.github.mainyf.socialsystem.config.ConfigManager
import io.github.mainyf.socialsystem.config.sendLang
import io.netty.buffer.ByteBuf
import org.bukkit.entity.Player
import java.util.*

object FriendInvites {
    
    private val invTpRequests = mutableMapOf<String, Pair<UUID, Long>>()

    fun sendInviteTP(player: Player, offlineData: OfflinePlayerData) {
        val target = offlineData.uuid.asPlayer()
        player.sendLang("inviteTPToSender")
        if (target != null) {
            target.sendLang("inviteTPToReceiver", "{player}", player.name)
            invTpRequests[player.name] = offlineData.uuid to currentTime()
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
            invTpRequests[sender] = player.uuid to currentTime()
            player.sendLang("inviteTPToReceiver", "{player}", sender)
        }
    }

    fun handleInviteTPAgree(player: Player, target: String) {
        if (!invTpRequests.containsKey(target)) {
            player.sendLang("inviteTPInvalid")
            return
        }
        val pair = invTpRequests.remove(target)!!
        if(currentTime() - pair.second >= ConfigManager.inviteTpExpired * 1000L) {
            player.sendLang("tpRequestExpired")
            return
        }
        val offlineData = target.asOfflineData()!!

        player.sendLang("inviteTPSuccessToReceiver", "{friend}", offlineData.name)
        val targetPlayer = target.asPlayer()
        if (targetPlayer != null) {
            player.teleport(targetPlayer.location)
            targetPlayer.sendLang("inviteTPSuccessToSender", "{friend}", player.name)
        } else {
            CrossServerManager.sendData(SocialSystem.SOCIAL_LANG) {
                writeUUID(offlineData.uuid)
                writeString("inviteTPSuccessToSender")
                writeString("{friend}")
                writeString(player.name)
            }
            SocialSystem.INSTANCE.submitTask(delay = 10L) {
                CrossServerManager.stp(player, offlineData.uuid)
            }
        }
    }

    fun handleInviteTPRefuse(player: Player, target: String) {
        if (!invTpRequests.containsKey(target)) return
        val offlineData = target.asOfflineData() ?: return
        val pair = invTpRequests.remove(target)!!
        if(currentTime() - pair.second >= ConfigManager.inviteTpExpired * 1000L) {
            player.sendLang("tpRequestExpired")
            return
        }

        player.sendLang("refuseTPInvToReceiver", "{friend}", target)

        val targetPlayer = target.asPlayer()
        if (targetPlayer != null) {
            targetPlayer.sendLang("refuseTPInvToSender", "{friend}", player.name)
        } else {
            CrossServerManager.sendData(SocialSystem.SOCIAL_LANG) {
                writeUUID(offlineData.uuid)
                writeString("refuseTPInvToSender")
                writeString("{friend}")
                writeString(player.name)
            }
        }
    }
    
}