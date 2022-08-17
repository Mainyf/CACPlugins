package io.github.mainyf.playeraccount

import io.github.mainyf.newmclib.BasePlugin
import io.github.mainyf.newmclib.utils.ExtractPluginLib
import io.github.mainyf.playeraccount.config.ConfigManager
import io.github.mainyf.playeraccount.storage.StorageManager
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class PlayerAccount : BasePlugin() {

    companion object {

        val LOGGER: Logger = LogManager.getLogger("PlayerAccount")
        lateinit var INSTANCE: PlayerAccount

    }

    override fun enable() {
        INSTANCE = this

        ExtractPluginLib.extractLibs(this)
        ConfigManager.load()
        StorageManager.init()
        SMSManager.init()
        CommandHandler.init()
        CommandHandler.register()
    }

}