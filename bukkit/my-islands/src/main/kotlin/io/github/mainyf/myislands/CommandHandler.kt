package io.github.mainyf.myislands

import dev.jorel.commandapi.arguments.DoubleArgument
import io.github.mainyf.myislands.config.ConfigManager
import io.github.mainyf.myislands.features.MoveIslandCore
import io.github.mainyf.myislands.menu.IslandsChooseMenu
import io.github.mainyf.myislands.menu.IslandsMainMenu
import io.github.mainyf.newmclib.command.APICommand
import io.github.mainyf.newmclib.command.playerArguments
import io.github.mainyf.newmclib.exts.errorMsg
import io.github.mainyf.newmclib.exts.msg
import io.github.mainyf.newmclib.exts.successMsg
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player

object CommandHandler : APICommand("myislands") {

    fun init() {
        withPermission("${MyIslands.INSTANCE.name}.command")
        apply {
            "reload" {
                executeOP {
                    ConfigManager.load()
                    sender.successMsg("[MyIsLands] 重载成功")
                }
            }
            "range" {
                withArguments(DoubleArgument("范围"))
                executeOP {
                    val player = sender as Player
                    val range = args[0] as Double
                    val entitys = player.getNearbyEntities(range, range, range)
                    entitys.forEach {
                        player.msg(it.toString())
                    }
                }
            }
            "rangeKill" {
                withArguments(DoubleArgument("范围"))
                executeOP {
                    val player = sender as Player
                    val range = args[0] as Double
                    val entitys = player.getNearbyEntities(range, range, range)
                    entitys.forEach {
                        if (it is ArmorStand) {
                            it.remove()
                        }
                    }
                }
            }
            "moveCore" {
                withArguments(playerArguments("玩家名"))
                executeOP {
                    val target = args[0] as Player
                    MoveIslandCore.tryStartMoveCore(target)
                }
            }
            "endMove" {
                withArguments(playerArguments("玩家名"))
                executeOP {
                    val target = args[0] as Player
                    if (!MoveIslandCore.hasMoveingCore(target.uniqueId)) {
                        target.errorMsg("你不在移动核心的状态")
                        return@executeOP
                    }
                    MoveIslandCore.endMoveCore(target)
                    MoveIslandCore.removePlayerLater20Tick(target)
                    target.successMsg("结束移动核心水晶")
                }
            }
            "coreLoc" {
                withArguments(playerArguments("玩家名"))
                executeOP {
                    val target = args[0] as Player
//                    val plotPlayer = MyIslands.plotAPI.wrapPlayer(target.uniqueId)!!
                    val plot = MyIslands.plotUtils.getPlotByPLoc(target)
                    if (plot == null) {
                        target.errorMsg("你的脚下没有地皮")
                        return@executeOP
                    }
                    val dLoc = plot.bottomAbs
//                    target.msg("dLoc: ${dLoc.x} ${dLoc.y} ${dLoc.z}")
                    val loc = target.location
                    val x = dLoc.x - loc.blockX
                    val y = dLoc.y - loc.blockY
                    val z = dLoc.z - loc.blockZ
                    target.msg("相对坐标为: $x $y $z")
                }
            }
            "tryMenu" {
                withArguments(playerArguments("玩家名"))
                executeOP {
                    val target = args[0] as Player
                    val plotPlayer = MyIslands.plotAPI.wrapPlayer(target.uniqueId) ?: return@executeOP
                    if (MyIslands.plotAPI.getPlayerPlots(plotPlayer).isNotEmpty()) {
                        return@executeOP
                    }
                    IslandsChooseMenu(antiClose = true, block = IslandsManager::chooseIslandSchematic).open(target)
                }
            }
            "menu" {
                withArguments(playerArguments("玩家名"))
                executeOP {
                    val target = args[0] as Player
                    IslandsMainMenu().open(target)
                }
            }
            "viewLoc" {
                withArguments(playerArguments("玩家名"))
                executeOP {
                    val target = args[0] as Player
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
            "deleteIsland" {
                withArguments(playerArguments("玩家名"))
                executeOP {
                    val target = args[0] as Player
                    val pp = target.asPlotPlayer()!!
                    val plot = pp.location.plotAbs
                    if (plot == null) {
                        target.msg("你的脚下没有岛屿")
                        return@executeOP
                    }
                    IslandsManager.removeIsland(pp, plot).whenComplete {
                        target.msg("删除成功")
                    }
                }
            }
        }
    }

//    fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
//        if (!sender.isOp) return false
//        cmdParser(sender, args) {
//            val type = arg<String>() ?: return@cmdParser
//            when (type) {
//                "moveCore" -> {
//                    val target = arg<Player>() ?: return@cmdParser
//                    MoveIslandCore.tryStartMoveCore(target)
//                }
//                "endMove" -> {
//                    val target = arg<Player>() ?: return@cmdParser
//                    if (!MoveIslandCore.hasMoveingCore(target.uniqueId)) {
//                        target.errorMsg("你不在移动核心的状态")
//                        return@cmdParser
//                    }
//                    MoveIslandCore.endMoveCore(target)
//                    MoveIslandCore.removePlayerLater20Tick(target)
//                    target.successMsg("结束移动核心水晶")
//                }
//                "coreLoc" -> {
//                    val target = arg<Player>() ?: return@cmdParser
////                    val plotPlayer = MyIslands.plotAPI.wrapPlayer(target.uniqueId)!!
//                    val plot = MyIslands.plotUtils.getPlotByPLoc(target)
//                    if (plot == null) {
//                        target.errorMsg("你的脚下没有地皮")
//                        return@cmdParser
//                    }
//                    val dLoc = plot.bottomAbs
////                    target.msg("dLoc: ${dLoc.x} ${dLoc.y} ${dLoc.z}")
//                    val loc = target.location
//                    val x = dLoc.x - loc.blockX
//                    val y = dLoc.y - loc.blockY
//                    val z = dLoc.z - loc.blockZ
//                    target.msg("相对坐标为: $x $y $z")
//                }
//                "tryMenu" -> {
//                    val target = arg<Player>() ?: return@cmdParser
//                    val plotPlayer = MyIslands.plotAPI.wrapPlayer(target.uniqueId) ?: return@cmdParser
//                    if (MyIslands.plotAPI.getPlayerPlots(plotPlayer).isNotEmpty()) {
//                        return@cmdParser
//                    }
//                    IslandsChooseMenu(true, IslandsManager::chooseIslandSchematic).open(target)
//                }
//                "menu" -> {
//                    val target = arg<Player>() ?: return@cmdParser
////                    IslandsChooseMenu().open(target)
//                    IslandsMainMenu().open(target)
//                }
//                "get" -> {
//                    val target = arg<Player>() ?: return@cmdParser
//                    val plotPlayer = MyIslands.plotAPI.wrapPlayer(target.uniqueId)!!
//                    val plots = MyIslands.plotAPI.getPlayerPlots(plotPlayer)
//                    if (plots.isNotEmpty()) {
//                        target.errorMsg("你已经领取了一个地皮")
//                        return@cmdParser
//                    }
//                    MyIslands.plotUtils.autoClaimPlot(target, plotPlayer) {
//                        target.successMsg("领取成功")
//                    }
//                }
//                "addH" -> {
//                    val player = arg<Player>() ?: return@cmdParser
//                    val target = arg<String>() ?: return@cmdParser
//                    IslandsManager.addHelpers(player, target.asUUIDFromByte())
//                }
//                "paste" -> {
//                    val target = arg<Player>() ?: return@cmdParser
//                    val schematiceName = arg<String>() ?: return@cmdParser
//
//                    val wrapPlayer = MyIslands.plotAPI.wrapPlayer(target.uniqueId)!!
//                    val plots = MyIslands.plotAPI.getPlayerPlots(wrapPlayer)
//                    if (plots.isEmpty()) {
//                        target.errorMsg("你需要领取一个地皮")
//                        return@cmdParser
//                    }
//                    MyIslands.plotUtils.paste(target, plots.first(), schematiceName) {
//                        if (it) {
//                            target.successMsg("成功")
//                        } else {
//                            target.successMsg("错误")
//                        }
//                    }
//                }
//                "viewLoc" -> {
//                    val target = arg<Player>() ?: return@cmdParser
//                    val wrapPlayer = MyIslands.plotAPI.wrapPlayer(target.uniqueId)!!
//                    val plots = MyIslands.plotAPI.getPlayerPlots(wrapPlayer)
//                    plots.forEach { plot ->
//                        sender.msg("id: ${plot.id}")
//                        plot.getDefaultHome {
//                            sender.msg("default: $it")
//                        }
//                        plot.getHome {
//                            sender.msg("home: $it")
//                        }
//                    }
//                }
//                "deleteIsland" -> {
//                    val target = arg<Player>() ?: return@cmdParser
//                    val pp = target.asPlotPlayer()!!
//                    val plot = pp.location.plotAbs
//                    if (plot == null) {
//                        target.msg("你的脚下没有岛屿")
//                        return@cmdParser
//                    }
//                    IslandsManager.removeIsland(pp, plot).whenComplete {
//                        target.msg("删除成功")
//                    }
//                }
//            }
//        }
//        return false
//    }

}