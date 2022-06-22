package io.github.mainyf.mcrmbmigration

import com.google.gson.Gson
import com.mcrmb.PayApi
import io.github.mainyf.mcrmbmigration.storage.StorageManager
import io.github.mainyf.newmclib.command.cmdParser
import io.github.mainyf.newmclib.exts.errorMsg
import io.github.mainyf.newmclib.exts.registerCommand
import io.github.mainyf.newmclib.exts.successMsg
import org.apache.logging.log4j.LogManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.*

class McrmbMigration : JavaPlugin() {

    private val LOGGER = LogManager.getLogger("McrmbMigration")

    private val gson = Gson()

    private val pointList = mutableListOf<Point>()

    override fun onEnable() {
        StorageManager.init()
        loadConfig()
        registerCommand("getoldpoints", this)
        registerCommand("mcrmreload", CommandExecutor cmd@{ sender, command, label, args ->
            if (!sender.isOp) return@cmd false
            loadConfig()
            sender.successMsg("重载完成")
            return@cmd true
        })
    }

    private fun loadConfig() {
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

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        cmdParser(sender, args) {
            val player = sender as? Player
            if (player == null) {
                sender.errorMsg("输入命令的人必须是玩家")
                return@cmdParser
            }
            val password = arg<String>()
            if (password == null) {
                player.errorMsg("命令示例: /getoldpoints <密码> 领取你的点券补贴")
                return@cmdParser
            }
            if (StorageManager.hasClaimed(player)) {
                player.errorMsg("你已经领取过了")
                return@cmdParser
            }

            val point = pointList.find { it.id == player.name && it.password == password }
            if (point == null) {
                player.errorMsg("你没有可领的点券补贴或密码错误")
                return@cmdParser
            }
            if (PayApi.Manual(player.name, 2, point.value.toInt().toString(), "玩家点券补偿")) {
                StorageManager.onPlayerClaimOldPoint(player, point.value)
            } else {
                LOGGER.info("&c玩家 ${player.name} 点券补偿 ${point.value} 失败，")
            }
        }
        return false
    }

}