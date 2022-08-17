package io.github.mainyf.socialsystem.listeners

import io.github.mainyf.newmclib.offline_player_ext.asOfflineData
import io.github.mainyf.socialsystem.menu.SocialCardMenu
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent

object PlayerListeners : Listener {

    @EventHandler
    fun onInteractEntity(event: PlayerInteractEntityEvent) {
        val entity = event.rightClicked
        if (event.player.isSneaking && entity is Player) {
            SocialCardMenu(entity.asOfflineData()).open(event.player)
        }
    }

}