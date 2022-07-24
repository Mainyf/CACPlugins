package io.github.mainyf.bungeesettingsbukkit

import dev.jorel.commandapi.arguments.DoubleArgument
import dev.jorel.commandapi.arguments.GreedyStringArgument
import io.github.mainyf.bungeesettingsbukkit.network.ClientSocketManager
import io.github.mainyf.newmclib.command.apiCommand
import io.github.mainyf.newmclib.command.playerArguments
import io.github.mainyf.newmclib.command.stringArguments
import io.github.mainyf.newmclib.exts.successMsg
import io.github.mainyf.newmclib.exts.toComp
import io.github.mainyf.newmclib.exts.writeString
import io.github.mainyf.newmclib.offline_player_ext.asOfflineData
import io.github.mainyf.newmclib.serverId
import org.bukkit.Location

object CommandHandler {

    fun init() {
        apiCommand("bcd") {
            withAliases("bc")
            onlyOP()
            "stp-pos" {
                withArguments(
                    playerArguments("玩家名"),
                    stringArguments("服务器名") { _ -> CrossServerManager.serverIds.toTypedArray() },
                    stringArguments("世界", "请输入世界名"),
                    DoubleArgument("x"),
                    DoubleArgument("y"),
                    DoubleArgument("z"),
                    stringArguments("yaw"),
                    stringArguments("pitch")
                )
                executeOP {
                    val player = player()
                    val serverName = text()
                    val world = text()
                    val x = double()
                    val y = double()
                    val z = double()
                    val yaw = text().toFloatOrNull() ?: player.location.yaw
                    val pitch = text().toFloatOrNull() ?: player.location.pitch
                    CrossServerManager.stp(player, serverName, world, Location(null, x, y, z, yaw, pitch))
                }
            }
            "stp-player" {
                withArguments(
                    playerArguments("玩家名"),
                    stringArguments("目标玩家名") { info -> CrossServerManager.playerMap.map { it.value }.toTypedArray() }
                )
                executeOP {
                    val player = player()
                    val target = text().asOfflineData()!!.uuid

                    CrossServerManager.stp(player, target)
                }
            }
            "cmd" {
                withArguments(
                    stringArguments("服务器ID") { _ -> CrossServerManager.serverIds.toTypedArray() },
                    GreedyStringArgument("命令")
                )
                executeOP {
                    val serverId = text()
                    val cmd = text()
                    CrossServerManager.executeCommandToServer(serverId, cmd)
                }
            }
            "cmdAll" {
                withArguments(
                    GreedyStringArgument("命令")
                )
                executeOP {
                    val cmd = text()
                    CrossServerManager.executeCommandToAll(cmd)
                }
            }
            "chat" {
                withArguments(
                    stringArguments("玩家"),
                    stringArguments("消息")
                )
                executeOP {
                    val offlinePlayer = text().asOfflineData()!!
                    val msg = text()
                    CrossServerManager.sendChat(offlinePlayer.uuid, msg.toComp())
                }
            }
            "reload" {
                executeOP {
                    BungeeSettingsBukkit.INSTANCE.loadConfig()
                    sender.successMsg("[BungeeSettingsBukkit] 重载成功")
                }
            }
        }.register()
    }

}