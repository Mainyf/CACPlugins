package io.github.mainyf.playersettings

import io.github.mainyf.bungeesettingsbukkit.CrossServerManager
import io.github.mainyf.bungeesettingsbukkit.ServerPacket
import io.github.mainyf.bungeesettingsbukkit.events.ServerPacketReceiveEvent
import io.github.mainyf.newmclib.BasePlugin
import io.github.mainyf.newmclib.command.apiCommand
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.serverId
import io.github.mainyf.playersettings.config.ConfigManager
import io.github.mainyf.playersettings.listeners.LoginListener
import io.github.mainyf.playersettings.listeners.PlayerListener
import io.github.mainyf.playersettings.storage.StorageManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*

class PlayerSettings : BasePlugin(), Listener {

    companion object {

        lateinit var INSTANCE: PlayerSettings

        val FLY_STATUS_REQ = ServerPacket.registerPacket("broadcast_fly_status_req")

        val FLY_STATUS_RES = ServerPacket.registerPacket("broadcast_fly_status_res")

    }

    private val LOG = io.github.mainyf.newmclib.getLogger("PlayerSettings")
    private val playerDisconnect = mutableSetOf<UUID>()
    private val playerFlyMap = mutableMapOf<UUID, Pair<Boolean, Float>>()

    override fun enable() {
        INSTANCE = this
        ConfigManager.load()
        StorageManager.init()
        CommandHandler.init()
        CommandHandler.register()
        pluginManager().registerEvents(this, this)
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

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        if (serverId() == "lobby") return
        if(playerDisconnect.contains(event.player.uuid)) {
            event.player.allowFlight = false
            event.player.flySpeed = 0.1f
        }
        playerDisconnect.remove(event.player.uuid)
        CrossServerManager.sendData(FLY_STATUS_REQ) {
            writeUUID(event.player.uuid)
        }
    }

    @EventHandler
    fun onPacket(event: ServerPacketReceiveEvent) {
        val buf = event.buf
        when (event.packet) {
            FLY_STATUS_REQ -> {
                if (serverId() == "lobby") return
                val uuid = buf.readUUID()
                if (playerFlyMap.containsKey(uuid)) {
                    val (allowFlight, speed) = playerFlyMap.remove(uuid)!!
                    CrossServerManager.sendData(FLY_STATUS_RES) {
                        writeString(serverId())
                        writeUUID(uuid)
                        writeBoolean(allowFlight)
                        writeFloat(speed)
                    }
                }
            }
            FLY_STATUS_RES -> {
                val serverId = buf.readString()
                if (serverId == serverId()) return
                val player = buf.readUUID().asPlayer() ?: return
                val allowFlight = buf.readBoolean()
                val speed = buf.readFloat()
                player.allowFlight = allowFlight
                player.flySpeed = speed
            }
            ServerPacket.PLAYER_DISCONNECT -> {
                val uuid = buf.readUUID()

                playerDisconnect.add(uuid)
                playerFlyMap.remove(uuid)
            }
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        if (playerDisconnect.contains(player.uuid)) return
        playerFlyMap[player.uuid] = player.allowFlight to player.flySpeed
    }

}