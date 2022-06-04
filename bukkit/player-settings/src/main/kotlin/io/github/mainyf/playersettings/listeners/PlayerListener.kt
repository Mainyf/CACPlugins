package io.github.mainyf.playersettings.listeners

import io.github.mainyf.newmclib.exts.currentTime
import io.github.mainyf.newmclib.exts.runTaskBR
import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.playersettings.PlayerSettings
import io.github.mainyf.playersettings.storage.StorageManager
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.TextComponent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import kotlin.math.ceil

object PlayerListener : Listener {

    private val playerLoginMap = mutableMapOf<UUID, Long>()

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        playerLoginMap[event.player.uuid] = currentTime()
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val pu = event.player.uuid
        if (playerLoginMap.containsKey(pu)) {
            val prev = playerLoginMap.remove(pu)!!
            val minutes = ceil((currentTime() - prev).toDouble() / 1000.0 / 60.0).toLong()
            StorageManager.updatePlayerOnlineTime(event.player, minutes)
        }
    }

    @EventHandler
    fun onPlayerCommand(event: PlayerCommandPreprocessEvent) {
        StorageManager.addPlayerCommandLog(event.player, event.message)
//        StorageManager.onCommandExecute(event.message)
    }

    @EventHandler
    fun onPlayerChat(event: AsyncChatEvent) {
        val player = event.player
        val tC = event.message() as TextComponent
        PlayerSettings.INSTANCE.runTaskBR {
            StorageManager.addPlayerMessageLog(player, tC.content())
        }
    }

//    @EventHandler
//    fun onUnknownCmd(event: UnknownCommandEvent) {
//        StorageManager.onCommandExecute(event.commandLine)
//    }

}