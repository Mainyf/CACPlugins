package io.github.mainyf.worldsettings

import io.github.mainyf.newmclib.command.APICommand
import io.github.mainyf.newmclib.exts.msg
import io.github.mainyf.newmclib.exts.successMsg
import io.github.mainyf.worldsettings.config.ConfigWS
import org.bukkit.Bukkit

object CommandHandler : APICommand("worldsettings") {

    fun init() {
        withAliases("wsett", "wse", "ws")
        withPermission("${WorldSettings.INSTANCE.name}.command")
        apply {
            "reload" {
                executeOP {
                    ConfigWS.load()
                    sender.successMsg("[WorldSettings] 重载成功")
                }
            }
            "applyGameRule" {
                executeOP {
                    Bukkit.getWorlds().forEach {
                        val settings = ConfigWS.getSetting(it) ?: return@forEach
                        settings.gameRules.forEach { (rule, value) ->
                            val oldValue = it.getGameRuleValue(rule)
                            if (oldValue != value) {
                                it.setGameRule(rule, value)
                                sender.msg("世界: ${it.name} 应用: ${rule.name}: $value")
                            }
                        }
                    }
                    sender.successMsg("应用完成")
                }
            }
//            "dot" {
//                withArguments(playerArguments("玩家名"))
//                executeOP {
//                    val sPlayer = sender as Player
//                    val player = player()
//                    val spVec = sPlayer.location.apply { this.pitch = 0f }.direction
//                    val pVec = player.location.apply { this.pitch = 0f }.direction
//                    val dot = spVec.x * pVec.x + spVec.z * pVec.z
//                    sender.msg(dot.toString())
//                }
//            }
        }
    }

}