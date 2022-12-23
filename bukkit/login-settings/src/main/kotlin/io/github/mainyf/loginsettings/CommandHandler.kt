package io.github.mainyf.loginsettings

import io.github.mainyf.loginsettings.config.ConfigLS
import io.github.mainyf.loginsettings.module.ResetPasswords
import io.github.mainyf.loginsettings.storage.StorageLS
import io.github.mainyf.newmclib.command.APICommand
import io.github.mainyf.newmclib.command.playerArguments
import io.github.mainyf.newmclib.exts.successMsg

object CommandHandler : APICommand("loginsettings") {

    init {
        withAliases("lset")
        "reload" {
            executeOP {
                ConfigLS.load()
                sender.successMsg("[LoginSettings] 重载成功")
            }
        }
        "agree" {
            withArguments(playerArguments("玩家名"))
            executeOP {
                val player = player()
                if (StorageLS.hasAgreePlayRuleInWeek(player)) {
                    ConfigLS.playRuleNoExpired?.execute(player)
                    return@executeOP
                }
                StorageLS.addPlayRuleAgreeLog(player)
                ConfigLS.playRuleSuccess?.execute(player)
            }
        }
        "resetpasswd" {
            withArguments(playerArguments("玩家名"))
            executeOP {
                val player = player()
                if(!ConfigLS.qqEnable) {
                    ConfigLS.emergencyAction?.execute(player)
                    return@executeOP
                }
                ResetPasswords.startResetPassword(player)
            }
        }
    }

}