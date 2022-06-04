package io.github.mainyf.commandsettings

import io.github.mainyf.commandsettings.config.ConfigManager
import io.github.mainyf.commandsettings.config.ItemAction
import io.github.mainyf.newmclib.command.cmdParser
import io.github.mainyf.newmclib.exts.*
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CommandHandler : CommandExecutor {

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<String>): Boolean {
        if (!sender.hasPermission("${CommandSettings.INSTANCE.name}.command")) return false
        cmdParser(sender, args) cmd@{
            val type = arg<String>() ?: return@cmd
            when (type) {
                "reload" -> {
                    ConfigManager.load()
                    sender.successMsg("重载成功")
                }
                "send" -> {
                    val id = arg<String>() ?: return@cmd
                    val action = ConfigManager.getAction(id) ?: return@cmd
                    val player = arg<Player>() ?: return@cmd
                    trySendAction(player, action)
                }
                "sendAll" -> {
                    val id = arg<String>() ?: return@cmd
                    val action = ConfigManager.getAction(id) ?: return@cmd
                    Bukkit.getOnlinePlayers().forEach {
                        trySendAction(it, action)
                    }
                }
            }
        }
        return true
    }

    private fun trySendAction(player: Player, action: ItemAction) {
        var flag = true
        for ((type, amount) in action.demandItems) {
            val itemCount = player.countByItem { it.equalsByIaNamespaceID(type) }
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
            player.takeItem(amount) { it.equalsByIaNamespaceID(type) }
        }
        action.actions?.execute(player)
        action.plays?.execute(player.location)
    }

}