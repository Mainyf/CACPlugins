package io.github.mainyf.soulbind

import io.github.mainyf.newmclib.command.apiCommand
import io.github.mainyf.newmclib.command.playerArguments
import io.github.mainyf.newmclib.exts.msg
import io.github.mainyf.newmclib.exts.pluginManager
import io.github.mainyf.newmclib.exts.successMsg
import io.github.mainyf.soulbind.config.ConfigSB
import io.github.mainyf.soulbind.listeners.PlayerListeners
import org.apache.logging.log4j.LogManager
import org.bukkit.plugin.java.JavaPlugin

class SoulBind : JavaPlugin() {

    companion object {

        val LOGGER = LogManager.getLogger("SoulBind")

        lateinit var INSTANCE: SoulBind

    }

    override fun onEnable() {
        INSTANCE = this
        ConfigSB.load()
        pluginManager().registerEvents(PlayerListeners, this)
        apiCommand("soulbind") {
            withAliases("soulb", "sb")
            "reload" {
                executeOP {
                    ConfigSB.load()
                    sender.successMsg("[SoulBind] 重载成功")
                }
            }
            "bind" {
                withArguments(
                    playerArguments("玩家")
                )
                executeOP {
                    val player = player()
                    val itemStack = player.inventory.itemInMainHand
                    SBManager.bindItem(itemStack, player)
                    sender.msg("绑定成功")
                }
            }
        }
    }
}