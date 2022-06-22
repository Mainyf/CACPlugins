package io.github.mainyf.loggercenters

import io.github.mainyf.newmclib.exts.registerCommand
import org.bukkit.plugin.java.JavaPlugin

class LoggerCenters : JavaPlugin() {

    override fun onEnable() {

        registerCommand("mlog", MoneyLoggerCommand)

    }

}