package io.github.mainyf.loggercenters

import io.github.mainyf.newmclib.BasePlugin
import io.github.mainyf.newmclib.exts.registerCommand

class LoggerCenters : BasePlugin() {

    override fun enable() {

        registerCommand("mlog", MoneyLoggerCommand)

    }

}