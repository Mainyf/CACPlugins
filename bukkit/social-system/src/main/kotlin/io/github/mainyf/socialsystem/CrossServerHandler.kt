package io.github.mainyf.socialsystem

import io.github.mainyf.bungeesettingsbukkit.events.ServerPacketReceiveEvent
import io.github.mainyf.socialsystem.module.FriendHandler
import io.github.mainyf.socialsystem.module.FriendInvites
import io.github.mainyf.socialsystem.module.FriendIslandTPRequests
import io.github.mainyf.socialsystem.module.FriendTPRequests
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object CrossServerHandler : Listener {

//    private val receivePackets = mutableMapOf<ServerPacket, ByteBuf.() -> Unit>()

    @EventHandler
    fun onPacket(event: ServerPacketReceiveEvent) {
        val buf = event.buf
        when (event.packet) {
            SocialSystem.SOCIAL_LANG -> {
                FriendHandler.handleCrossServerLang(buf)
            }
            SocialSystem.FRIEND_TP_REQUEST -> {
                FriendTPRequests.handleTPRequest(buf)
            }
            SocialSystem.FRIEND_TP_INVITE -> {
                FriendInvites.handleInviteTP(buf)
            }
            SocialSystem.FRIEND_TP_REQUEST_AGREE -> {
                FriendTPRequests.handleTPReqAgreeSender(buf)
            }
            SocialSystem.ISLAND_TP_RES -> {
                FriendIslandTPRequests.handleIslandTPRes(buf)
            }
            SocialSystem.ISLAND_EMPTY_RES -> {
                FriendIslandTPRequests.handleIslandEmpty(buf)
            }
            SocialSystem.ISLAND_NOT_ALLOWED_ACCESS -> {
                FriendIslandTPRequests.handleIslandTPAccessNotAllowed(buf)
            }
        }
    }

}