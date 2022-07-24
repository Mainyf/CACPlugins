package io.github.mainyf.commandsettings

import io.github.mainyf.bungeesettingsbukkit.ServerPacket
import io.github.mainyf.bungeesettingsbukkit.events.ServerPacketReceiveEvent
import io.github.mainyf.commandsettings.config.ConfigManager
import io.github.mainyf.commandsettings.config.ItemAction
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.serverId
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

class CommandSettings : JavaPlugin(), Listener {

    companion object {

        lateinit var INSTANCE: CommandSettings

        val ACTION_ID = ServerPacket.registerPacket("broadcast_action_id")

    }

    override fun onEnable() {
        INSTANCE = this
        ConfigManager.load()
        CommandHandler.init()
        CommandHandler.register()
        pluginManager().registerEvents(this, this)
    }

    @EventHandler
    fun onPacket(event: ServerPacketReceiveEvent) {
        val buf = event.buf
        when (event.packet) {
            ACTION_ID -> {
                val type = buf.readString()
                if (type != serverId() && type != "all") return
                val ID = buf.readString()
                val action = ConfigManager.getAction(ID) ?: return
                action.actions?.execute(console())
//                action.plays?.execute(player.location)
            }
        }
    }

    fun trySendAction(player: Player, action: ItemAction) {
        var flag = true

        for ((type, amount) in action.demandItems) {
            val itemCount = player.countByItem { it?.equalsByIaNamespaceID(type) ?: false }
            if (itemCount < amount) {
                flag = false
                break
            }
        }
        if (!flag) {
            action.noDemandActions?.execute(player)
            return
        }
        for ((type, amount) in action.demandItems) {
            player.takeItem(amount) { it?.equalsByIaNamespaceID(type) ?: false }
        }
        action.actions?.execute(player)
        action.plays?.execute(player.location)
    }

}