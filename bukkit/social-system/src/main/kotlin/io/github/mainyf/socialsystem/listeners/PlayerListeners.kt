package io.github.mainyf.socialsystem.listeners

import io.github.mainyf.bungeesettingsbukkit.events.ServerPacketReceiveEvent
import io.github.mainyf.newmclib.exts.asPlayer
import io.github.mainyf.newmclib.exts.readString
import io.github.mainyf.newmclib.exts.readUUID
import io.github.mainyf.socialsystem.SocialSystem
import io.github.mainyf.socialsystem.config.sendLang
import io.github.mainyf.socialsystem.menu.SocialCardMenu
import io.github.mainyf.socialsystem.module.FriendHandler
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent

object PlayerListeners : Listener {

    @EventHandler
    fun onInteractEntity(event: PlayerInteractEntityEvent) {
        val entity = event.rightClicked
        if (event.player.isSneaking && entity is Player) {
            SocialCardMenu(entity).open(event.player)
        }
    }

    @EventHandler
    fun onPacket(event: ServerPacketReceiveEvent) {
        val buf = event.buf
        when (event.packet) {
            SocialSystem.SOCIAL_LANG -> {
                FriendHandler.handleCrossServerLang(buf)
            }
            SocialSystem.FRIEND_TP_REQUEST -> {
                FriendHandler.handleTPRequest(buf)
            }
            SocialSystem.FRIEND_TP_INVITE -> {
                FriendHandler.handleInviteTP(buf)
            }
            SocialSystem.FRIEND_TP_REQUEST_AGREE -> {
                FriendHandler.handleTPReqAgreeSender(buf)
            }
        }
    }

}