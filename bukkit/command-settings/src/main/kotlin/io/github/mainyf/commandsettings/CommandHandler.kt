package io.github.mainyf.commandsettings

import dev.jorel.commandapi.arguments.GreedyStringArgument
import io.github.mainyf.bungeesettingsbukkit.CrossServerManager
import io.github.mainyf.commandsettings.config.ConfigManager
import io.github.mainyf.commandsettings.config.ItemAction
import io.github.mainyf.newmclib.command.APICommand
import io.github.mainyf.newmclib.command.customArguments
import io.github.mainyf.newmclib.command.playerArguments
import io.github.mainyf.newmclib.command.stringArguments
import io.github.mainyf.newmclib.config.ActionParser
import io.github.mainyf.newmclib.config.action.BaseAction
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.serverId
import org.bukkit.entity.Player

object CommandHandler : APICommand("cset") {

    private val cacheMap = mutableMapOf<String, BaseAction>()

    fun init() {
        apply {
            withPermission("${CommandSettings.INSTANCE.name}.command")
            "reload" {
                executeOP {
                    ConfigManager.load()
                    sender.successMsg("重载成功")
                }
            }
            "serverSend" {
                withArguments(
                    stringArguments("类型") { _ -> arrayOf("all", *CrossServerManager.serverIds.toTypedArray()) },
                    stringArguments("ID") { _ -> ConfigManager.getActionNames().toTypedArray() }
                )
                executeOP {
                    val type = text()
                    val id = text()
                    if (!ConfigManager.getActionNames().contains(id)) {
                        return@executeOP
                    }
                    if (type == serverId()) {
                        val action = ConfigManager.getAction(id) ?: return@executeOP
                        action.actions?.execute(console())
                    } else {
                        if(type == "all") {
                            val action = ConfigManager.getAction(id) ?: return@executeOP
                            action.actions?.execute(console())
                        }
                        CrossServerManager.sendData(CommandSettings.ACTION_ID) {
                            writeString(type)
                            writeString(id)
                        }
                    }
                }
            }
            "send" {
                withArguments(
                    customArguments(
                        "id",
                        "错误的Action ID: ",
                        { ConfigManager.getActionNames().toTypedArray() },
                        { ConfigManager.getAction(it) }),
                    playerArguments("玩家")
                )
                executeOP {
                    val action = args[0] as ItemAction
                    val player = args[1] as Player
                    CommandSettings.INSTANCE.trySendAction(player, action)
                }
            }
            "sendAll" {
                withArguments(
                    customArguments(
                        "id",
                        "错误的Action ID: ",
                        { ConfigManager.getActionNames().toTypedArray() },
                        { ConfigManager.getAction(it) })
                )
                executeOP {
                    val action = args[0] as ItemAction
                    onlinePlayers().forEach {
                        CommandSettings.INSTANCE.trySendAction(it, action)
                    }
                }
            }
            "sendMsg" {
                withArguments(
                    playerArguments("玩家名"),
                    GreedyStringArgument("msg")
                )
                executeOP {
                    val player = args[0] as Player
                    val msg = args[1] as String
                    val action = if (!cacheMap.containsKey(msg)) {
                        val parsedAction = ActionParser.parseAction(msg)
                        if (parsedAction == null) {
                            sender.msg("语法错误: $msg")
                            return@executeOP
                        }
                        cacheMap[msg] = parsedAction
                        parsedAction
                    } else cacheMap[msg]!!
                    action.execute(player)
                }
            }
            "sendMsgAll" {
                withArguments(
                    GreedyStringArgument("msg")
                )
                executeOP {
                    val msg = args[0] as String
                    val action = if (!cacheMap.containsKey(msg)) {
                        val parsedAction = ActionParser.parseAction(msg)
                        if (parsedAction == null) {
                            sender.msg("语法错误: $msg")
                            return@executeOP
                        }
                        cacheMap[msg] = parsedAction
                        parsedAction
                    } else cacheMap[msg]!!
                    onlinePlayers().forEach {
                        action.execute(it)
                    }
                }
            }
        }
    }


}