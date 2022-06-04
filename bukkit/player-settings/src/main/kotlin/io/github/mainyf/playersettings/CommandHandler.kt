package io.github.mainyf.playersettings

import io.github.mainyf.newmclib.command.cmdParser
import io.github.mainyf.newmclib.exts.errorMsg
import io.github.mainyf.newmclib.exts.successMsg
import io.github.mainyf.playersettings.config.ConfigManager
import io.github.mainyf.playersettings.storage.StorageManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CommandHandler : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        cmdParser(sender, args) {
            val type = arg<String>() ?: return@cmdParser
            when (type) {
                "reload" -> {
                    if (!sender.isOp) return@cmdParser
                    ConfigManager.load()
                    sender.successMsg("[PlayerSettings] 重载成功")
                }
                "export" -> {
                    if (!sender.isOp) return@cmdParser
                    StorageManager.exportToFile(sender)
                }
            }
        }
        return false
    }

}