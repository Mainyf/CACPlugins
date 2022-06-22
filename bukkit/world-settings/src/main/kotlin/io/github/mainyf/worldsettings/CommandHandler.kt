package io.github.mainyf.worldsettings

import io.github.mainyf.newmclib.command.cmdParser
import io.github.mainyf.newmclib.config.action.PlayerCommandAction
import io.github.mainyf.newmclib.exts.msg
import io.github.mainyf.newmclib.exts.successMsg
import io.github.mainyf.worldsettings.config.ConfigManager
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

object CommandHandler : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (!sender.hasPermission("${WorldSettings.INSTANCE.name}.command")) return false
        cmdParser(sender, args) {
            val type = arg<String>() ?: return@cmdParser
            when (type) {
                "test" -> {
                    PlayerCommandAction("help", true).execute(sender)
                }
                "reload" -> {
                    val plugin = WorldSettings.INSTANCE
                    plugin.saveDefaultConfig()
                    plugin.reloadConfig()
                    ConfigManager.load(plugin.config)
                    sender.successMsg("[WorldSettings] 重载成功")
                }
                "applyGameRule" -> {
                    Bukkit.getWorlds().forEach {
                        val settings = ConfigManager.getSetting(it) ?: return@forEach
                        settings.gameRules.forEach { (rule, value) ->
                            val oldValue = it.getGameRuleValue(rule)
                            if (oldValue != value) {
                                it.setGameRule(rule, value)
                                sender.msg("世界: ${it.name} 应用: ${rule.name}: $value")
                            }
                        }
                    }
                    sender.successMsg("应用完成")
                }
//                "viewGameRule" -> {
//                    Bukkit.getWorlds().forEach { world ->
//                        GameRule.values().forEach {
//                            val value = world.getGameRuleValue(it)
//                            sender.msg("${it.name}: $value")
//                        }
//                    }
//                }
            }
        }
        return false
    }

}