package io.github.mainyf.commandsettings

import io.github.mainyf.commandsettings.config.ConfigManager
import io.github.mainyf.newmclib.exts.registerCommand
import org.bukkit.plugin.java.JavaPlugin

class CommandSettings : JavaPlugin() {

    companion object {
        lateinit var INSTANCE: CommandSettings
    }

    override fun onEnable() {
        INSTANCE = this
        ConfigManager.load()
        this.registerCommand("cset", CommandHandler())
    }
}