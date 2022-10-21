package io.github.mainyf.commandsettings

import io.github.mainyf.bungeesettingsbukkit.CrossServerManager
import io.github.mainyf.bungeesettingsbukkit.ServerPacket
import io.github.mainyf.bungeesettingsbukkit.events.ServerPacketReceiveEvent
import io.github.mainyf.commandsettings.config.ConfigManager
import io.github.mainyf.commandsettings.config.ItemAction
import io.github.mainyf.newmclib.BasePlugin
import io.github.mainyf.newmclib.config.ActionParser
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.serverId
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class CommandSettings : BasePlugin(), Listener {

    companion object {

        lateinit var INSTANCE: CommandSettings

        val ACTION_ID = ServerPacket.registerPacket("broadcast_action_id")

        val UNPARSE_ACTION_ID = ServerPacket.registerPacket("broadcast_unparse_action_id")

        val ACTION_ID_PLAYER = ServerPacket.registerPacket("broadcast_action_id_player")

    }

    override fun enable() {
        INSTANCE = this
        ConfigManager.load()
        CommandHandler.init()
        CommandHandler.register()
        pluginManager().registerEvents(this, this)
    }

    fun sendAction(serverId: String, id: String) {
        if (!ConfigManager.getActionNames().contains(id)) {
            return
        }
        if (serverId == serverId()) {
            val action = ConfigManager.getAction(id) ?: return
            action.actions?.execute(console())
        } else {
//            if (serverId == "all") {
//                val action = ConfigManager.getAction(id) ?: return
//                action.actions?.execute(console())
//            }
            CrossServerManager.sendData(ACTION_ID) {
                writeString(serverId)
                writeString(id)
            }
        }
    }

    fun sendAction(serverId: String, id: String, playerName: String) {
        if (!ConfigManager.getActionNames().contains(id)) {
            return
        }
        if (serverId == serverId()) {
            val action = ConfigManager.getAction(id) ?: return
            action.actions?.execute(console(), "{player}", playerName)
        } else {
//            if (serverId == "all") {
//                val action = ConfigManager.getAction(id) ?: return
//                action.actions?.execute(console(), "{player}", playerName)
//            }
            CrossServerManager.sendData(ACTION_ID_PLAYER) {
                writeString(serverId)
                writeString(id)
                writeString(playerName)
            }
        }
    }

    fun sendUnparseAction(serverId: String, msg: String) {
        if (serverId == serverId()) {
            val parsedAction = ActionParser.parseAction(msg)
            parsedAction.execute(console())
        } else {
//            if (serverId == "all") {
//                val parsedAction = ActionParser.parseAction(msg)
//                parsedAction.execute(console())
//            }
            CrossServerManager.sendData(UNPARSE_ACTION_ID) {
                writeString(serverId)
                writeString(msg)
            }
        }
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
            UNPARSE_ACTION_ID -> {
                val type = buf.readString()
                if (type != serverId() && type != "all") return
                val msg = buf.readString()
                val parsedAction = ActionParser.parseAction(msg)
                parsedAction.execute(console())
            }
            ACTION_ID_PLAYER -> {
                val type = buf.readString()
                if (type != serverId() && type != "all") return
                val ID = buf.readString()
                val action = ConfigManager.getAction(ID) ?: return
                action.actions?.execute(console(), "{player}", buf.readString())
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