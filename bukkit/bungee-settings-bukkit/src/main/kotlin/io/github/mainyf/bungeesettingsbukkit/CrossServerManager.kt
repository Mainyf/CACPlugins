package io.github.mainyf.bungeesettingsbukkit

import com.Zrips.CMI.events.CMIPlayerTeleportEvent
import io.github.mainyf.bungeesettingsbukkit.network.ClientSocketManager
import io.github.mainyf.newmclib.dsl.event
import io.github.mainyf.newmclib.dsl.events
import io.github.mainyf.newmclib.exts.*
import io.github.mainyf.newmclib.serverId
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import java.util.*

object CrossServerManager {

    private val pendingTPRequest = mutableMapOf<UUID, Location>()
    private val joinPlayers = mutableSetOf<UUID>()
    private val CHANNEL_NAME = "cacserver:dispatcher"
    val serverIds = mutableSetOf<String>()

    fun init() {
        server().messenger.registerOutgoingPluginChannel(BungeeSettingsBukkit.INSTANCE, CHANNEL_NAME)
        server().messenger.registerIncomingPluginChannel(
            BungeeSettingsBukkit.INSTANCE, CHANNEL_NAME
        ) { _, _, data ->
            val buf = Unpooled.wrappedBuffer(data)
            when (buf.readString()) {
                "tp" -> {
                    val playerUUID = buf.readUUID()
                    val world = buf.readString().asWorld() ?: return@registerIncomingPluginChannel
                    val x = buf.readDouble()
                    val y = buf.readDouble()
                    val z = buf.readDouble()
                    val yaw = buf.readFloat()
                    val pitch = buf.readFloat()
                    val loc = Location(world, x, y, z, yaw, pitch)
                    val player = playerUUID.asPlayer()
                    if (player == null) {
                        pendingTPRequest[playerUUID] = loc
                    } else {
                        player.teleport(loc)
                    }
                }
            }
        }
        BungeeSettingsBukkit.INSTANCE.events {
            event<PlayerJoinEvent> {
                joinPlayers.add(player.uniqueId)
            }
            event<CMIPlayerTeleportEvent> {
                if (joinPlayers.contains(player.uniqueId)) {
                    joinPlayers.remove(player.uniqueId)
                    if (pendingTPRequest.containsKey(player.uniqueId)) {
                        isCancelled = true
                        val loc = pendingTPRequest[player.uniqueId]!!
                        player.teleport(loc)
                        pendingTPRequest.remove(player.uniqueId)
                    }
                }
            }
        }
    }

    fun handleServerPacket(id: Int, buf: ByteBuf) {
        when (id) {
            1 -> {
                val cmd = buf.readString()
                execmd(cmd)
            }
            101 -> {
                serverIds.clear()
                serverIds.addAll(buf.readStringList())
            }
        }
    }

    fun initServerID() {
        ClientSocketManager.sendData(0) {
            writeString(serverId())
        }
    }

    fun loadSocket(port: Int) {
        kotlin.runCatching {
            ClientSocketManager.initClient(port)
        }.onFailure {
            it.printStackTrace()
        }
    }

    fun stp(player: Player, serverId: String, worldName: String, loc: Location) {
        if (serverId == serverId()) {
            player.teleport(loc.clone().apply {
                this.world = worldName.asWorld()
            })
        } else {
            player.sendPluginMessage(BungeeSettingsBukkit.INSTANCE, CHANNEL_NAME, Unpooled.buffer().apply {
                writeString("tp")
                writeUUID(player.uniqueId)
                writeString(serverId)
                writeString(worldName)
                writeDouble(loc.x)
                writeDouble(loc.y)
                writeDouble(loc.z)
                writeFloat(loc.yaw)
                writeFloat(loc.pitch)
            }.toByteArray())
        }
    }

    fun executeCommandToServer(serverId: String, cmd: String) {
//        if (serverId == serverId()) {
//            execmd(cmd)
//        } else {
//            ClientSocketManager.sendData(1) {
//                writeString(serverId)
//                writeString(cmd)
//            }
//        }
        ClientSocketManager.sendData(1) {
            writeString(serverId)
            writeString(cmd)
        }
    }

    fun executeCommandToAll(cmd: String) {
        execmd(cmd)
        ClientSocketManager.sendData(2) {
            writeString(cmd)
        }
    }

}