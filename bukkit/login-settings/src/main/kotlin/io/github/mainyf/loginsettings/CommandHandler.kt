package io.github.mainyf.loginsettings

import io.github.mainyf.loginsettings.config.ConfigManager
import io.github.mainyf.loginsettings.module.ResetPasswords
import io.github.mainyf.loginsettings.storage.StorageManager
import io.github.mainyf.newmclib.command.APICommand
import io.github.mainyf.newmclib.command.playerArguments
import io.github.mainyf.newmclib.exts.successMsg

object CommandHandler : APICommand("loginsettings") {

    init {
        withAliases("lset")
        "reload" {
            executeOP {
                ConfigManager.load()
                sender.successMsg("[LoginSettings] 重载成功")
            }
        }
        "agree" {
            withArguments(playerArguments("玩家名"))
            executeOP {
                val player = player()
                if (StorageManager.hasAgreePlayRuleInWeek(player)) {
                    ConfigManager.playRuleNoExpired?.execute(player)
                    return@executeOP
                }
                StorageManager.addPlayRuleAgreeLog(player)
                ConfigManager.playRuleSuccess?.execute(player)
            }
        }
        "resetpasswd" {
            withArguments(playerArguments("玩家名"))
            executeOP {
                val player = player()
                if(!ConfigManager.qqEnable) {
                    ConfigManager.emergencyAction?.execute(player)
                    return@executeOP
                }
                ResetPasswords.startResetPassword(player)
            }
        }
    }

}