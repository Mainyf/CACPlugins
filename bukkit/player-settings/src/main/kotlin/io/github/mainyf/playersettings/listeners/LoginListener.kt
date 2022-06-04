package io.github.mainyf.playersettings.listeners

import fr.xephi.authme.events.LoginEvent
import fr.xephi.authme.events.RegisterEvent
import io.github.mainyf.playersettings.storage.StorageManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object LoginListener : Listener {

    @EventHandler
    fun onLogin(event: LoginEvent) {
        StorageManager.handlePlayerLogin(event.player)
    }

    @EventHandler
    fun onRegister(event: RegisterEvent) {
        StorageManager.handlePlayerRegister(event.player)
    }

}