package io.github.mainyf.loginsettings

import io.github.mainyf.loginsettings.config.ConfigManager
import io.github.mainyf.newmclib.command.APICommand
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
    }

}