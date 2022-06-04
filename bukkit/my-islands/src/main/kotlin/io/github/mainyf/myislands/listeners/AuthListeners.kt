package io.github.mainyf.myislands.listeners

import com.Zrips.CMI.events.CMIPlayerTeleportEvent
import fr.xephi.authme.events.LoginEvent
import io.github.mainyf.myislands.IslandsManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

object AuthListeners : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onLogin(event: LoginEvent) {
        IslandsManager.tryOpenPlayerIslandMenu(event.player)
    }

    @EventHandler
    fun onCMITP(event: CMIPlayerTeleportEvent) {
        IslandsManager.handleCMITP(event.player, event)
    }

}