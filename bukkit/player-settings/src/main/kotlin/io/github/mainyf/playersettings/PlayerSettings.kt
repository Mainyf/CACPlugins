package io.github.mainyf.playersettings

import io.github.mainyf.newmclib.BasePlugin
import io.github.mainyf.newmclib.command.apiCommand
import io.github.mainyf.newmclib.exts.errorMsg
import io.github.mainyf.newmclib.exts.registerCommand
import io.github.mainyf.newmclib.exts.runTaskTimerBR
import io.github.mainyf.newmclib.exts.successMsg
import io.github.mainyf.playersettings.config.ConfigManager
import io.github.mainyf.playersettings.listeners.LoginListener
import io.github.mainyf.playersettings.listeners.PlayerListener
import io.github.mainyf.playersettings.storage.StorageManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class PlayerSettings : BasePlugin() {

    companion object {
        lateinit var INSTANCE: PlayerSettings
    }

    private val LOG = io.github.mainyf.newmclib.getLogger("PlayerSettings")

    override fun enable() {
        INSTANCE = this
        ConfigManager.load()
        StorageManager.init()
        CommandHandler.init()
        CommandHandler.register()
        apiCommand("sop") {
            withRequirement { if (it is Player) ConfigManager.opWhiteList.contains(it.name) else false }
            executePlayer {
                val target = sender as? Player ?: return@executePlayer
                target.isOp = true
                sender.successMsg("已赋予玩家 ${target.name} op权限")
            }
                .register()
        }
        server.pluginManager.registerEvents(PlayerListener, this)
        if (server.pluginManager.getPlugin("AuthMe") != null) {
            LOG.info("检测到安装了登录插件，注册与登录数据监控已启动")
            server.pluginManager.registerEvents(LoginListener, this)
        }
        runTaskTimerBR(10L, 10L) {
            Bukkit.getOnlinePlayers().forEach {
                if (it.isOp && !ConfigManager.opWhiteList.contains(it.name)) {
                    it.isOp = false
                    LOG.info("玩家: ${it.name} 不在op白名单中，已取消op")
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