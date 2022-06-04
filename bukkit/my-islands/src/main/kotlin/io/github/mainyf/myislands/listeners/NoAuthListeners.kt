package io.github.mainyf.myislands.listeners

import com.Zrips.CMI.events.CMIPlayerTeleportEvent
import io.github.mainyf.myislands.IslandsManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

object NoAuthListeners : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onJoin(event: PlayerJoinEvent) {
        IslandsManager.tryOpenPlayerIslandMenu(event.player)
    }

    @EventHandler
    fun onCMITP(event: CMIPlayerTeleportEvent) {
        IslandsManager.handleCMITP(event.player, event)
    }

}