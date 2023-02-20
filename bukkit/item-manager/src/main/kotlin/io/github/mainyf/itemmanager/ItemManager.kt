package io.github.mainyf.itemmanager

import io.github.mainyf.newmclib.command.apiCommand
import io.github.mainyf.newmclib.exts.msg
import io.github.mainyf.newmclib.exts.pluginManager
import io.github.mainyf.newmclib.exts.successMsg
import org.apache.logging.log4j.LogManager
import org.bukkit.plugin.java.JavaPlugin

class ItemManager : JavaPlugin() {

    companion object {

        val LOGGER = LogManager.getLogger("ItemManager")

        lateinit var INSTANCE: ItemManager

    }

    override fun onEnable() {
        INSTANCE = this
        ConfigIM.load()
        IaItemAutoUpdate.init()
        pluginManager().registerEvents(IaItemAutoUpdate, this)
        apiCommand("item-manager") {
            withAliases("itemm", "im")
            "reload" {
                executeOP {
                    ConfigIM.load()
                    sender.successMsg("[ItemManager] 配置重载成功")
                }
            }
        }
    }

}