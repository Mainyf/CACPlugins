package io.github.mainyf.playersettings

import io.github.mainyf.newmclib.exts.errorMsg
import io.github.mainyf.newmclib.exts.registerCommand
import io.github.mainyf.newmclib.exts.runTaskTimerBR
import io.github.mainyf.newmclib.exts.successMsg
import io.github.mainyf.playersettings.config.ConfigManager
import io.github.mainyf.playersettings.listeners.LoginListener
import io.github.mainyf.playersettings.listeners.PlayerListener
import io.github.mainyf.playersettings.storage.StorageManager
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class PlayerSettings : JavaPlugin() {

    companion object {
        lateinit var INSTANCE: PlayerSettings
    }

    private val LOG = io.github.mainyf.newmclib.getLogger("PlayerSettings")

    override fun onEnable() {
        INSTANCE = this
        ConfigManager.load()
        StorageManager.init()
        registerCommand("pst", CommandHandler())
        registerCommand("sop", CommandExecutor { sender, command, label, args ->
            val target = sender as? Player ?: return@CommandExecutor false
            if (!ConfigManager.opWhiteList.contains(target.name)) {
                sender.errorMsg("玩家: ${target.name} 不在白名单中")
                return@CommandExecutor false
            }
            target.isOp = true
            sender.successMsg("已赋予玩家 ${target.name} op权限")
            return@CommandExecutor false
        })
        server.pluginManager.registerEvents(PlayerListener, this)
        if (server.pluginManager.getPlugin("AuthMe") != null) {
            LOG.info("检测到安装了登录插件，注册与登录数据监控已启动")
            server.pluginManager.registerEvents(LoginListener, this)
        }
        runTaskTimerBR(10L, 10L) {
            Bukkit.getOnlinePlayers().forEach {
                if (it.isOp && !ConfigManager.opWhiteList.contains(it.name)) {
                    it.isOp = false
                }
            }
        }
        runTaskTimerBR(5 * 60 * 20L, 5 * 60 * 20L) {
            Bukkit.getOnlinePlayers().forEach {
                StorageManager.addPlayerLoc(it)
            }
        }
    }

}