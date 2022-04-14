package io.github.mainyf.myislands

import dev.lone.itemsadder.api.CustomBlock
import io.github.mainyf.myislands.config.ConfigManager
import io.github.mainyf.myislands.menu.IslandsChooseMenu
import io.github.mainyf.newmclib.command.cmdParser
import io.github.mainyf.newmclib.exts.errorMsg
import io.github.mainyf.newmclib.exts.msg
import io.github.mainyf.newmclib.exts.successMsg
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CommandHandler : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        cmdParser(sender, args) {
            val type = arg<String>() ?: return@cmdParser
            when (type) {
                "reload" -> {
                    val plugin = MyIslands.INSTANCE
                    plugin.saveDefaultConfig()
                    plugin.reloadConfig()
                    ConfigManager.load()
                    sender.successMsg("[MyIsLands] 重载成功")
                }
                "tt" -> {
                    val target = arg<Player>() ?: return@cmdParser
                    val coreBlock = CustomBlock.getInstance("itemsadder:lit_campfire")!!
                    coreBlock.place(target.location)
                }
                "coreLoc" -> {
                    val target = arg<Player>() ?: return@cmdParser
                    val plotPlayer = MyIslands.plotAPI.wrapPlayer(target.uniqueId)!!
                    val plot = plotPlayer.location.plotAbs
                    if (plot == null) {
                        target.errorMsg("你的脚下没有地皮")
                        return@cmdParser
                    }
                    val dLoc = plot.bottomAbs
                    target.msg("dLoc: ${dLoc.x} ${dLoc.y} ${dLoc.z}")
                    val loc = target.location
                    val x = dLoc.x - loc.blockX
                    val y = dLoc.y - loc.blockY
                    val z = dLoc.z - loc.blockZ
                    target.msg("相对坐标为: $x $y $z")
                }
                "menu" -> {
                    val target = arg<Player>() ?: return@cmdParser
                    IslandsChooseMenu().open(target)
                }
                "get" -> {
                    val target = arg<Player>() ?: return@cmdParser
                    val plotPlayer = MyIslands.plotAPI.wrapPlayer(target.uniqueId)!!
                    val plots = MyIslands.plotAPI.getPlayerPlots(plotPlayer)
                    if (plots.isNotEmpty()) {
                        target.errorMsg("你已经领取了一个地皮")
                        return@cmdParser
                    }
                    MyIslands.plotUtils.autoClaimPlot(plotPlayer) {
                        target.successMsg("领取成功")
                    }
                }
                "paste" -> {
                    val target = arg<Player>() ?: return@cmdParser
                    val schematiceName = arg<String>() ?: return@cmdParser

                    val wrapPlayer = MyIslands.plotAPI.wrapPlayer(target.uniqueId)!!
                    val plots = MyIslands.plotAPI.getPlayerPlots(wrapPlayer)
                    if (plots.isEmpty()) {
                        target.errorMsg("你需要领取一个地皮")
                        return@cmdParser
                    }
                    MyIslands.plotUtils.paste(target, plots.first(), schematiceName) {
                        if (it) {
                            target.successMsg("成功")
                        } else {
                            target.successMsg("错误")
                        }
                    }
                }
                "viewLoc" -> {
                    val target = arg<Player>() ?: return@cmdParser
                    val wrapPlayer = MyIslands.plotAPI.wrapPlayer(target.uniqueId)!!
                    val plots = MyIslands.plotAPI.getPlayerPlots(wrapPlayer)
                    plots.forEach { plot ->
                        sender.msg("id: ${plot.id}")
                        plot.getDefaultHome {
                            sender.msg("default: $it")
                        }
                        plot.getHome {
                            sender.msg("home: $it")
                        }
                    }
                }
            }
        }
        return false
    }

}