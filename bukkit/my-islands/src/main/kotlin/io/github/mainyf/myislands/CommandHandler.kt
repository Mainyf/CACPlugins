package io.github.mainyf.myislands

import io.github.mainyf.myislands.config.ConfigManager
import io.github.mainyf.myislands.features.MoveIslandCore
import io.github.mainyf.myislands.menu.IslandsChooseMenu
import io.github.mainyf.myislands.storage.StorageManager
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
                "moveCore" -> {
                    val target = arg<Player>() ?: return@cmdParser
                    val plotPlayer = MyIslands.plotAPI.wrapPlayer(target.uniqueId)!!
                    val plot = plotPlayer.location.plotAbs
                    if (MoveIslandCore.hasMoveingCore(target.uniqueId)) {
                        target.errorMsg("你正在移动你的核心")
                        return@cmdParser
                    }
                    if (plot == null) {
                        target.errorMsg("你的脚下没有地皮")
                        return@cmdParser
                    }
                    val owner = plot.owner
                    if (owner != target.uniqueId) {
                        target.errorMsg("你不是脚下地皮的主人")
                        return@cmdParser
                    }
                    val islandData = StorageManager.getPlayerIsland(target.uniqueId)
                    if (islandData == null) {
                        target.errorMsg("意外的错误: 0xMI1")
                        return@cmdParser
                    }
                    MoveIslandCore.startMoveCore(target, islandData, plot)
                    target.successMsg("开始移动核心水晶")
                }
                "endMove" -> {
                    val target = arg<Player>() ?: return@cmdParser
                    if (!MoveIslandCore.hasMoveingCore(target.uniqueId)) {
                        target.errorMsg("你不在移动核心的状态")
                        return@cmdParser
                    }
                    MoveIslandCore.endMoveCore(target)
                    target.successMsg("结束移动核心水晶")
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
//                    target.msg("dLoc: ${dLoc.x} ${dLoc.y} ${dLoc.z}")
                    val loc = target.location
                    val x = dLoc.x - loc.blockX
                    val y = dLoc.y - loc.blockY
                    val z = dLoc.z - loc.blockZ
                    target.msg("相对坐标为: $x $y $z")
                }
                "tryMenu" -> {
                    val target = arg<Player>() ?: return@cmdParser
                    val plotPlayer = MyIslands.plotAPI.wrapPlayer(target.uniqueId) ?: return@cmdParser
                    if (MyIslands.plotAPI.getPlayerPlots(plotPlayer).isNotEmpty()) {
                        return@cmdParser
                    }
                    IslandsChooseMenu().open(target)
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