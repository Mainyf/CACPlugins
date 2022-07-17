package io.github.mainyf.socialsystem

import io.github.mainyf.newmclib.command.APICommand
import io.github.mainyf.newmclib.command.playerArguments
import io.github.mainyf.newmclib.command.stringArguments
import io.github.mainyf.newmclib.exts.successMsg
import io.github.mainyf.newmclib.exts.uuid
import io.github.mainyf.newmclib.offline_player_ext.asOfflineData
import io.github.mainyf.socialsystem.config.ConfigManager
import io.github.mainyf.socialsystem.config.sendLang
import io.github.mainyf.socialsystem.menu.SocialCardMenu
import org.bukkit.entity.Player
import java.util.UUID

object CommandHandler : APICommand("social") {

    val friendRequest = mutableMapOf<UUID, MutableList<String>>()

    init {
        withAliases("ss", "ssm", "friend")
        "reload" {
            executeOP {
                ConfigManager.load()
                sender.successMsg("[SocialSystem] 重载完成")
            }
        }
        "add" {
            withArguments(playerArguments("玩家名"))
            executePlayer {
                val target = args[0] as Player
                friendRequest.getOrPut(target.uuid) { mutableListOf() }.add(sender.name)
                sender.sendLang("sendFriendRequestToSender")
                target.sendLang("sendFriendRequestToAudience", mapOf("{player}" to sender.name))
            }
        }
        "card" {
            executePlayer {
                val target = sender
                SocialCardMenu(target.uuid.asOfflineData()!!).open(target)
            }
        }
        "agree" {
            withArguments(stringArguments("玩家名") { info ->
                friendRequest[(info.sender as? Player)?.uuid ?: return@stringArguments emptyArray()]?.toTypedArray()
                    ?: emptyArray()
            })
            executePlayer {
                val target = args[0] as String

            }
        }
        "refuse" {
            withArguments(stringArguments("玩家名") { info ->
                friendRequest[(info.sender as Player).uuid]?.toTypedArray()
                    ?: emptyArray()
            })
            executePlayer {
                val target = args[0] as String

            }
        }
    }

}