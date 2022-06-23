package io.github.mainyf.playersettings

import io.github.mainyf.newmclib.command.APICommand
import io.github.mainyf.newmclib.command.cmdParser
import io.github.mainyf.newmclib.exts.errorMsg
import io.github.mainyf.newmclib.exts.successMsg
import io.github.mainyf.playersettings.config.ConfigManager
import io.github.mainyf.playersettings.storage.StorageManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object CommandHandler : APICommand("pst") {

    fun init() {
        apply {
            "reload" {
                executeOP {
                    ConfigManager.load()
                    sender.successMsg("[PlayerSettings] 重载成功")
                }
            }
            "export" {
                executeOP {
                    if (!sender.isOp) return@executeOP
                    StorageManager.exportToFile(sender)
                }
            }
        }
    }

}