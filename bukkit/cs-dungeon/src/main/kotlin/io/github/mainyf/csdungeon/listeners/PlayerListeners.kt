package io.github.mainyf.csdungeon.listeners

import io.github.mainyf.csdungeon.CsDungeon
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerQuitEvent

object PlayerListeners : Listener {

    @EventHandler
    fun onDead(event: PlayerDeathEvent) {
        CsDungeon.INSTANCE.dungeonBattles.forEach {
            it.removePlayer(event.entity)
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        CsDungeon.INSTANCE.dungeonBattles.forEach {
            it.removePlayer(event.player)
        }
    }

}