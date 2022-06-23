package io.github.mainyf.commandsettings

import dev.jorel.commandapi.arguments.GreedyStringArgument
import io.github.mainyf.commandsettings.config.ConfigManager
import io.github.mainyf.commandsettings.config.ItemAction
import io.github.mainyf.newmclib.command.APICommand
import io.github.mainyf.newmclib.command.customArguments
import io.github.mainyf.newmclib.command.playerArguments
import io.github.mainyf.newmclib.config.ActionParser
import io.github.mainyf.newmclib.config.action.BaseAction
import io.github.mainyf.newmclib.exts.*
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
                    trySendAction(player, action)
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
                        trySendAction(it, action)
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

//    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<String>): Boolean {
//        if (!sender.hasPermission("${CommandSettings.INSTANCE.name}.command")) return false
//        cmdParser(sender, args) cmd@{
//            val type = arg<String>() ?: return@cmd
//            when (type) {
//                "reload" -> {
//                    ConfigManager.load()
//                    sender.successMsg("重载成功")
//                }
//                "send" -> {
//                    val id = arg<String>() ?: return@cmd
//                    val action = ConfigManager.getAction(id) ?: return@cmd
//                    val player = arg<Player>() ?: return@cmd
//                    trySendAction(player, action)
//                }
//                "sendAll" -> {
//                    val id = arg<String>() ?: return@cmd
//                    val action = ConfigManager.getAction(id) ?: return@cmd
//                    Bukkit.getOnlinePlayers().forEach {
//                        trySendAction(it, action)
//                    }
//                }
//            }
//        }
//        return true
//    }

    private fun trySendAction(player: Player, action: ItemAction) {
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