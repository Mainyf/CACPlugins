package io.github.mainyf.miningcheck

import io.github.mainyf.miningcheck.config.ConfigMC
import io.github.mainyf.miningcheck.storage.StorageMC
import io.github.mainyf.newmclib.command.apiCommand
import io.github.mainyf.newmclib.exts.pluginManager
import io.github.mainyf.newmclib.exts.successMsg
import org.apache.logging.log4j.LogManager
import org.bukkit.plugin.java.JavaPlugin

class MiningCheck : JavaPlugin() {

    companion object {

        val LOGGER = LogManager.getLogger("MiningCheck")

        lateinit var INSTANCE: MiningCheck

    }

    override fun onEnable() {
        INSTANCE = this
        ConfigMC.load()
        StorageMC.init()
        pluginManager().registerEvents(PlayerListeners, this)
        apiCommand("miningcheck") {
            withAliases("minc", "mc")
            "reload" {
                executeOP {
                    ConfigMC.load()
                    sender.successMsg("[MiningCheck] 重载成功")
                }
            }
            "reset" {
                executeOP {
                    StorageMC.reset()
                    PlayerListeners.reset()
                    sender.successMsg("[MiningCheck] 清空完成")
                }
            }
        }
    }

}