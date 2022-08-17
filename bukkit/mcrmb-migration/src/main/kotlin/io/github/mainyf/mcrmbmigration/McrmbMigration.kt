package io.github.mainyf.mcrmbmigration

import com.google.gson.Gson
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.arguments.GreedyStringArgument
import dev.jorel.commandapi.arguments.TextArgument
import io.github.mainyf.mcrmbmigration.storage.StorageManager
import io.github.mainyf.newmclib.BasePlugin
import io.github.mainyf.newmclib.command.apiCommand
import io.github.mainyf.newmclib.command.offlinePlayerArguments
import io.github.mainyf.newmclib.command.stringArguments
import io.github.mainyf.newmclib.exts.*
import org.apache.logging.log4j.LogManager
import org.black_ixx.playerpoints.PlayerPoints
import java.io.File
import java.util.*
import kotlin.collections.find

class McrmbMigration : BasePlugin() {

    private val LOGGER = LogManager.getLogger("McrmbMigration")

    private val gson = Gson()

    private val pointList = mutableListOf<Point>()

    override fun enable() {
        loadConfig()
        StorageManager.init()
        apiCommand("getoldpoints") {
            withHelp("领取点券补偿", "/getoldpoints <密码> 领取补偿的点券")
            withArguments(GreedyStringArgument("密码"))
            executePlayer {
                val player = sender
                val password = args[0] as String
                val claimed = StorageManager.getClaimed(player.uuid)
                if (claimed != null) {
                    player.sendLang("alreadyClaim", "{date}", claimed.createTime.formatYMDHM())
                    return@executePlayer
                }

                val point = pointList.find { it.id == player.name && it.password == password }
                if (point == null) {
                    player.sendLang("noPointClaim")
                    return@executePlayer
                }
                if (PlayerPoints.getInstance().api.give(player.uuid, point.value.toInt())) {
                    StorageManager.onPlayerClaimOldPoint(player, point.value)
                    player.sendLang("claimSuccess")
                    LOGGER.info("&c玩家 ${player.name} 点券补偿 ${point.value} 成功。")
                } else {
                    player.sendLang("claimFail")
                    LOGGER.info("&c玩家 ${player.name} 点券补偿 ${point.value} 失败。")
                }
            }
        }.register()
        apiCommand("mmi") {
            "view" {
                withArguments(
                    offlinePlayerArguments("玩家名")
                )
                executeOP {
                    val offlinePlayer = offlinePlayer()
                    val claimed = StorageManager.getClaimed(offlinePlayer.uuid)
                    if(claimed == null) {
                        sender.msg("该玩家没有领取过")
                    } else {
                        sender.msg("该玩家在 ${claimed.createTime.formatYMDHM()} 时领取了 ${claimed.value}")
                    }
                }
            }
            "reload" {
                executeOP {
                    loadConfig()
                    sender.successMsg("重载完成")
                }
            }
        }
    }

    override fun onDisable() {
        CommandAPI.unregister("getoldpoints")
    }

    private fun loadConfig() {
        saveDefaultConfig()
        reloadConfig()
        Lang.load(config)
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
        val playerDatas = File(dataFolder, "playerdatas.json")
        if (playerDatas.exists()) {
            val points = gson.fromJson(
                playerDatas.readText(),
                Array<Point>::class.java
            )
            pointList.clear()
            pointList.addAll(listOf(*points))
        }
    }

//    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
//        cmdParser(sender, args) {
//            val player = sender as? Player
//            if (player == null) {
//                sender.errorMsg("输入命令的人必须是玩家")
//                return@cmdParser
//            }
//            val password = arg<String>()
//            if (password == null) {
//                player.errorMsg("命令示例: /getoldpoints <密码> 领取你的点券补贴")
//                return@cmdParser
//            }
//            if (StorageManager.hasClaimed(player)) {
//                player.errorMsg("你已经领取过了")
//                return@cmdParser
//            }
//
//            val point = pointList.find { it.id == player.name && it.password == password }
//            if (point == null) {
//                player.errorMsg("你没有可领的点券补贴或密码错误")
//                return@cmdParser
//            }
//            if (PayApi.Manual(player.name, 2, point.value.toInt().toString(), "玩家点券补偿")) {
//                StorageManager.onPlayerClaimOldPoint(player, point.value)
//            } else {
//                LOGGER.info("&c玩家 ${player.name} 点券补偿 ${point.value} 失败，")
//            }
//        }
//        return false
//    }

}