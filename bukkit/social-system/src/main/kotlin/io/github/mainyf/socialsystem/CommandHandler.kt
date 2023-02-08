package io.github.mainyf.socialsystem

import dev.jorel.commandapi.arguments.LongArgument
import io.github.mainyf.newmclib.command.APICommand
import io.github.mainyf.newmclib.command.offlinePlayerArguments
import io.github.mainyf.newmclib.command.playerArguments
import io.github.mainyf.newmclib.command.stringArguments
import io.github.mainyf.newmclib.exts.msg
import io.github.mainyf.newmclib.exts.successMsg
import io.github.mainyf.newmclib.offline_player_ext.asOfflineData
import io.github.mainyf.socialsystem.config.ConfigSS
import io.github.mainyf.socialsystem.menu.SocialCardMenu
import io.github.mainyf.socialsystem.menu.SocialMainMenu
import io.github.mainyf.socialsystem.module.*
import org.bukkit.entity.Player

object CommandHandler : APICommand("social") {

    init {
        withAliases("ss", "ssm", "friend")
        "reload" {
            executeOP {
                NicknameConversation.endTasks()
                ConfigSS.load()
                NicknameConversation.initTasks()
                sender.successMsg("[SocialSystem] 重载完成")
            }
        }
        "card" {
            executePlayer {
                val target = sender
                SocialCardMenu(target.asOfflineData()).open(target)
            }
        }
        "player-qqnum" {
            withArguments(offlinePlayerArguments("玩家名"))
            executeOP {
                val player = offlinePlayer()
                val qqNum = SocialManager.getPlayerQQNum(player.uuid)
                sender.msg("玩家 ${player.name}: $qqNum")
            }
        }
        "qqnum-player" {
            withArguments(LongArgument("qq号"))
            executeOP {
                val qqnum = value<Long>()
                val player = SocialManager.getQQNumPlayer(qqnum)?.asOfflineData()
                if(player == null) {
                    sender.msg("没有查询到绑定了该账号的玩家")
                } else {
                    sender.msg("玩家 ${player.name}: $qqnum")
                }
            }
        }
        "menu" {
            executePlayer {
                val target = sender
                SocialMainMenu(target.asOfflineData()).open(target)
            }
        }
        "menu-player" {
            withArguments(playerArguments("玩家名"))
            executeOP {
                val target = player()
                SocialMainMenu(target.asOfflineData()).open(target)
            }
        }
        "agree" {
            withArguments(stringArguments("玩家名") { info ->
                FriendHandler.getReceiveFriendRequests((info.sender as? Player) ?: return@stringArguments emptyArray())
                    .toTypedArray()
            })
            executePlayer {
                val target = text().asOfflineData()!!
                FriendHandler.agressFriendRequest(sender, target.uuid)
            }
        }
        "refuse" {
            withArguments(stringArguments("玩家名") { info ->
                FriendHandler.getReceiveFriendRequests((info.sender as? Player) ?: return@stringArguments emptyArray())
                    .toTypedArray()
            })
            executePlayer {
                val target = text().asOfflineData()!!
                FriendHandler.refuseFriendRequest(sender, target.uuid)
            }
        }
        "agreeTPReq" {
            withArguments(
                stringArguments("玩家名")
            )
            executePlayer {
                val target = text()
                FriendTPRequests.handleTPReqAgree(sender, target)
            }
        }
        "refuseTPReq" {
            withArguments(
                stringArguments("玩家名")
            )
            executePlayer {
                val target = text()
                FriendTPRequests.handleTPReqRefuse(sender, target)
            }
        }
        "agreeTPReqInv" {
            withArguments(
                stringArguments("玩家名")
            )
            executePlayer {
                val target = text()
                FriendInvites.handleInviteTPAgree(sender, target)

            }
        }
        "refuseTPReqInv" {
            withArguments(
                stringArguments("玩家名")
            )
            executePlayer {
                val target = text()
                FriendInvites.handleInviteTPRefuse(sender, target)
            }
        }
        "nickname" {
            withArguments(
                playerArguments("玩家名")
            )
            executePlayer {
                val player = player()
                NicknameConversation.join(player)
            }
        }
    }

}